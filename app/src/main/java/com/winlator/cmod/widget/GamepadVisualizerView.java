package com.winlator.cmod.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import java.util.Locale;

public class GamepadVisualizerView extends View {
    private static final String TAG = "GamepadVisualizer";
    private static final float DEADZONE = 0.08f;
    private static final float TRIGGER_PRESS_THRESHOLD = 0.2f;
    private static final float AXIS_LOG_EPSILON = 0.02f;
    private static final boolean ENABLE_TRIGGER_DIAGNOSTICS = false;
    private static final int MAX_DIAGNOSTIC_LINES = 7;

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint controlPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint diagnosticBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint diagnosticTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final ArraySet<Integer> pressedKeys = new ArraySet<>();
    private final SparseArray<Float> lastLoggedAxisValues = new SparseArray<>();
    private String[] diagnosticLines = {
            "Diagnostics: press LT/RT",
            "Changed axes will appear here"
    };
    private boolean leftTriggerKeyPressed;
    private boolean rightTriggerKeyPressed;
    private boolean hasRxRy;

    private float leftStickX;
    private float leftStickY;
    private float rightStickX;
    private float rightStickY;
    private float leftTrigger;
    private float rightTrigger;
    private float hatX;
    private float hatY;

    public GamepadVisualizerView(Context context) {
        super(context);
        init();
    }

    public GamepadVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GamepadVisualizerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        basePaint.setStyle(Paint.Style.FILL);
        basePaint.setColor(Color.argb(255, 52, 58, 66));

        controlPaint.setStyle(Paint.Style.FILL);
        controlPaint.setColor(Color.argb(255, 78, 86, 95));

