package com.winlator.cmod.xenvironment;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.cmod.MainActivity;
import com.winlator.cmod.R;
import com.winlator.cmod.SettingsFragment;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.DownloadProgressDialog;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.StreamUtils;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.WineInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ImageFsInstaller {
    private static final String TAG = "ImageFsInstaller";
    private static final String EXPORT_ASSETS_DIR = "export";
    private static final int ROOTFS_PROGRESS_END = 76;
    private static final int INSTALL_PROGRESS_END = 100;
    private static final byte DEFAULT_COMPRESSION_RATIO = 22;
    public static final byte LATEST_VERSION = 21;

    private static void resetContainerImgVersions(Context context) {
        ContainerManager manager = new ContainerManager(context);
        for (Container container : manager.getContainers()) {
            String imgVersion = container.getExtra("imgVersion");
            String wineVersion = container.getWineVersion();
            if (!imgVersion.isEmpty() && WineInfo.isMainWineVersion(wineVersion) && Short.parseShort(imgVersion) <= 5) {
                container.putExtra("wineprefixNeedsUpdate", "t");
            }

            container.putExtra("imgVersion", null);
            container.saveData();
        }
    }

    public static void installWineFromAssets(final MainActivity activity, DownloadProgressDialog dialog) {
        installWineFromAssets(activity, dialog, ROOTFS_PROGRESS_END, INSTALL_PROGRESS_END);
    }

    private static void installWineFromAssets(final MainActivity activity, DownloadProgressDialog dialog, int startProgress, int endProgress) {
        String[] versions = activity.getResources().getStringArray(R.array.wine_entries);
        File rootDir = ImageFs.find(activity).getRootDir();
        if (versions.length == 0) return;

        int totalProgressRange = Math.max(0, endProgress - startProgress);
        AtomicReference<String> lastFileRef = new AtomicReference<>("");

        for (int i = 0; i < versions.length; i++) {
            String version = versions[i];
            int segmentStart = startProgress + (totalProgressRange * i) / versions.length;
            int segmentEnd = startProgress + (totalProgressRange * (i + 1)) / versions.length;
            long contentLength = estimateExtractedSize(activity, version + ".txz");
            AtomicLong extractedSizeRef = new AtomicLong();

            activity.runOnUiThread(() ->
                    dialog.setStatusText(activity.getString(R.string.installing_wine_version_step, version)));
            File outFile = new File(rootDir, "/opt/" + version);
            outFile.mkdirs();
            boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, activity, version + ".txz", outFile, (file, size) -> {
                if (size > 0) {
                    long extractedSize = extractedSizeRef.addAndGet(size);
                    final int progress = segmentProgress(segmentStart, segmentEnd, extractedSize, contentLength);
                    String relativePath = file != null ? file.getAbsolutePath().replace(outFile.getAbsolutePath() + "/", "") : "";
                    String displayName = relativePath.isEmpty() ? version : shortenPathForStatus(relativePath);
                    boolean statusChanged = !displayName.equals(lastFileRef.get());
                    if (statusChanged) {
                        lastFileRef.set(displayName);
                    }
                    activity.runOnUiThread(() -> {
                        dialog.setProgress(progress);
                        if (statusChanged) {
                            dialog.setStatusText(activity.getString(R.string.extracting_file_step, displayName));
                        }
                    });
                }
                return file;
            });

            if (success) {
                final int completedProgress = segmentEnd;
                activity.runOnUiThread(() -> dialog.setProgress(completedProgress));
            }
        }
    }

    public static void installFromAssets(final MainActivity activity) {
        AppUtils.keepScreenOn(activity);
        ImageFs imageFs = ImageFs.find(activity);
        File rootDir = imageFs.getRootDir();

        SettingsFragment.resetEmulatorsVersion(activity);

        final DownloadProgressDialog dialog = new DownloadProgressDialog(activity);
        dialog.show(R.string.installing_system_files, true);
        Executors.newSingleThreadExecutor().execute(() -> {
            activity.runOnUiThread(() -> dialog.setStatusText(R.string.preparing_installation));
            clearRootDir(rootDir);
            final long contentLength = estimateExtractedSize(activity, "imagefs.txz");
            AtomicLong totalSizeRef = new AtomicLong();
            AtomicReference<String> lastFileRef = new AtomicReference<>("");

            activity.runOnUiThread(() -> dialog.setStatusText(R.string.unpacking_rootfs_step));

            boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, activity, "imagefs.txz", rootDir, (file, size) -> {
                if (size > 0) {
                    long totalSize = totalSizeRef.addAndGet(size);
                    final int progress = segmentProgress(0, ROOTFS_PROGRESS_END, totalSize, contentLength);
                    String relativePath = file != null ? file.getAbsolutePath().replace(rootDir.getAbsolutePath() + "/", "") : "";
                    String displayName = relativePath.isEmpty() ? "..." : shortenPathForStatus(relativePath);
                    boolean statusChanged = !displayName.equals(lastFileRef.get());
                    if (statusChanged) {
                        lastFileRef.set(displayName);
                    }
                    activity.runOnUiThread(() -> {
                        dialog.setProgress(progress);
                        if (statusChanged) {
                            dialog.setStatusText(activity.getString(R.string.extracting_file_step, displayName));
                        }
                    });
                }
                return file;
            });

            if (success) {
                activity.runOnUiThread(() -> dialog.setProgress(ROOTFS_PROGRESS_END));
                installWineFromAssets(activity, dialog, ROOTFS_PROGRESS_END, INSTALL_PROGRESS_END);
                activity.runOnUiThread(() -> dialog.setStatusText(R.string.finishing_installation));
                installExportFilesFromAssets(activity, rootDir);
                imageFs.createImgVersionFile(LATEST_VERSION);
                resetContainerImgVersions(activity);
                activity.runOnUiThread(() -> dialog.setProgress(INSTALL_PROGRESS_END));
            }
            else AppUtils.showToast(activity, R.string.unable_to_install_system_files);

            dialog.closeOnUiThread();
        });
    }

    private static boolean installExportFilesFromAssets(Context context, File rootDir) {
        File exportDir = new File(rootDir, EXPORT_ASSETS_DIR);
        if (!exportDir.isDirectory() && !exportDir.mkdirs()) {
            Log.e(TAG, "Failed to create export directory: " + exportDir.getAbsolutePath());
            return false;
        }

        try {
            return copyAssetDirectory(context.getAssets(), EXPORT_ASSETS_DIR, exportDir);
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy export assets to: " + exportDir.getAbsolutePath(), e);
            return false;
        }
    }

    private static boolean copyAssetDirectory(AssetManager assetManager, String assetPath, File dstDir) throws IOException {
        String[] assetNames = assetManager.list(assetPath);
        if (assetNames == null || assetNames.length == 0) {
            return copyAssetFile(assetManager, assetPath, new File(dstDir, FileUtils.getName(assetPath)));
        }

        if (!dstDir.isDirectory() && !dstDir.mkdirs()) {
            Log.e(TAG, "Failed to create asset destination directory: " + dstDir.getAbsolutePath());
            return false;
        }

        boolean success = true;
        for (String assetName : assetNames) {
            String childAssetPath = assetPath + "/" + assetName;
            String[] childAssetNames = assetManager.list(childAssetPath);
            File childDstFile = new File(dstDir, assetName);
            if (childAssetNames != null && childAssetNames.length > 0) {
                success &= copyAssetDirectory(assetManager, childAssetPath, childDstFile);
            } else {
                success &= copyAssetFile(assetManager, childAssetPath, childDstFile);
            }
        }
        return success;
    }

    private static boolean copyAssetFile(AssetManager assetManager, String assetPath, File dstFile) {
        File parent = dstFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            Log.e(TAG, "Failed to create asset file parent: " + parent.getAbsolutePath());
            return false;
        }

        try (InputStream inStream = assetManager.open(assetPath);
             BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(dstFile), StreamUtils.BUFFER_SIZE)) {
            return StreamUtils.copy(inStream, outStream);
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy asset: " + assetPath + " to " + dstFile.getAbsolutePath(), e);
            return false;
        }
    }

    private static long estimateExtractedSize(Context context, String assetFile) {
        return Math.max(1L, (long)(FileUtils.getSize(context, assetFile) * (100.0f / DEFAULT_COMPRESSION_RATIO)));
    }

    private static int segmentProgress(int startProgress, int endProgress, long extractedSize, long estimatedSize) {
        if (endProgress <= startProgress) return startProgress;

        float fraction = estimatedSize > 0 ? (float)extractedSize / estimatedSize : 0f;
        fraction = Math.max(0f, Math.min(1f, fraction));
        return startProgress + Math.round((endProgress - startProgress) * fraction);
    }

    private static String shortenPathForStatus(String path) {
        if (path == null || path.isEmpty()) return "...";

        String normalized = path.replace('\\', '/');
        String[] parts = normalized.split("/");
        if (parts.length <= 2) return normalized;

        return parts[parts.length - 2] + "/" + parts[parts.length - 1];
    }

    public static void installIfNeeded(final MainActivity activity) {
        ImageFs imageFs = ImageFs.find(activity);
        if (!imageFs.isValid() || imageFs.getVersion() < LATEST_VERSION) installFromAssets(activity);
    }

    private static void clearOptDir(File optDir) {
        File[] files = optDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals("installed-wine")) continue;
                FileUtils.delete(file);
            }
        }
    }

    private static void clearRootDir(File rootDir) {
        if (rootDir.isDirectory()) {
            File[] files = rootDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        String name = file.getName();
                        if (name.equals("home")) {
                            continue;
                        }
                    }
                    FileUtils.delete(file);
                }
            }
        }
        else rootDir.mkdirs();
    }
}
