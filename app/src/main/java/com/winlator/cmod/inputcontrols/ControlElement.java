package com.winlator.cmod.inputcontrols;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.core.graphics.ColorUtils;

import com.winlator.cmod.core.CubicBezierInterpolator;
import com.winlator.cmod.math.Mathf;
import com.winlator.cmod.widget.InputControlsView;
import com.winlator.cmod.widget.TouchpadView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class ControlElement {
    public static final float STICK_DEAD_ZONE = 0.15f;
    public static final float DPAD_DEAD_ZONE = 0.3f;
    public static final float STICK_SENSITIVITY = 3.0f;
    public static final float TRACKPAD_MIN_SPEED = 0.8f;
    public static final float TRACKPAD_MAX_SPEED = 20.0f;
    public static final byte TRACKPAD_ACCELERATION_THRESHOLD = 4;
    public static final short BUTTON_MIN_TIME_TO_KEEP_PRESSED = 300;
    public enum Type {
        BUTTON, D_PAD, RANGE_BUTTON, STICK, TRACKPAD, SWIPE_PAD, RADIAL_MENU;

        public static String[] names() {
            Type[] types = values();
            String[] names = new String[types.length];
            for (int i = 0; i < types.length; i++) names[i] = types[i].name().replace("_", "-");
            return names;
        }
    }
    public enum Shape {
        CIRCLE, RECT, ROUND_RECT, SQUARE;

        public static String[] names() {
            Shape[] shapes = values();
            String[] names = new String[shapes.length];
            for (int i = 0; i < shapes.length; i++) names[i] = shapes[i].name().replace("_", " ");
            return names;
        }
    }
    public enum Range {
        FROM_A_TO_Z(26), FROM_0_TO_9(10), FROM_F1_TO_F12(12), FROM_NP0_TO_NP9(10);
        public final byte max;

        Range(int max) {
            this.max = (byte)max;
        }

        public static String[] names() {
            Range[] ranges = values();
            String[] names = new String[ranges.length];
            for (int i = 0; i < ranges.length; i++) names[i] = ranges[i].name().replace("_", " ");
            return names;
        }
    }
    private final InputControlsView inputControlsView;
    private Type type = Type.BUTTON;
    private Shape shape = Shape.CIRCLE;
    private Binding[] bindings = {Binding.NONE, Binding.NONE, Binding.NONE, Binding.NONE};
    private float scale = 1.0f;
    private short x;
    private short y;
    private boolean selected = false;
    private boolean toggleSwitch = false;
    private int currentPointerId = -1;
    private final Rect boundingBox = new Rect();
    private boolean[] states = new boolean[4];
    private boolean boundingBoxNeedsUpdate = true;
    private String text = "";
    private byte iconId;
    private String customIconPath = "";
    private transient Bitmap customIconBitmap;
    private Range range;
    private byte orientation;
    private PointF currentPosition;
    private RangeScroller scroller;
    private CubicBezierInterpolator interpolator;
    private Object touchTime;
    // SWIPE_PAD: [0]=center, [1]=up, [2]=right, [3]=down, [4]=left
    private String[] swipePadTexts = {"", "", "", "", ""};
    private long swipePadTouchStartTime = -1;
    private float swipePadStartX, swipePadStartY;
    // alpha for direction indicators: 0=hidden, 255=fully visible
    private int swipePadAlpha = 0;
    private static final long SWIPE_PAD_SHOW_DELAY_MS = 200;
    // RADIAL_MENU
    private boolean visible = false;
    private Path[] paths;

    public ControlElement(InputControlsView inputControlsView) {
        this.inputControlsView = inputControlsView;
    }

    private void reset() {
        setBinding(Binding.NONE);
        scroller = null;

        if (type == Type.D_PAD || type == Type.STICK) {
            bindings[0] = Binding.KEY_W;
            bindings[1] = Binding.KEY_D;
            bindings[2] = Binding.KEY_S;
            bindings[3] = Binding.KEY_A;
        }
        else if (type == Type.TRACKPAD) {
            bindings[0] = Binding.MOUSE_MOVE_UP;
            bindings[1] = Binding.MOUSE_MOVE_RIGHT;
            bindings[2] = Binding.MOUSE_MOVE_DOWN;
            bindings[3] = Binding.MOUSE_MOVE_LEFT;
        }
        else if (type == Type.RANGE_BUTTON) {
            scroller = new RangeScroller(inputControlsView, this);
        }
        else if (type == Type.SWIPE_PAD) {
            // bindings[0]=up, [1]=right, [2]=down, [3]=left (center tap = none)
            bindings[0] = Binding.KEY_1;
            bindings[1] = Binding.KEY_2;
            bindings[2] = Binding.KEY_3;
            bindings[3] = Binding.KEY_4;
            swipePadTexts = new String[]{"SWIPE", "1", "2", "3", "4"};
        }
        else if (type == Type.RADIAL_MENU) {
            setBindingCount(3);
        }

        text = "";
        iconId = 0;
        customIconPath = "";
        customIconBitmap = null;
        range = null;
        visible = false;
        paths = null;
        boundingBoxNeedsUpdate = true;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
        reset();
    }

    public int getBindingCount() {
        return bindings.length;
    }

    public void setBindingCount(int bindingCount) {
        int oldBindingCount = bindings.length;
        bindings = Arrays.copyOf(bindings, bindingCount);
        if (bindingCount > oldBindingCount) {
            Arrays.fill(bindings, oldBindingCount, bindingCount, Binding.NONE);
        }
        states = Arrays.copyOf(states, bindingCount);
        boundingBoxNeedsUpdate = true;
    }

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
        boundingBoxNeedsUpdate = true;
    }

    public Range getRange() {
        return range != null ? range : Range.FROM_A_TO_Z;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public byte getOrientation() {
        return orientation;
    }

    public void setOrientation(byte orientation) {
        this.orientation = orientation;
        boundingBoxNeedsUpdate = true;
    }

    public boolean isToggleSwitch() {
        return toggleSwitch;
    }

    public void setToggleSwitch(boolean toggleSwitch) {
        this.toggleSwitch = toggleSwitch;
    }

    public Binding getBindingAt(int index) {
        return index < bindings.length ? bindings[index] : Binding.NONE;
    }

    public void setBindingAt(int index, Binding binding) {
        if (index >= bindings.length) {
            int oldLength = bindings.length;
            bindings = Arrays.copyOf(bindings, index+1);
            Arrays.fill(bindings, oldLength, bindings.length, Binding.NONE);
            states = Arrays.copyOf(states, bindings.length);
            boundingBoxNeedsUpdate = true;
        }
        bindings[index] = binding;
    }

    public void setBinding(Binding binding) {
        Arrays.fill(bindings, binding);
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        boundingBoxNeedsUpdate = true;
    }

    public short getX() {
        return x;
    }

    public void setX(int x) {
        this.x = (short)x;
        boundingBoxNeedsUpdate = true;
    }

    public short getY() {
        return y;
    }

    public void setY(int y) {
        this.y = (short)y;
        boundingBoxNeedsUpdate = true;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        if (type == Type.RADIAL_MENU) visible = selected;
        this.selected = selected;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    public byte getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = (byte)iconId;
    }

    public String getCustomIconPath() {
        return customIconPath;
    }

    public void setCustomIconPath(String customIconPath) {
        this.customIconPath = customIconPath != null ? customIconPath : "";
        this.customIconBitmap = null;
    }

    public Rect getBoundingBox() {
        if (boundingBoxNeedsUpdate) computeBoundingBox();
        return boundingBox;
    }

    private Rect computeBoundingBox() {
        int snappingSize = inputControlsView.getSnappingSize();
        int halfWidth = 0;
        int halfHeight = 0;

        switch (type) {
            case BUTTON:
                switch (shape) {
                    case RECT:
                    case ROUND_RECT:
                        halfWidth = snappingSize * 4;
                        halfHeight = snappingSize * 2;
                        break;
                    case SQUARE:
                        halfWidth = (int)(snappingSize * 2.5f);
                        halfHeight = (int)(snappingSize * 2.5f);
                        break;
                    case CIRCLE:
                        halfWidth = snappingSize * 3;
                        halfHeight = snappingSize * 3;
                        break;
                }
                break;

            case D_PAD: {
                halfWidth = snappingSize * 7;
                halfHeight = snappingSize * 7;
                break;
            }
            case TRACKPAD:
            case STICK: {
                halfWidth = snappingSize * 6;
                halfHeight = snappingSize * 6;
                break;
            }
            case RANGE_BUTTON: {
                halfWidth = snappingSize * ((bindings.length * 4) / 2);
                halfHeight = snappingSize * 2;

                if (orientation == 1) {
                    int tmp = halfWidth;
                    halfWidth = halfHeight;
                    halfHeight = tmp;
                }
                break;
            }
            case SWIPE_PAD: {
                halfWidth = (int)(snappingSize * 3.5f);
                halfHeight = (int)(snappingSize * 3.5f);
                break;
            }
            case RADIAL_MENU:
                halfWidth = snappingSize * 3;
                halfHeight = snappingSize * 3;
                break;
        }

        halfWidth *= scale;
        halfHeight *= scale;
        boundingBox.set(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight);
        boundingBoxNeedsUpdate = false;
        paths = null;
        return boundingBox;
    }

    private String getBindingTextAt(int index) {
        Binding binding = getBindingAt(index);
        String text = binding.toString().replace("NUMPAD ", "NP").replace("BUTTON ", "");
        if (text.length() > 7) {
            String[] parts = text.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) sb.append(part.charAt(0));
            return (binding.isMouse() ? "M" : "")+sb;
        }
        else return text;
    }



    private String getDisplayText() {
        if (text != null && !text.isEmpty()) {
            return text;
        }
        else {
            Binding binding = getBindingAt(0);
            String text = binding.toString().replace("NUMPAD ", "NP").replace("BUTTON ", "");
            if (text.length() > 7) {
                String[] parts = text.split(" ");
                StringBuilder sb = new StringBuilder();
                for (String part : parts) sb.append(part.charAt(0));
                return (binding.isMouse() ? "M" : "")+ sb;
            }
            else return text;
        }
    }

    private static float getTextSizeForWidth(Paint paint, String text, float desiredWidth) {
        final byte testTextSize = 48;
        paint.setTextSize(testTextSize);
        return testTextSize * desiredWidth / paint.measureText(text);
    }

    private static String getRangeTextForIndex(Range range, int index) {
        String text = "";
        switch (range) {
            case FROM_A_TO_Z:
                text = String.valueOf((char)(65 + index));
                break;
            case FROM_0_TO_9:
                text = String.valueOf((index + 1) % 10);
                break;
            case FROM_F1_TO_F12:
                text = "F"+(index + 1);
                break;
            case FROM_NP0_TO_NP9:
                text = "NP"+((index + 1) % 10);
                break;
        }
        return text;
    }

    public void draw(Canvas canvas) {
        int snappingSize = inputControlsView.getSnappingSize();
        Paint paint = inputControlsView.getPaint();
        int primaryColor = inputControlsView.getPrimaryColor();

        paint.setColor(selected ? inputControlsView.getSecondaryColor() : primaryColor);
        paint.setStyle(Paint.Style.STROKE);
        float strokeWidth = snappingSize * 0.25f;
        paint.setStrokeWidth(strokeWidth);
        Rect boundingBox = getBoundingBox();

        switch (type) {
            case BUTTON: {
                float cx = boundingBox.centerX();
                float cy = boundingBox.centerY();


                if (states[0]) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(ColorUtils.setAlphaComponent(0xFF00BFFF, 77));
                    switch (shape) {
                        case CIRCLE:
                            canvas.drawCircle(cx, cy, boundingBox.width() * 0.5f, paint);
                            break;
                        case RECT:
                            canvas.drawRect(boundingBox, paint);
                            break;
                        case ROUND_RECT: {
                            float radius = boundingBox.height() * 0.5f;
                            canvas.drawRoundRect(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom, radius, radius, paint);
                            break;
                        }
                        case SQUARE: {
                            float radius = snappingSize * 0.75f * scale;
                            canvas.drawRoundRect(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom, radius, radius, paint);
                            break;
                        }
                    }

                    paint.setColor(selected ? inputControlsView.getSecondaryColor() : primaryColor);
                    paint.setStyle(Paint.Style.STROKE);
                }

                if (!states[0]) {
                    switch (shape) {
                        case CIRCLE:
                            canvas.drawCircle(cx, cy, boundingBox.width() * 0.5f, paint);
                            break;
                        case RECT:
                            canvas.drawRect(boundingBox, paint);
                            break;
                        case ROUND_RECT: {
                            float radius = boundingBox.height() * 0.5f;
                            canvas.drawRoundRect(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom, radius, radius, paint);
                            break;
                        }
                        case SQUARE: {
                            float radius = snappingSize * 0.75f * scale;
                            canvas.drawRoundRect(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom, radius, radius, paint);
                            break;
                        }
                    }
                }


                if (!customIconPath.isEmpty()) {
                    drawCustomIcon(canvas, cx, cy, boundingBox.width(), boundingBox.height());
                }
                else if (iconId > 0) {
                    drawIcon(canvas, cx, cy, boundingBox.width(), boundingBox.height(), iconId);
                }
                else {
                    String text = getDisplayText();
                    paint.setTextSize(Math.min(getTextSizeForWidth(paint, text, boundingBox.width() - strokeWidth * 2), snappingSize * 2 * scale));
                    paint.setTextAlign(Paint.Align.CENTER);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(primaryColor);
                    canvas.drawText(text, x, (y - ((paint.descent() + paint.ascent()) * 0.5f)), paint);
                }
                break;
            }
            case D_PAD: {
                float cx = boundingBox.centerX();
                float cy = boundingBox.centerY();
                float offsetX = snappingSize * 2 * scale;
                float offsetY = snappingSize * 3 * scale;
                float start = snappingSize * scale;
                Path path = inputControlsView.getPath();


                paint.setStyle(Paint.Style.FILL);
                paint.setColor(ColorUtils.setAlphaComponent(0xFF00BFFF, 77));


                if (states[0]) {
                    path.reset();
                    path.moveTo(cx, cy - start);
                    path.lineTo(cx - offsetX, cy - offsetY);
                    path.lineTo(cx - offsetX, boundingBox.top);
                    path.lineTo(cx + offsetX, boundingBox.top);
                    path.lineTo(cx + offsetX, cy - offsetY);
                    path.close();
                    canvas.drawPath(path, paint);
                }


                if (states[3]) {
                    path.reset();
                    path.moveTo(cx - start, cy);
                    path.lineTo(cx - offsetY, cy - offsetX);
                    path.lineTo(boundingBox.left, cy - offsetX);
                    path.lineTo(boundingBox.left, cy + offsetX);
                    path.lineTo(cx - offsetY, cy + offsetX);
                    path.close();
                    canvas.drawPath(path, paint);
                }


                if (states[2]) {
                    path.reset();
                    path.moveTo(cx, cy + start);
                    path.lineTo(cx - offsetX, cy + offsetY);
                    path.lineTo(cx - offsetX, boundingBox.bottom);
                    path.lineTo(cx + offsetX, boundingBox.bottom);
                    path.lineTo(cx + offsetX, cy + offsetY);
                    path.close();
                    canvas.drawPath(path, paint);
                }


                if (states[1]) {
                    path.reset();
                    path.moveTo(cx + start, cy);
                    path.lineTo(cx + offsetY, cy - offsetX);
                    path.lineTo(boundingBox.right, cy - offsetX);
                    path.lineTo(boundingBox.right, cy + offsetX);
                    path.lineTo(cx + offsetY, cy + offsetX);
                    path.close();
                    canvas.drawPath(path, paint);
                }


                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(selected ? inputControlsView.getSecondaryColor() : primaryColor);
                path.reset();

                path.moveTo(cx, cy - start);
                path.lineTo(cx - offsetX, cy - offsetY);
                path.lineTo(cx - offsetX, boundingBox.top);
                path.lineTo(cx + offsetX, boundingBox.top);
                path.lineTo(cx + offsetX, cy - offsetY);
                path.close();

                path.moveTo(cx - start, cy);
                path.lineTo(cx - offsetY, cy - offsetX);
                path.lineTo(boundingBox.left, cy - offsetX);
                path.lineTo(boundingBox.left, cy + offsetX);
                path.lineTo(cx - offsetY, cy + offsetX);
                path.close();

                path.moveTo(cx, cy + start);
                path.lineTo(cx - offsetX, cy + offsetY);
                path.lineTo(cx - offsetX, boundingBox.bottom);
                path.lineTo(cx + offsetX, boundingBox.bottom);
                path.lineTo(cx + offsetX, cy + offsetY);
                path.close();

                path.moveTo(cx + start, cy);
                path.lineTo(cx + offsetY, cy - offsetX);
                path.lineTo(boundingBox.right, cy - offsetX);
                path.lineTo(boundingBox.right, cy + offsetX);
                path.lineTo(cx + offsetY, cy + offsetX);
                path.close();

                canvas.drawPath(path, paint);
                break;
            }
            case RANGE_BUTTON: {
                Range range = getRange();
                int oldColor = paint.getColor();
                float radius = snappingSize * 0.75f * scale;
                float elementSize = scroller.getElementSize();
                float minTextSize = snappingSize * 2 * scale;
                float scrollOffset = scroller.getScrollOffset();
                byte[] rangeIndex = scroller.getRangeIndex();
                Path path = inputControlsView.getPath();
                path.reset();

                if (orientation == 0) {
                    float lineTop = boundingBox.top + strokeWidth * 0.5f;
                    float lineBottom = boundingBox.bottom - strokeWidth * 0.5f;
                    float startX = boundingBox.left;
                    canvas.drawRoundRect(startX, boundingBox.top, boundingBox.right, boundingBox.bottom, radius, radius, paint);

                    canvas.save();
                    path.addRoundRect(startX, boundingBox.top, boundingBox.right, boundingBox.bottom, radius, radius, Path.Direction.CW);
                    canvas.clipPath(path);
                    startX -= scrollOffset % elementSize;

                    for (byte i = rangeIndex[0]; i < rangeIndex[1]; i++) {
                        int index = i % range.max;
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(oldColor);


                        if (scroller.getPressedIndex() == index) {
                            paint.setStyle(Paint.Style.FILL);
                            paint.setColor(ColorUtils.setAlphaComponent(0xFF00BFFF, 77));
                            canvas.drawRoundRect(startX, boundingBox.top, startX + elementSize, boundingBox.bottom, radius, radius, paint);
                        }

                        if (startX > boundingBox.left && startX  < boundingBox.right) canvas.drawLine(startX, lineTop, startX, lineBottom, paint);
                        String text = getRangeTextForIndex(range, index);

                        if (startX < boundingBox.right && startX + elementSize > boundingBox.left) {
                            paint.setStyle(Paint.Style.FILL);
                            paint.setColor(primaryColor);
                            paint.setTextSize(Math.min(getTextSizeForWidth(paint, text, elementSize - strokeWidth * 2), minTextSize));
                            paint.setTextAlign(Paint.Align.CENTER);
                            canvas.drawText(text, startX + elementSize * 0.5f, (y - ((paint.descent() + paint.ascent()) * 0.5f)), paint);
                        }
                        startX += elementSize;
                    }

                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(oldColor);
                    canvas.restore();
                }
                else {
                    float lineLeft = boundingBox.left + strokeWidth * 0.5f;
                    float lineRight = boundingBox.right - strokeWidth * 0.5f;
                    float startY = boundingBox.top;
                    canvas.drawRoundRect(boundingBox.left, startY, boundingBox.right, boundingBox.bottom, radius, radius, paint);

                    canvas.save();
                    path.addRoundRect(boundingBox.left, startY, boundingBox.right, boundingBox.bottom, radius, radius, Path.Direction.CW);
                    canvas.clipPath(inputControlsView.getPath());
                    startY -= scrollOffset % elementSize;

                    for (byte i = rangeIndex[0]; i < rangeIndex[1]; i++) {
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(oldColor);

                        if (scroller.getPressedIndex() == i) {
                            paint.setStyle(Paint.Style.FILL);
                            paint.setColor(ColorUtils.setAlphaComponent(0xFF00BFFF, 77));
                            canvas.drawRoundRect(boundingBox.left, startY, boundingBox.right, startY + elementSize, radius, radius, paint);
                        }

                        if (startY > boundingBox.top && startY < boundingBox.bottom) canvas.drawLine(lineLeft, startY, lineRight, startY, paint);
                        String text = getRangeTextForIndex(range, i);

                        if (startY < boundingBox.bottom && startY + elementSize > boundingBox.top) {
                            paint.setStyle(Paint.Style.FILL);
                            paint.setColor(primaryColor);
                            paint.setTextSize(Math.min(getTextSizeForWidth(paint, text, boundingBox.width() - strokeWidth * 2), minTextSize));
                            paint.setTextAlign(Paint.Align.CENTER);
                            canvas.drawText(text, x, startY + elementSize * 0.5f - ((paint.descent() + paint.ascent()) * 0.5f), paint);
                        }
                        startY += elementSize;
                    }

                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(oldColor);
                    canvas.restore();
                }
                break;
            }
            case STICK: {
                int cx = boundingBox.centerX();  // Fixed outer circle center
                int cy = boundingBox.centerY();  // Fixed outer circle center
                int oldColor = paint.getColor();

                // Draw the outer circle (base of the stick)
                canvas.drawCircle(cx, cy, boundingBox.height() * 0.5f, paint);

                // Draw the inner thumbstick (current position based on gyroscope movement)
                float thumbstickX = getCurrentPosition().x;
                float thumbstickY = getCurrentPosition().y;

                short thumbRadius = (short) (snappingSize * 3.5f * scale); // Radius of the thumbstick
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(ColorUtils.setAlphaComponent(primaryColor, 50)); // Semi-transparent fill for thumbstick
                canvas.drawCircle(thumbstickX, thumbstickY, thumbRadius, paint); // Draw thumbstick

                // Draw the thumbstick border
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(oldColor);
                canvas.drawCircle(thumbstickX, thumbstickY, thumbRadius + strokeWidth * 0.5f, paint);
                break;
            }

            case TRACKPAD: {
                float radius = boundingBox.height() * 0.15f;
                canvas.drawRoundRect(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom, radius, radius, paint);
                float offset = strokeWidth * 2.5f;
                float innerStrokeWidth = strokeWidth * 2;
                float innerHeight = boundingBox.height() - offset * 2;
                radius = (innerHeight / boundingBox.height()) * radius - (innerStrokeWidth * 0.5f + strokeWidth * 0.5f);
                paint.setStrokeWidth(innerStrokeWidth);
                canvas.drawRoundRect(boundingBox.left + offset, boundingBox.top + offset, boundingBox.right - offset, boundingBox.bottom - offset, radius, radius, paint);
                break;
            }
            case SWIPE_PAD: {
                drawSwipePad(canvas, paint, primaryColor, strokeWidth, boundingBox, snappingSize);
                break;
            }
            case RADIAL_MENU: {
                float startAngle = 0;
                float cx = boundingBox.centerX();
                float cy = boundingBox.centerY();
                float innerRadius = boundingBox.width() * 0.5f + snappingSize * 0.5f;
                float outerRadius = boundingBox.width() + snappingSize * scale;
                float radius = boundingBox.width() * 0.5f;
                int oldColor = paint.getColor();

                if (paths == null) {
                    Path path0 = new Path();
                    Path path1 = new Path();
                    RectF outerOval = new RectF(cx - outerRadius, cy - outerRadius, cx + outerRadius, cy + outerRadius);
                    RectF innerOval = new RectF(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius);
                    float outerMargin = (float)Math.toRadians(2);
                    float innerMargin = outerMargin * (outerRadius / innerRadius);

                    for (int i = 0; i <= bindings.length; i++) {
                        float t = (float)i / bindings.length;
                        float endAngle = (float)(t * Math.PI * 2 + Math.PI * 1.5f);

                        if (i > 0) {
                            path0.moveTo((float)(cx + Math.cos(startAngle + innerMargin) * innerRadius), (float)(cy + Math.sin(startAngle + innerMargin) * innerRadius));
                            path0.lineTo((float)(cx + Math.cos(startAngle + outerMargin) * outerRadius), (float)(cy + Math.sin(startAngle + outerMargin) * outerRadius));
                            path0.arcTo(outerOval, (float)Math.toDegrees(startAngle + outerMargin), (float)Math.toDegrees(endAngle - startAngle - outerMargin * 2));
                            path0.lineTo((float)(cx + Math.cos(endAngle - innerMargin) * innerRadius), (float)(cy + Math.sin(endAngle - innerMargin) * innerRadius));
                            path0.arcTo(innerOval, (float)Math.toDegrees(endAngle - innerMargin), (float)-Math.toDegrees(endAngle - startAngle - innerMargin * 2));

                            float middleAngle = (startAngle + endAngle) * 0.5f;
                            float endX = (float)(cx + Math.cos(middleAngle) * radius * 0.5f);
                            float endY = (float)(cy + Math.sin(middleAngle) * radius * 0.5f);
                            path1.moveTo(cx, cy);
                            path1.lineTo(endX, endY);
                            path1.addCircle(endX, endY, snappingSize * 0.4f, Path.Direction.CW);
                        }

                        startAngle = endAngle;
                    }

                    paths = new Path[]{path0, path1};
                }

                if (visible) {
                    float minTextSize = snappingSize * 2 * scale;
                    int darkColor = getDarkColor();
                    paint.setStrokeCap(Paint.Cap.SQUARE);
                    canvas.drawPath(paths[0], paint);
                    paint.setStrokeCap(Paint.Cap.BUTT);

                    paint.setStyle(Paint.Style.FILL);
                    paint.setTextAlign(Paint.Align.CENTER);
                    float touchAreaRadius = (outerRadius - innerRadius) * 0.5f * 1.25f;

                    for (int i = 0, j = 0; i <= bindings.length; i++) {
                        float t = (float)i / bindings.length;
                        float endAngle = (float)(t * Math.PI * 2 + Math.PI * 1.5f);

                        if (i > 0) {
                            float middleAngle = (startAngle + endAngle) * 0.5f;
                            float touchAreaCenter = (innerRadius + outerRadius) * 0.5f;
                            float touchAreaX = (short)(cx + Math.cos(middleAngle) * touchAreaCenter);
                            float touchAreaY = (short)(cy + Math.sin(middleAngle) * touchAreaCenter);

                            canvas.save();
                            canvas.translate(touchAreaX, touchAreaY);
                            float textAngle = (float)Math.toDegrees(middleAngle + Math.PI * 0.5f) % 360;
                            if (textAngle > 90 && textAngle <= 270) textAngle += 180;
                            canvas.rotate(textAngle);
                            String text = getBindingTextAt(j++);
                            paint.setTextSize(Math.min(getTextSizeForWidth(paint, text, touchAreaRadius * 2), minTextSize));
                            paint.setColor(Color.WHITE);
                            canvas.drawText(text, 0, -((paint.descent() + paint.ascent()) * 0.5f), paint);
                            canvas.restore();
                        }

                        startAngle = endAngle;
                    }

                    drawIcon(canvas, cx, cy, boundingBox.width() * 0.4f, boundingBox.width() * 0.4f, 32, false);
                }
                else {
                    paint.setStyle(Paint.Style.FILL_AND_STROKE);
                    paint.setColor(oldColor);
                    canvas.drawPath(paths[1], paint);
                }

                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(oldColor);
                canvas.drawCircle(cx, cy, radius, paint);
                break;
            }
        }
    }

    private void drawIcon(Canvas canvas, float cx, float cy, float width, float height, int iconId) {
        drawIcon(canvas, cx, cy, width, height, iconId, true);
    }

    private void drawIcon(Canvas canvas, float cx, float cy, float width, float height, int iconId, boolean automargin) {
        Paint paint = inputControlsView.getPaint();
        Bitmap icon = inputControlsView.getIcon((byte)iconId);
        if (icon == null) return;
        if (inputControlsView.shouldTintIcon((byte)iconId)) {
            paint.setColorFilter(inputControlsView.getColorFilter());
        }
        int margin = automargin ? (int)(inputControlsView.getSnappingSize() * (shape == Shape.CIRCLE || shape == Shape.SQUARE ? 2.0f : 1.0f) * scale) : 0;
        int halfSize = (int)((Math.min(width, height) - margin) * 0.5f);

        Rect srcRect = new Rect(0, 0, icon.getWidth(), icon.getHeight());
        Rect dstRect = new Rect((int)(cx - halfSize), (int)(cy - halfSize), (int)(cx + halfSize), (int)(cy + halfSize));
        canvas.drawBitmap(icon, srcRect, dstRect, paint);
        paint.setColorFilter(null);
    }

    private void drawCustomIcon(Canvas canvas, float cx, float cy, float width, float height) {
        if (customIconBitmap == null && customIconPath != null && !customIconPath.isEmpty()) {
            customIconBitmap = BitmapFactory.decodeFile(customIconPath);
        }
        if (customIconBitmap == null) return;

        Paint paint = inputControlsView.getPaint();
        int margin = (int)(inputControlsView.getSnappingSize() * (shape == Shape.CIRCLE || shape == Shape.SQUARE ? 2.0f : 1.0f) * scale);
        int halfSize = (int)((Math.min(width, height) - margin) * 0.5f);
        Rect srcRect = new Rect(0, 0, customIconBitmap.getWidth(), customIconBitmap.getHeight());
        Rect dstRect = new Rect((int)(cx - halfSize), (int)(cy - halfSize), (int)(cx + halfSize), (int)(cy + halfSize));
        canvas.drawBitmap(customIconBitmap, srcRect, dstRect, paint);
    }

    private void drawSwipePad(Canvas canvas, Paint paint, int primaryColor, float strokeWidth, Rect bb, int snappingSize) {
        float cx = bb.centerX();
        float cy = bb.centerY();
        float half = bb.width() * 0.5f;      // half-size of center square
        float gap = strokeWidth * 2f;         // gap between center and direction squares
        float dirSize = half * 0.75f;         // size of direction squares
        float cornerR = snappingSize * 0.5f * scale;
        float alpha255 = swipePadAlpha;       // 0..255

        // ── Direction squares (drawn only when alpha > 0) ─────────────────────
        if (alpha255 > 0) {
            int fillColor = ColorUtils.setAlphaComponent(0xFF00BFFF, (int)(alpha255 * 0.3f));
            int strokeColor = ColorUtils.setAlphaComponent(primaryColor, (int)alpha255);

            // Direction rects: Up, Right, Down, Left
            float[][] dirs = {
                {cx - dirSize, cy - half - gap - dirSize * 2, cx + dirSize, cy - half - gap},
                {cx + half + gap, cy - dirSize, cx + half + gap + dirSize * 2, cy + dirSize},
                {cx - dirSize, cy + half + gap, cx + dirSize, cy + half + gap + dirSize * 2},
                {cx - half - gap - dirSize * 2, cy - dirSize, cx - half - gap, cy + dirSize}
            };
            boolean[] pressed = {states[0], states[1], states[2], states[3]};
            String[] dTexts = {
                swipePadTexts != null && swipePadTexts.length > 1 ? swipePadTexts[1] : "",
                swipePadTexts != null && swipePadTexts.length > 2 ? swipePadTexts[2] : "",
                swipePadTexts != null && swipePadTexts.length > 3 ? swipePadTexts[3] : "",
                swipePadTexts != null && swipePadTexts.length > 4 ? swipePadTexts[4] : ""
            };

            for (int i = 0; i < 4; i++) {
                float l = dirs[i][0], t = dirs[i][1], r = dirs[i][2], b = dirs[i][3];
                // fill
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(pressed[i] ? ColorUtils.setAlphaComponent(0xFF00BFFF, (int)(alpha255 * 0.5f)) : fillColor);
                canvas.drawRoundRect(l, t, r, b, cornerR, cornerR, paint);
                // stroke
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(strokeWidth);
                paint.setColor(strokeColor);
                canvas.drawRoundRect(l, t, r, b, cornerR, cornerR, paint);
                // text
                if (dTexts[i] != null && !dTexts[i].isEmpty()) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(ColorUtils.setAlphaComponent(primaryColor, (int)alpha255));
                    float tw = r - l;
                    float th = b - t;
                    paint.setTextSize(Math.min(getTextSizeForWidth(paint, dTexts[i], tw - strokeWidth * 2), snappingSize * 1.5f * scale));
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(dTexts[i], l + tw / 2f, t + th / 2f - (paint.descent() + paint.ascent()) * 0.5f, paint);
                }
            }
        }

        // ── Center square (always visible) ────────────────────────────────────
        float r = cornerR * 1.5f;
        // pressed fill
        if (states[0] || states[1] || states[2] || states[3]) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ColorUtils.setAlphaComponent(0xFF00BFFF, 77));
            canvas.drawRoundRect(cx - half, cy - half, cx + half, cy + half, r, r, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(selected ? inputControlsView.getSecondaryColor() : primaryColor);
        canvas.drawRoundRect(cx - half, cy - half, cx + half, cy + half, r, r, paint);

        // center text
        String centerText = (swipePadTexts != null && swipePadTexts.length > 0 && !swipePadTexts[0].isEmpty())
                ? swipePadTexts[0] : (text != null && !text.isEmpty() ? text : "");
        if (!centerText.isEmpty()) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(primaryColor);
            paint.setTextSize(Math.min(getTextSizeForWidth(paint, centerText, half * 2 - strokeWidth * 2), snappingSize * 2 * scale));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(centerText, cx, cy - (paint.descent() + paint.ascent()) * 0.5f, paint);
        }
    }

    public JSONObject toJSONObject() {
        try {
            JSONObject elementJSONObject = new JSONObject();
            elementJSONObject.put("type", type.name());
            elementJSONObject.put("shape", shape.name());

            JSONArray bindingsJSONArray = new JSONArray();
            for (Binding binding : bindings) bindingsJSONArray.put(binding.name());

            elementJSONObject.put("bindings", bindingsJSONArray);
            elementJSONObject.put("scale", Float.valueOf(scale));
            elementJSONObject.put("x", (float)x / inputControlsView.getMaxWidth());
            elementJSONObject.put("y", (float)y / inputControlsView.getMaxHeight());
            elementJSONObject.put("toggleSwitch", toggleSwitch);
            elementJSONObject.put("text", text);
            elementJSONObject.put("iconId", iconId);
            if (customIconPath != null && !customIconPath.isEmpty()) {
                elementJSONObject.put("customIconPath", customIconPath);
            }

            if (type == Type.RANGE_BUTTON && range != null) {
                elementJSONObject.put("range", range.name());
                if (orientation != 0) elementJSONObject.put("orientation", orientation);
            }
            if (type == Type.SWIPE_PAD && swipePadTexts != null) {
                JSONArray textsArray = new JSONArray();
                for (String t : swipePadTexts) textsArray.put(t != null ? t : "");
                elementJSONObject.put("swipePadTexts", textsArray);
            }
            return elementJSONObject;
        }
        catch (JSONException e) {
            return null;
        }
    }

    public boolean containsPoint(float x, float y) {
        if (type == Type.RADIAL_MENU && visible) {
            float outerRadius = boundingBox.width() + inputControlsView.getSnappingSize() * scale;
            return distance(boundingBox.centerX(), boundingBox.centerY(), x, y) < outerRadius;
        }
        else return getBoundingBox().contains((int)(x + 0.5f), (int)(y + 0.5f));
    }

    public String[] getSwipePadTexts() {
        if (swipePadTexts == null) swipePadTexts = new String[]{"","","","",""};
        return swipePadTexts;
    }

    public void setSwipePadTexts(String[] texts) {
        if (texts != null && texts.length == 5) swipePadTexts = texts;
    }

    public void setSwipePadText(int index, String text) {
        if (swipePadTexts == null) swipePadTexts = new String[]{"","","","",""};
        if (index >= 0 && index < 5) swipePadTexts[index] = text != null ? text : "";
    }

    private boolean isKeepButtonPressedAfterMinTime() {
        Binding binding = getBindingAt(0);
        return !toggleSwitch && (binding == Binding.GAMEPAD_BUTTON_L3 || binding == Binding.GAMEPAD_BUTTON_R3);
    }

    public boolean handleTouchDown(int pointerId, float x, float y) {

        if (currentPointerId == -1 && containsPoint(x, y)) {
            currentPointerId = pointerId;
            if (type == Type.BUTTON) {
                states[0] = true;
                inputControlsView.invalidate();
                if (isKeepButtonPressedAfterMinTime()) touchTime = System.currentTimeMillis();
                if (!toggleSwitch || !selected) inputControlsView.handleInputEvent(getBindingAt(0), true);
                return true;
            }
            else if (type == Type.RANGE_BUTTON) {
                scroller.handleTouchDown(x, y);
                return true;
            }
            else if (type == Type.SWIPE_PAD) {
                swipePadTouchStartTime = System.currentTimeMillis();
                swipePadStartX = x;
                swipePadStartY = y;
                swipePadAlpha = 0;
                // Post delayed show of direction indicators
                inputControlsView.postDelayed(() -> {
                    if (swipePadTouchStartTime >= 0) {
                        swipePadAlpha = 255;
                        inputControlsView.invalidate();
                    }
                }, SWIPE_PAD_SHOW_DELAY_MS);
                return true;
            }
            else if (type == Type.RADIAL_MENU) {
                if (visible) {
                    if (distance(boundingBox.centerX(), boundingBox.centerY(), x, y) < (boundingBox.width() * 0.5f)) {
                        visible = false;
                    }
                }
                else visible = true;
                inputControlsView.invalidate();
                return true;
            }
            else {
                if (type == Type.TRACKPAD) {
                    if (currentPosition == null) currentPosition = new PointF();
                    currentPosition.set(x, y);
                }
                return handleTouchMove(pointerId, x, y);
            }
        }
        else return false;
    }

    public boolean handleTouchMove(int pointerId, float x, float y) {
        if (pointerId == currentPointerId && (type == Type.D_PAD || type == Type.STICK || type == Type.TRACKPAD)) {
            float deltaX, deltaY;
            Rect boundingBox = getBoundingBox();
            float radius = boundingBox.width() * 0.5f;
            TouchpadView touchpadView =  inputControlsView.getTouchpadView();

            if (type == Type.TRACKPAD) {
                if (currentPosition == null) currentPosition = new PointF();
                float[] deltaPoint = touchpadView.computeDeltaPoint(currentPosition.x, currentPosition.y, x, y);
                deltaX = deltaPoint[0];
                deltaY = deltaPoint[1];
                currentPosition.set(x, y);
            }
            else {
                float localX = x - boundingBox.left;
                float localY = y - boundingBox.top;
                float offsetX = localX - radius;
                float offsetY = localY - radius;

                float distance = Mathf.lengthSq(radius - localX, radius - localY);
                if (distance > radius * radius) {
                    float angle = (float)Math.atan2(offsetY, offsetX);
                    offsetX = (float)(Math.cos(angle) * radius);
                    offsetY = (float)(Math.sin(angle) * radius);
                }

                deltaX = Mathf.clamp(offsetX / radius, -1, 1);
                deltaY = Mathf.clamp(offsetY / radius, -1, 1);
            }

            if (type == Type.STICK) {
                if (currentPosition == null) currentPosition = new PointF();
                currentPosition.x = boundingBox.left + deltaX * radius + radius;
                currentPosition.y = boundingBox.top + deltaY * radius + radius;
                final boolean[] states = {deltaY <= -STICK_DEAD_ZONE, deltaX >= STICK_DEAD_ZONE, deltaY >= STICK_DEAD_ZONE, deltaX <= -STICK_DEAD_ZONE};

                for (byte i = 0; i < 4; i++) {
                    float value = i == 1 || i == 3 ? deltaX : deltaY;
                    Binding binding = getBindingAt(i);
                    if (binding.isGamepad()) {
                        value = Mathf.clamp(Math.max(0, Math.abs(value) - 0.01f) * Mathf.sign(value) * STICK_SENSITIVITY, -1, 1);
                        inputControlsView.handleInputEvent(binding, true, value);
                        this.states[i] = true;
                    }
                    else {
                        boolean state = binding.isMouseMove() ? (states[i] || states[(i+2)%4]) : states[i];
                        inputControlsView.handleInputEvent(binding, state, value);
                        this.states[i] = state;
                    }
                }

                inputControlsView.invalidate();
            }
            else if (type == Type.TRACKPAD) {
                final boolean[] states = {deltaY <= -TRACKPAD_MIN_SPEED, deltaX >= TRACKPAD_MIN_SPEED, deltaY >= TRACKPAD_MIN_SPEED, deltaX <= -TRACKPAD_MIN_SPEED};
                int cursorDx = 0;
                int cursorDy = 0;

                for (byte i = 0; i < 4; i++) {
                    float value = (i == 1 || i == 3 ? deltaX : deltaY);
                    Binding binding = getBindingAt(i);
                    if (binding.isGamepad()) {
                        if (interpolator == null) interpolator = new CubicBezierInterpolator();
                        if (Math.abs(value) > TRACKPAD_ACCELERATION_THRESHOLD) value *= STICK_SENSITIVITY;
                        interpolator.set(0.075f, 0.95f, 0.45f, 0.95f);
                        float interpolatedValue = interpolator.getInterpolation(Math.min(1.0f, Math.abs(value / TRACKPAD_MAX_SPEED)));
                        inputControlsView.handleInputEvent(binding, true, Mathf.clamp(interpolatedValue * Mathf.sign(value), -1, 1));
                        this.states[i] = true;
                    }
                    else {
                        if (Math.abs(value) > TouchpadView.CURSOR_ACCELERATION_THRESHOLD) value *= TouchpadView.CURSOR_ACCELERATION;
                        if (binding == Binding.MOUSE_MOVE_LEFT || binding == Binding.MOUSE_MOVE_RIGHT) {
                            cursorDx = Mathf.roundPoint(value);
                        }
                        else if (binding == Binding.MOUSE_MOVE_UP || binding == Binding.MOUSE_MOVE_DOWN) {
                            cursorDy = Mathf.roundPoint(value);
                        }
                        else {
                            inputControlsView.handleInputEvent(binding, states[i], value);
                            this.states[i] = states[i];
                        }
                    }
                }

                if (cursorDx != 0 || cursorDy != 0) inputControlsView.getXServer().injectPointerMoveDelta(cursorDx, cursorDy);
            }
            else {
                final boolean[] states = {deltaY <= -DPAD_DEAD_ZONE, deltaX >= DPAD_DEAD_ZONE, deltaY >= DPAD_DEAD_ZONE, deltaX <= -DPAD_DEAD_ZONE};

                for (byte i = 0; i < 4; i++) {
                    float value = i == 1 || i == 3 ? deltaX : deltaY;
                    Binding binding = getBindingAt(i);
                    boolean state = binding.isMouseMove() ? (states[i] || states[(i+2)%4]) : states[i];
                    inputControlsView.handleInputEvent(binding, state, value);
                    this.states[i] = state;
                }
                inputControlsView.invalidate();
            }

            return true;
        }
        else if (pointerId == currentPointerId && type == Type.RANGE_BUTTON) {
            scroller.handleTouchMove(x, y);
            return true;
        }
        else if (pointerId == currentPointerId && type == Type.SWIPE_PAD) {
            float dx = x - swipePadStartX;
            float dy = y - swipePadStartY;
            float threshold = getBoundingBox().width() * 0.25f;
            // Determine active direction
            boolean[] newStates = new boolean[4];
            if (Math.abs(dx) > threshold || Math.abs(dy) > threshold) {
                if (Math.abs(dx) > Math.abs(dy)) {
                    newStates[dx > 0 ? 1 : 3] = true; // right / left
                } else {
                    newStates[dy > 0 ? 2 : 0] = true; // down / up
                }
            }
            for (byte i = 0; i < 4; i++) {
                if (newStates[i] != states[i]) {
                    states[i] = newStates[i];
                    // only fire if indicator is visible (slow swipe) or immediately (fast)
                    if (newStates[i]) inputControlsView.handleInputEvent(getBindingAt(i), true);
                    else inputControlsView.handleInputEvent(getBindingAt(i), false);
                }
            }
            inputControlsView.invalidate();
            return true;
        }
        else return false;
    }

    public boolean handleTouchUp(int pointerId) {
        return handleTouchUp(pointerId, 0, 0);
    }

    public boolean handleTouchUp(int pointerId, float x, float y) {
        if (pointerId == currentPointerId) {
            if (type == Type.BUTTON) {
                Binding binding = getBindingAt(0);
                states[0] = false;
                inputControlsView.invalidate();
                if (isKeepButtonPressedAfterMinTime() && touchTime != null) {
                    selected = (System.currentTimeMillis() - (long)touchTime) > BUTTON_MIN_TIME_TO_KEEP_PRESSED;
                    if (!selected) inputControlsView.handleInputEvent(binding, false);
                    touchTime = null;
                    inputControlsView.invalidate();
                }
                else if (!toggleSwitch || selected) inputControlsView.handleInputEvent(binding, false);

                if (toggleSwitch) {
                    selected = !selected;
                    inputControlsView.invalidate();
                }
            }
            else if (type == Type.SWIPE_PAD) {
                long elapsed = System.currentTimeMillis() - swipePadTouchStartTime;
                // Release all active directions
                for (byte i = 0; i < 4; i++) {
                    if (states[i]) {
                        inputControlsView.handleInputEvent(getBindingAt(i), false);
                        states[i] = false;
                    }
                }
                swipePadTouchStartTime = -1;
                swipePadAlpha = 0;
                inputControlsView.invalidate();
            }
            else if (type == Type.RADIAL_MENU) {
                if (visible) handleRadialMenuClick(x, y);
                inputControlsView.invalidate();
            }
            else if (type == Type.RANGE_BUTTON || type == Type.D_PAD || type == Type.STICK || type == Type.TRACKPAD) {
                for (byte i = 0; i < states.length; i++) {
                    if (states[i]) inputControlsView.handleInputEvent(getBindingAt(i), false);
                    states[i] = false;
                }

                if (type == Type.RANGE_BUTTON) {
                    scroller.handleTouchUp();
                }
                else if (type == Type.STICK) {
                    inputControlsView.invalidate();
                }

                if (currentPosition != null) currentPosition = null;
            }
            currentPointerId = -1;
            return true;
        }
        return false;
    }

    private void handleRadialMenuClick(float x, float y) {
        int snappingSize = inputControlsView.getSnappingSize();
        float cx = boundingBox.centerX();
        float cy = boundingBox.centerY();
        float innerRadius = boundingBox.width() * 0.5f + snappingSize * 0.5f;
        float outerRadius = boundingBox.width() + snappingSize * scale;
        float startAngle = 0;
        Binding clickedBinding = Binding.NONE;

        for (int i = 0, j = 0; i <= bindings.length; i++) {
            float t = (float)i / bindings.length;
            float endAngle = (float)(t * Math.PI * 2 + Math.PI * 1.5f);

            if (i > 0) {
                float middleAngle = (startAngle + endAngle) * 0.5f;
                float touchAreaCenter = (innerRadius + outerRadius) * 0.5f;
                float touchAreaX = (short)(cx + Math.cos(middleAngle) * touchAreaCenter);
                float touchAreaY = (short)(cy + Math.sin(middleAngle) * touchAreaCenter);

                float lineAx = (float)(cx + Math.cos(startAngle) * touchAreaCenter);
                float lineAy = (float)(cy + Math.sin(startAngle) * touchAreaCenter);
                float lineBx = (float)(cx + Math.cos(endAngle) * touchAreaCenter);
                float lineBy = (float)(cy + Math.sin(endAngle) * touchAreaCenter);

                if (distance(touchAreaX, touchAreaY, x, y) <= distance(lineAx, lineAy, lineBx, lineBy) * 0.5f &&
                    distance(cx, cy, x, y) > innerRadius &&
                    distance(cx, cy, x, y) <= outerRadius) {
                    clickedBinding = bindings[j];
                    break;
                }
                j++;
            }

            startAngle = endAngle;
        }

        if (clickedBinding != Binding.NONE) {
            visible = false;
            final Binding finalBinding = clickedBinding;
            inputControlsView.handleInputEvent(finalBinding, true);
            inputControlsView.postDelayed(() -> inputControlsView.handleInputEvent(finalBinding, false), 30);
        }
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    private int getDarkColor() {
        float overlayOpacity = inputControlsView.getOverlayOpacity();
        float opacity = inputControlsView.isEditMode() ? Math.max(0.15f, overlayOpacity) : overlayOpacity;
        return Color.argb((int)(opacity * 255), 0, 0, 0);
    }

    public PointF getCurrentPosition() {
        if (currentPosition == null) {
            currentPosition = new PointF(x, y); // Initialize to the center (same as outer circle)
        }
        return currentPosition;
    }

    // New setter for current position to allow resetting
    public void setCurrentPosition(float x, float y) {
        if (currentPosition == null) {
            currentPosition = new PointF();
        }
        currentPosition.set(x, y);
        // Optionally invalidate the view to trigger a redraw
        inputControlsView.invalidate();
    }
}