        activePaint.setStyle(Paint.Style.FILL);
        activePaint.setColor(Color.argb(255, 67, 160, 71));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);

        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(3f);
        outlinePaint.setColor(Color.argb(255, 120, 130, 140));

        diagnosticBgPaint.setStyle(Paint.Style.FILL);
        diagnosticBgPaint.setColor(Color.argb(205, 20, 24, 28));

        diagnosticTextPaint.setColor(Color.WHITE);
        diagnosticTextPaint.setTextAlign(Paint.Align.LEFT);

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public boolean handleKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (!isGamepadKey(keyCode)) {
            return false;
        }

        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN) {
            pressedKeys.add(keyCode);
        } else if (action == KeyEvent.ACTION_UP) {
            pressedKeys.remove(keyCode);
        } else {
            return false;
        }

        if (isLeftTriggerKey(keyCode)) {
            leftTriggerKeyPressed = action == KeyEvent.ACTION_DOWN;
            leftTrigger = leftTriggerKeyPressed ? 1f : 0f;
            setDiagnosticLines("Key " + keyActionName(action) + ": " + KeyEvent.keyCodeToString(keyCode));
        } else if (isRightTriggerKey(keyCode)) {
            rightTriggerKeyPressed = action == KeyEvent.ACTION_DOWN;
            rightTrigger = rightTriggerKeyPressed ? 1f : 0f;
            setDiagnosticLines("Key " + keyActionName(action) + ": " + KeyEvent.keyCodeToString(keyCode));
        }
        invalidate();
        return true;
    }

    public boolean handleMotionEvent(MotionEvent event) {
        if ((event.getSource() & android.view.InputDevice.SOURCE_JOYSTICK) != android.view.InputDevice.SOURCE_JOYSTICK
                || event.getAction() != MotionEvent.ACTION_MOVE) {
            return false;
        }

        leftStickX = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_X));
        leftStickY = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Y));
        hasRxRy = hasAxis(event, MotionEvent.AXIS_RX) || hasAxis(event, MotionEvent.AXIS_RY);
        rightStickX = applyDeadzone(event.getAxisValue(hasRxRy ? MotionEvent.AXIS_RX : MotionEvent.AXIS_Z));
        rightStickY = applyDeadzone(event.getAxisValue(hasRxRy ? MotionEvent.AXIS_RY : MotionEvent.AXIS_RZ));

        logChangedAxes(event);
        updateTriggers(event);

        hatX = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_HAT_X));
        hatY = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_HAT_Y));

        invalidate();
        return true;
    }

    private boolean isGamepadKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_BUTTON_A
                || keyCode == KeyEvent.KEYCODE_BUTTON_1
                || keyCode == KeyEvent.KEYCODE_BUTTON_B
                || keyCode == KeyEvent.KEYCODE_BUTTON_2
                || keyCode == KeyEvent.KEYCODE_BUTTON_X
                || keyCode == KeyEvent.KEYCODE_BUTTON_3
                || keyCode == KeyEvent.KEYCODE_BUTTON_Y
                || keyCode == KeyEvent.KEYCODE_BUTTON_4
                || keyCode == KeyEvent.KEYCODE_BUTTON_L1
                || keyCode == KeyEvent.KEYCODE_BUTTON_5
                || keyCode == KeyEvent.KEYCODE_BUTTON_R1
                || keyCode == KeyEvent.KEYCODE_BUTTON_6
                || keyCode == KeyEvent.KEYCODE_BUTTON_L2
                || keyCode == KeyEvent.KEYCODE_BUTTON_7
                || keyCode == KeyEvent.KEYCODE_BUTTON_R2
                || keyCode == KeyEvent.KEYCODE_BUTTON_8
                || keyCode == KeyEvent.KEYCODE_BUTTON_THUMBL
                || keyCode == KeyEvent.KEYCODE_BUTTON_11
                || keyCode == KeyEvent.KEYCODE_BUTTON_THUMBR
                || keyCode == KeyEvent.KEYCODE_BUTTON_12
                || keyCode == KeyEvent.KEYCODE_BUTTON_START
                || keyCode == KeyEvent.KEYCODE_BUTTON_10
                || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT
                || keyCode == KeyEvent.KEYCODE_BUTTON_9
                || keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    private float applyDeadzone(float value) {
        return Math.abs(value) < DEADZONE ? 0f : Math.max(-1f, Math.min(1f, value));
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private boolean hasAxis(MotionEvent event, int axis) {
        InputDevice device = event.getDevice();
        return device != null
                && (device.getMotionRange(axis, event.getSource()) != null
                || device.getMotionRange(axis) != null);
    }

    private InputDevice.MotionRange getMotionRange(MotionEvent event, int axis) {
        InputDevice device = event.getDevice();
        if (device == null) return null;
        InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());
        return range != null ? range : device.getMotionRange(axis);
    }

    private float normalizeTo01(float value, float start, float end) {
        float denom = end - start;
        if (Math.abs(denom) < 1e-5f) return 0f;
        return clamp01((value - start) / denom);
    }

    private void updateTriggers(MotionEvent event) {
        float lt = leftTriggerKeyPressed ? 1f : 0f;
        float rt = rightTriggerKeyPressed ? 1f : 0f;

        lt = Math.max(lt, readTriggerAxis(event, MotionEvent.AXIS_LTRIGGER));
        lt = Math.max(lt, readTriggerAxis(event, MotionEvent.AXIS_BRAKE));
        rt = Math.max(rt, readTriggerAxis(event, MotionEvent.AXIS_RTRIGGER));
        rt = Math.max(rt, readTriggerAxis(event, MotionEvent.AXIS_GAS));

        if (hasRxRy) {
            lt = Math.max(lt, readTriggerAxis(event, MotionEvent.AXIS_Z));
            rt = Math.max(rt, readTriggerAxis(event, MotionEvent.AXIS_RZ));
        }

        leftTrigger = lt >= TRIGGER_PRESS_THRESHOLD ? 1f : 0f;
        rightTrigger = rt >= TRIGGER_PRESS_THRESHOLD ? 1f : 0f;
    }

    private float readTriggerAxis(MotionEvent event, int axis) {
        InputDevice device = InputDevice.getDevice(event.getDeviceId());
        if (device == null) return 0f;

        InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());
        if (range == null) return 0f;

        float raw = event.getAxisValue(axis);
        float min = range != null ? range.getMin() : -1f;
        float max = range != null ? range.getMax() : 1f;
        if (Math.abs(max - min) < 0.001f) return 0f;

        return normalizeTo01(raw, min, max);
    }

    private void logChangedAxes(MotionEvent event) {
        if (!ENABLE_TRIGGER_DIAGNOSTICS) return;

        InputDevice device = InputDevice.getDevice(event.getDeviceId());
        if (device == null) return;

        boolean changed = false;
        String[] changedLines = new String[MAX_DIAGNOSTIC_LINES];
        int changedLineCount = 0;
        for (InputDevice.MotionRange range : device.getMotionRanges()) {
            int axis = range.getAxis();
            float value = event.getAxisValue(axis);
            Float lastValue = lastLoggedAxisValues.get(axis);
            if (lastValue == null || Math.abs(value - lastValue) >= AXIS_LOG_EPSILON) {
                changed = true;
                if (changedLineCount < MAX_DIAGNOSTIC_LINES) {
                    changedLines[changedLineCount++] = formatAxisLine(axis, value, range);
                }
            }
            lastLoggedAxisValues.put(axis, value);
        }
        if (!changed) return;

        String[] screenLines = new String[Math.max(2, changedLineCount + 1)];
        screenLines[0] = "Changed axes, hasRxRy=" + hasRxRy;
        if (changedLineCount == 0) {
            screenLines[1] = "No axis changed enough";
        } else {
            System.arraycopy(changedLines, 0, screenLines, 1, changedLineCount);
        }
        diagnosticLines = screenLines;

        StringBuilder sb = new StringBuilder("=== ALL AXES ===");
        sb.append(" device=").append(device.getName());
        sb.append(" hasRxRy=").append(hasRxRy);
        for (InputDevice.MotionRange range : device.getMotionRanges()) {
            int axis = range.getAxis();
            float value = event.getAxisValue(axis);
            sb.append('\n')
                    .append("  axis=").append(MotionEvent.axisToString(axis))
                    .append('(').append(axis).append(')')
                    .append(" val=").append(value)
                    .append(" min=").append(range.getMin())
                    .append(" max=").append(range.getMax())
                    .append(" flat=").append(range.getFlat());
        }
        Log.d(TAG, sb.toString());
    }

    private String formatAxisLine(int axis, float value, InputDevice.MotionRange range) {
        return String.format(Locale.US, "%s(%d) val=%.3f min=%.1f max=%.1f",
                MotionEvent.axisToString(axis), axis, value, range.getMin(), range.getMax());
    }

    private String keyActionName(int action) {
        return action == KeyEvent.ACTION_DOWN ? "DOWN" : "UP";
    }

    private void setDiagnosticLines(String line) {
        diagnosticLines = new String[]{line};
    }

    private boolean isPressedAny(int... keyCodes) {
        for (int keyCode : keyCodes) {
            if (pressedKeys.contains(keyCode)) return true;
        }
        return false;
    }

    private boolean isLeftTriggerKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_BUTTON_L2 || keyCode == KeyEvent.KEYCODE_BUTTON_7;
    }

    private boolean isRightTriggerKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_BUTTON_R2 || keyCode == KeyEvent.KEYCODE_BUTTON_8;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float cx = w * 0.5f;
        float cy = h * 0.52f;
        float bodyRadiusX = w * 0.46f;
        float bodyRadiusY = h * 0.42f;

        RectF body = new RectF(cx - bodyRadiusX, cy - bodyRadiusY, cx + bodyRadiusX, cy + bodyRadiusY);
        canvas.drawRoundRect(body, 72f, 72f, basePaint);
        canvas.drawRoundRect(body, 72f, 72f, outlinePaint);

        float stickBaseR = Math.min(w, h) * 0.11f;
        float stickThumbR = stickBaseR * 0.45f;

        drawStick(canvas, w * 0.28f, h * 0.66f, stickBaseR, stickThumbR, leftStickX, leftStickY, "L");
        drawStick(canvas, w * 0.72f, h * 0.66f, stickBaseR, stickThumbR, rightStickX, rightStickY, "R");

        drawFaceButtons(canvas, w * 0.82f, h * 0.44f, Math.min(w, h) * 0.045f);
        drawDpad(canvas, w * 0.18f, h * 0.46f, Math.min(w, h) * 0.04f);
        drawShoulderAndCenter(canvas, w, h);
        drawDiagnosticOverlay(canvas, w, h);
    }

    private void drawStick(Canvas canvas, float cx, float cy, float baseR, float thumbR, float axisX, float axisY, String label) {
        canvas.drawCircle(cx, cy, baseR, controlPaint);
        canvas.drawCircle(cx, cy, baseR, outlinePaint);
        float thumbX = cx + axisX * (baseR - thumbR);
        float thumbY = cy + axisY * (baseR - thumbR);
        canvas.drawCircle(thumbX, thumbY, thumbR, activePaint);

        textPaint.setTextSize(baseR * 0.42f);
        canvas.drawText(label, cx, cy + (baseR * 1.55f), textPaint);

        boolean pressed = "L".equals(label)
                ? isPressedAny(KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_11)
                : isPressedAny(KeyEvent.KEYCODE_BUTTON_THUMBR, KeyEvent.KEYCODE_BUTTON_12);
        if (pressed) {
            Paint glow = new Paint(activePaint);
            glow.setAlpha(90);
            canvas.drawCircle(cx, cy, baseR * 1.12f, glow);
        }
    }

    private void drawFaceButtons(Canvas canvas, float cx, float cy, float r) {
        drawLabeledButton(canvas, cx, cy - r * 1.8f, r, "Y", KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_4);
        drawLabeledButton(canvas, cx - r * 1.8f, cy, r, "X", KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_3);
        drawLabeledButton(canvas, cx + r * 1.8f, cy, r, "B", KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_2);
        drawLabeledButton(canvas, cx, cy + r * 1.8f, r, "A", KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_1);
    }

    private void drawDpad(Canvas canvas, float cx, float cy, float size) {
        drawDpadKey(canvas, cx, cy - size * 1.7f, size, "^", KeyEvent.KEYCODE_DPAD_UP, hatY < -0.5f);
        drawDpadKey(canvas, cx, cy + size * 1.7f, size, "v", KeyEvent.KEYCODE_DPAD_DOWN, hatY > 0.5f);
        drawDpadKey(canvas, cx - size * 1.7f, cy, size, "<", KeyEvent.KEYCODE_DPAD_LEFT, hatX < -0.5f);
        drawDpadKey(canvas, cx + size * 1.7f, cy, size, ">", KeyEvent.KEYCODE_DPAD_RIGHT, hatX > 0.5f);
    }

    private void drawDpadKey(Canvas canvas, float cx, float cy, float size, String label, int keyCode, boolean axisPressed) {
        RectF rect = new RectF(cx - size, cy - size, cx + size, cy + size);
        boolean active = isPressedAny(keyCode) || axisPressed;
        canvas.drawRoundRect(rect, 8f, 8f, controlPaint);
        if (active) canvas.drawRoundRect(rect, 8f, 8f, activePaint);
        canvas.drawRoundRect(rect, 8f, 8f, outlinePaint);
        textPaint.setTextSize(size * 0.95f);
        canvas.drawText(label, cx, cy + (size * 0.35f), textPaint);
    }

    private void drawShoulderAndCenter(Canvas canvas, float w, float h) {
        float shoulderW = w * 0.17f;
        float shoulderH = h * 0.07f;

        RectF l1 = new RectF(w * 0.11f, h * 0.16f, w * 0.11f + shoulderW, h * 0.16f + shoulderH);
        RectF r1 = new RectF(w * 0.72f, h * 0.16f, w * 0.72f + shoulderW, h * 0.16f + shoulderH);
        RectF l2 = new RectF(w * 0.11f, h * 0.25f, w * 0.11f + shoulderW, h * 0.25f + shoulderH);
        RectF r2 = new RectF(w * 0.72f, h * 0.25f, w * 0.72f + shoulderW, h * 0.25f + shoulderH);

        drawShoulderButton(canvas, l1, "LB", h * 0.04f, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_5);
        drawShoulderButton(canvas, r1, "RB", h * 0.04f, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_6);
        drawTriggerButton(canvas, l2, "LT", leftTrigger > 0f);
        drawTriggerButton(canvas, r2, "RT", rightTrigger > 0f);

        drawLabeledButton(canvas, w * 0.46f, h * 0.48f, h * 0.032f, "SEL", KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_9);
        drawLabeledButton(canvas, w * 0.54f, h * 0.48f, h * 0.032f, "ST", KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_10);
    }

    private void drawShoulderButton(Canvas canvas, RectF rect, String label, float textSize, int... keyCodes) {
        canvas.drawRoundRect(rect, 12f, 12f, controlPaint);
        if (isPressedAny(keyCodes)) canvas.drawRoundRect(rect, 12f, 12f, activePaint);
        canvas.drawRoundRect(rect, 12f, 12f, outlinePaint);

        textPaint.setTextSize(textSize);
        canvas.drawText(label, rect.centerX(), rect.centerY() + (textSize * 0.32f), textPaint);
    }

    private void drawTriggerButton(Canvas canvas, RectF rect, String label, boolean pressed) {
        canvas.drawRoundRect(rect, 12f, 12f, controlPaint);
        if (pressed) canvas.drawRoundRect(rect, 12f, 12f, activePaint);
        canvas.drawRoundRect(rect, 12f, 12f, outlinePaint);

        float textSize = rect.height() * 0.58f;
        textPaint.setTextSize(textSize);
        canvas.drawText(label, rect.centerX(), rect.centerY() + (textSize * 0.32f), textPaint);
    }

    private void drawDiagnosticOverlay(Canvas canvas, float w, float h) {
        if (!ENABLE_TRIGGER_DIAGNOSTICS || diagnosticLines == null || diagnosticLines.length == 0) return;

        float padding = Math.max(8f, Math.min(w, h) * 0.018f);
        float textSize = Math.max(12f, Math.min(w, h) * 0.025f);
        float lineHeight = textSize * 1.25f;
        float x = padding;
        float y = h - padding - (lineHeight * diagnosticLines.length);
        float bgHeight = (lineHeight * diagnosticLines.length) + (padding * 1.4f);
        RectF bg = new RectF(padding * 0.5f, y - padding, w - (padding * 0.5f), y - padding + bgHeight);

        canvas.drawRoundRect(bg, 14f, 14f, diagnosticBgPaint);
        diagnosticTextPaint.setTextSize(textSize);
        for (int i = 0; i < diagnosticLines.length; i++) {
            canvas.drawText(diagnosticLines[i], x, y + (lineHeight * i), diagnosticTextPaint);
        }
    }

    private void drawLabeledButton(Canvas canvas, float cx, float cy, float r, String label, int... keyCodes) {
        canvas.drawCircle(cx, cy, r, controlPaint);
        if (isPressedAny(keyCodes)) canvas.drawCircle(cx, cy, r, activePaint);
        canvas.drawCircle(cx, cy, r, outlinePaint);
        textPaint.setTextSize(r * 0.9f);
        canvas.drawText(label, cx, cy + (r * 0.32f), textPaint);
    }
}
