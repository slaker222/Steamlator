package com.winlator.cmod.xserver;

import android.util.SparseArray;

import com.winlator.cmod.core.CursorLocker;
import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.winhandler.WinHandler;
import com.winlator.cmod.xserver.extensions.BigReqExtension;
import com.winlator.cmod.xserver.extensions.DRI3Extension;
import com.winlator.cmod.xserver.extensions.Extension;
import com.winlator.cmod.xserver.extensions.MITSHMExtension;
import com.winlator.cmod.xserver.extensions.PresentExtension;
import com.winlator.cmod.xserver.extensions.SyncExtension;

import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.LockSupport;

public class XServer {
    public enum Lockable {WINDOW_MANAGER, PIXMAP_MANAGER, DRAWABLE_MANAGER, GRAPHIC_CONTEXT_MANAGER, INPUT_DEVICE, CURSOR_MANAGER, SHMSEGMENT_MANAGER}
    public static final short VERSION = 11;
    public static final String VENDOR_NAME = "Elbrus Technologies, LLC";
    public static final Charset LATIN1_CHARSET = Charset.forName("latin1");
    public final SparseArray<Extension> extensions = new SparseArray<>();
    public final ScreenInfo screenInfo;
    public final PixmapManager pixmapManager;
    public final ResourceIDs resourceIDs = new ResourceIDs(128);
    public final GraphicsContextManager graphicsContextManager = new GraphicsContextManager();
    public final SelectionManager selectionManager;
    public final DrawableManager drawableManager;
    public final WindowManager windowManager;
    public final CursorManager cursorManager;
    public final Keyboard keyboard = Keyboard.createKeyboard(this);
    public final Pointer pointer = new Pointer(this);
    public final InputDeviceManager inputDeviceManager;
    public final GrabManager grabManager;
    public final CursorLocker cursorLocker;
    private SHMSegmentManager shmSegmentManager;
    private GLRenderer renderer;
    private WinHandler winHandler;
    private final EnumMap<Lockable, ReentrantLock> locks = new EnumMap<>(Lockable.class);
    private boolean relativeMouseMovement = false;
    private boolean forceMouseControl = false;
    private boolean simulateTouchScreen = false;
    private boolean isGrabbed = false;
    private XClient grabbingClient = null;

    // Frame limiter applied at X11 Present stage.
    // This throttles clients that present frames (e.g. DXVK) rather than the Android UI refresh.
    private volatile int presentFpsLimit = 0; // 0 = unlimited
    private long nextPresentDeadlineNs = 0L;

    // "Frame Gen": we render a duplicate intermediate frame between real presents.
    // This improves perceived smoothness without needing motion vectors/interpolation.
    private volatile boolean frameGenEnabled = false;
    private ScheduledExecutorService frameGenScheduler;

    public XServer(ScreenInfo screenInfo) {
        this.screenInfo = screenInfo;
        cursorLocker = new CursorLocker(this);
        for (Lockable lockable : Lockable.values()) locks.put(lockable, new ReentrantLock());

        pixmapManager = new PixmapManager();
        drawableManager = new DrawableManager(this);
        cursorManager = new CursorManager(drawableManager);
        windowManager = new WindowManager(screenInfo, drawableManager);
        selectionManager = new SelectionManager(windowManager);
        inputDeviceManager = new InputDeviceManager(this);
        grabManager = new GrabManager(this);

        DesktopHelper.attachTo(this);
        setupExtensions();
    }

    public int getPresentFpsLimit() {
        return presentFpsLimit;
    }

    public void setPresentFpsLimit(int fps) {
        this.presentFpsLimit = Math.max(0, fps);
        this.nextPresentDeadlineNs = 0L;
    }

    public boolean isFrameGenEnabled() {
        return frameGenEnabled;
    }

    public void setFrameGenEnabled(boolean enabled) {
        this.frameGenEnabled = enabled;
        if (!enabled) {
            // Avoid leaking the scheduler thread when feature is turned off or activity exits.
            ScheduledExecutorService scheduler = frameGenScheduler;
            frameGenScheduler = null;
            if (scheduler != null) {
                scheduler.shutdownNow();
            }
        }
    }

    public void scheduleGeneratedFrameIfNeeded() {
        if (!frameGenEnabled) return;
        final int limit = presentFpsLimit;
        if (limit <= 0) return;
        if (renderer == null || renderer.xServerView == null) return;

        if (frameGenScheduler == null) {
            synchronized (this) {
                if (frameGenScheduler == null) {
                    frameGenScheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "framegen");
                            t.setDaemon(true);
                            return t;
                        }
                    });
                }
            }
        }

        long delayNs = (1_000_000_000L / limit) / 2L;
        frameGenScheduler.schedule(() -> renderer.xServerView.requestRender(), delayNs, TimeUnit.NANOSECONDS);
    }

    public void pacePresentIfNeeded() {
        final int limit = presentFpsLimit;
        if (limit <= 0) {
            nextPresentDeadlineNs = 0L;
            return;
        }

        final long frameNs = 1_000_000_000L / limit;
        final long nowNs = System.nanoTime();

        // Use a stable timeline: nextDeadline += frameNs (avoids jitter/drift vs using "now" each time).
        if (nextPresentDeadlineNs == 0L) {
            nextPresentDeadlineNs = nowNs + frameNs;
            return;
        }

        long remainingNs = nextPresentDeadlineNs - nowNs;
        if (remainingNs <= 0L) {
            // We're late; move deadline forward but don't "fast-forward" too much.
            nextPresentDeadlineNs = nowNs + frameNs;
            return;
        }

        // Sleep most of the remaining time with parkNanos (finer than Thread.sleep),
        // then spin/yield for the last ~0.5ms for smoother pacing.
        final long spinThresholdNs = 500_000L;
        if (remainingNs > spinThresholdNs) {
            LockSupport.parkNanos(remainingNs - spinThresholdNs);
        }
        while ((remainingNs = nextPresentDeadlineNs - System.nanoTime()) > 0L) {
            Thread.yield();
        }

        nextPresentDeadlineNs += frameNs;
    }

    public boolean isRelativeMouseMovement() {
        return relativeMouseMovement;
    }

    public void setRelativeMouseMovement(boolean relativeMouseMovement) {
        cursorLocker.setEnabled(!relativeMouseMovement);
        this.relativeMouseMovement = relativeMouseMovement;
    }

    public boolean isForceMouseControl() { return forceMouseControl; }

    public void setForceMouseControl(boolean forceMouseControl) {
        cursorLocker.setEnabled(!forceMouseControl);
        this.forceMouseControl = forceMouseControl;
    }

    public boolean isSimulateTouchScreen() { return simulateTouchScreen; }

    public void setSimulateTouchScreen(boolean simulateTouchScreen) {
        this.simulateTouchScreen = simulateTouchScreen;
    }

    public GLRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(GLRenderer renderer) {
        this.renderer = renderer;
    }

    public WinHandler getWinHandler() {
        return winHandler;
    }

    public void setWinHandler(WinHandler winHandler) {
        this.winHandler = winHandler;
    }

    public SHMSegmentManager getSHMSegmentManager() {
        return shmSegmentManager;
    }

    public void setSHMSegmentManager(SHMSegmentManager shmSegmentManager) {
        this.shmSegmentManager = shmSegmentManager;
    }

    private class SingleXLock implements XLock {
        private final ReentrantLock lock;

        private SingleXLock(Lockable lockable) {
            this.lock = locks.get(lockable);
            lock.lock();
        }

        @Override
        public void close() {
            lock.unlock();
        }
    }

    private class MultiXLock implements XLock {
        private final Lockable[] lockables;

        private MultiXLock(Lockable[] lockables) {
            this.lockables = lockables;
            for (Lockable lockable : lockables) locks.get(lockable).lock();
        }

        @Override
        public void close() {
            for (int i = lockables.length - 1; i >= 0; i--) {
                locks.get(lockables[i]).unlock();
            }
        }
    }

    public XLock lock(Lockable lockable) {
        return new SingleXLock(lockable);
    }

    public XLock lock(Lockable... lockables) {
        return new MultiXLock(lockables);
    }

    public XLock lockAll() {
        return new MultiXLock(Lockable.values());
    }

    public Extension getExtensionByName(String name) {
        for (int i = 0; i < extensions.size(); i++) {
            Extension extension = extensions.valueAt(i);
            if (extension.getName().equals(name)) return extension;
        }
        return null;
    }

    public void injectPointerMove(int x, int y) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setPosition(x, y);
        }
    }

    public void injectPointerMoveDelta(int dx, int dy) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setPosition(pointer.getX() + dx, pointer.getY() + dy);
        }
    }

    public void injectPointerButtonPress(Pointer.Button buttonCode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setButton(buttonCode, true);
        }
    }

    public void injectPointerButtonRelease(Pointer.Button buttonCode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setButton(buttonCode, false);
        }
    }

    public void injectKeyPress(XKeycode xKeycode) {
        injectKeyPress(xKeycode, 0);
    }

    public void injectKeyPress(XKeycode xKeycode, int keysym) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            keyboard.setKeyPress(xKeycode.id, keysym);
        }
    }

    public void injectKeyRelease(XKeycode xKeycode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            keyboard.setKeyRelease(xKeycode.id);
        }
    }

    private void setupExtensions() {
        extensions.put(BigReqExtension.MAJOR_OPCODE, new BigReqExtension());
        extensions.put(MITSHMExtension.MAJOR_OPCODE, new MITSHMExtension());
        extensions.put(DRI3Extension.MAJOR_OPCODE, new DRI3Extension());
        extensions.put(PresentExtension.MAJOR_OPCODE, new PresentExtension());
        extensions.put(SyncExtension.MAJOR_OPCODE, new SyncExtension());
    }

    public <T extends Extension> T getExtension(int opcode) {
        return (T)extensions.get(opcode);
    }

    public synchronized void setGrabbed(boolean grabbed, XClient client) {
        this.isGrabbed = grabbed;
        this.grabbingClient = client;
    }

    public synchronized boolean isGrabbedBy(XClient client) {
        return isGrabbed && grabbingClient == client;
    }
}
