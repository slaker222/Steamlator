package com.winlator.cmod.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.winlator.cmod.R;

public class QuickHudFloatingView extends FrameLayout {
    private static final int BUBBLE_SIZE_DP = 52;
    private static final int MENU_GAP_DP = 8;
    private static final int EDGE_MARGIN_DP = 8;
    private static final int TOP_ANCHORED_PLAIN_MODE_TOP_DP = 18;
    private static final long IDLE_FADE_DELAY_MS = 30000L;
    private static final float IDLE_ALPHA = 0.5f;
    private final PointF startPoint = new PointF();
    private final Handler idleHandler = new Handler(Looper.getMainLooper());
    private Runnable fadeBubbleRunnable;
    private boolean dragging = false;
    private final int touchSlop;
    private final LinearLayout menuLayout;
    private final ImageButton bubbleButton;
    private final ImageButton keyboardButton;
    private final ImageButton inputControlsButton;
    private final ImageButton closeButton;
    private boolean menuVisible = false;
    private boolean centeredExpandedPlainMode = false;
    private QuickHudListener listener;

    public QuickHudFloatingView(Context context) {
        this(context, null);
    }

    public QuickHudFloatingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickHudFloatingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClipToPadding(false);
        setClipChildren(false);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        menuLayout = new LinearLayout(context);
        menuLayout.setOrientation(LinearLayout.HORIZONTAL);
        menuLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        menuLayout.setBackgroundResource(R.drawable.liquid_glass_card);
        int padding = dpToPx(8);
        menuLayout.setPadding(padding, padding, padding, padding);
        menuLayout.setVisibility(View.GONE);

        keyboardButton = createIconButton(R.drawable.icon_keyboard);
        keyboardButton.setContentDescription(context.getString(R.string.keyboard));
        keyboardButton.setOnClickListener(v -> {
            if (!centeredExpandedPlainMode) collapseMenu();
            if (listener != null) listener.onKeyboard();
        });

        inputControlsButton = createIconButton(R.drawable.icon_input_controls);
        inputControlsButton.setContentDescription(context.getString(R.string.input_controls));
        inputControlsButton.setOnClickListener(v -> {
            if (!centeredExpandedPlainMode) collapseMenu();
            if (listener != null) listener.onInputControls();
        });

        closeButton = createIconButton(R.drawable.icon_exit);
        closeButton.setContentDescription(context.getResources().getString(android.R.string.cancel));
        closeButton.setOnClickListener(v -> {
            if (!centeredExpandedPlainMode) collapseMenu();
            if (listener != null) listener.onClose();
        });

        menuLayout.addView(keyboardButton);
        menuLayout.addView(inputControlsButton);
        menuLayout.addView(closeButton);

        LayoutParams menuParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        addView(menuLayout, menuParams);

        bubbleButton = new ImageButton(context);
        bubbleButton.setBackgroundResource(R.drawable.liquid_glass_circle_button);
        bubbleButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        bubbleButton.setImageDrawable(null);
        bubbleButton.setAdjustViewBounds(true);
        bubbleButton.setClickable(true);
        bubbleButton.setFocusable(true);
        bubbleButton.setLongClickable(false);
        LayoutParams bubbleParams = new LayoutParams(dpToPx(BUBBLE_SIZE_DP), dpToPx(BUBBLE_SIZE_DP));
        bubbleParams.setMargins(0, 0, 0, 0);
        bubbleButton.setLayoutParams(bubbleParams);
        bubbleButton.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        bubbleButton.setOnTouchListener(this::onBubbleTouch);
        addView(bubbleButton);
        fadeBubbleRunnable = () -> bubbleButton.animate().alpha(IDLE_ALPHA).setDuration(200).start();
        resetIdleFadeTimer();
    }

    private ImageButton createIconButton(@DrawableRes int iconRes) {
        ImageButton button = new ImageButton(getContext());
        button.setBackgroundResource(R.drawable.liquid_glass_circle_button);
        button.setImageResource(iconRes);
        button.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        int size = dpToPx(42);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        button.setLayoutParams(params);
        return button;
    }

    private boolean onBubbleTouch(View v, MotionEvent event) {
        if (centeredExpandedPlainMode) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                restoreBubbleOpacity();
                startPoint.set(event.getRawX(), event.getRawY());
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - startPoint.x;
                float dy = event.getRawY() - startPoint.y;
                if (!dragging && Math.hypot(dx, dy) > touchSlop) {
                    dragging = true;
                }
                if (dragging) {
                    movePanel(bubbleButton.getX() + dx, bubbleButton.getY() + dy);
                    startPoint.set(event.getRawX(), event.getRawY());
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!dragging) {
                    toggleMenu();
                }
                dragging = false;
                resetIdleFadeTimer();
                return true;
        }
        return false;
    }

    private void toggleMenu() {
        if (centeredExpandedPlainMode) return;
        if (menuVisible) collapseMenu();
        else expandMenu();
    }

    public void collapseMenu() {
        if (centeredExpandedPlainMode) return;
        menuVisible = false;
        menuLayout.setVisibility(View.GONE);
        resetIdleFadeTimer();
    }

    private void expandMenu() {
        restoreBubbleOpacity();
        menuVisible = true;
        menuLayout.setVisibility(View.VISIBLE);
        menuLayout.post(this::updateMenuPosition);
        resetIdleFadeTimer();
    }

    public void setQuickHudListener(QuickHudListener listener) {
        this.listener = listener;
    }

    public void setBubblePosition(float x, float y) {
        if (centeredExpandedPlainMode) return;
        movePanel(x, y);
        resetIdleFadeTimer();
    }

    private void restoreBubbleOpacity() {
        if (centeredExpandedPlainMode) return;
        bubbleButton.animate().alpha(1.0f).setDuration(120).start();
    }

    private void resetIdleFadeTimer() {
        if (centeredExpandedPlainMode) return;
        idleHandler.removeCallbacks(fadeBubbleRunnable);
        restoreBubbleOpacity();
        if (!menuVisible) {
            idleHandler.postDelayed(fadeBubbleRunnable, IDLE_FADE_DELAY_MS);
        }
    }

    public void enableCenteredExpandedPlainMode() {
        centeredExpandedPlainMode = true;
        idleHandler.removeCallbacks(fadeBubbleRunnable);

        int padding = dpToPx(10);
        menuLayout.setPadding(padding, padding, padding, padding);
        menuLayout.setBackground(createRoundedBackground(Color.WHITE, Color.BLACK, 14, 2));

        applyPlainButtonStyle(keyboardButton);
        applyPlainButtonStyle(inputControlsButton);
        applyPlainButtonStyle(closeButton);

        bubbleButton.setVisibility(View.GONE);
        bubbleButton.setOnTouchListener(null);
        menuVisible = true;
        menuLayout.setVisibility(View.VISIBLE);
        menuLayout.post(this::updateMenuPosition);
    }

    private void applyPlainButtonStyle(ImageButton button) {
        button.setBackground(createRoundedBackground(Color.WHITE, Color.BLACK, 12, 2));
        button.setColorFilter(Color.BLACK);
    }

    private GradientDrawable createRoundedBackground(int fillColor, int strokeColor, int cornerDp, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dpToPx(cornerDp));
        drawable.setStroke(dpToPx(strokeDp), strokeColor);
        return drawable;
    }

    private void movePanel(float x, float y) {
        View parent = (View) getParent();
        if (parent == null) return;

        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        int bubbleSize = dpToPx(BUBBLE_SIZE_DP);
        int margin = dpToPx(EDGE_MARGIN_DP);

        if (x < margin) x = margin;
        if (y < margin) y = margin;
        if (x + bubbleSize > parentWidth - margin) x = parentWidth - bubbleSize - margin;
        if (y + bubbleSize > parentHeight - margin) y = parentHeight - bubbleSize - margin;

        bubbleButton.setX(x);
        bubbleButton.setY(y);
        updateMenuPosition();
    }

    private void updateMenuPosition() {
        View parent = (View) getParent();
        if (parent == null) return;

        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        int margin = dpToPx(EDGE_MARGIN_DP);

        if (centeredExpandedPlainMode) {
            float menuX = (parentWidth - menuLayout.getMeasuredWidth()) * 0.5f;
            float menuY = dpToPx(TOP_ANCHORED_PLAIN_MODE_TOP_DP);
            if (menuX < margin) menuX = margin;
            if (menuY < margin) menuY = margin;
            if (menuLayout.getMeasuredHeight() > 0 && menuY + menuLayout.getMeasuredHeight() > parentHeight - margin) {
                menuY = Math.max(margin, parentHeight - menuLayout.getMeasuredHeight() - margin);
            }
            menuLayout.setX(menuX);
            menuLayout.setY(menuY);
            return;
        }

        int bubbleSize = dpToPx(BUBBLE_SIZE_DP);
        int gap = dpToPx(MENU_GAP_DP);

        float menuX = bubbleButton.getX() + bubbleSize + gap;
        if (menuLayout.getMeasuredWidth() > 0 && menuX + menuLayout.getMeasuredWidth() > parentWidth - margin) {
            menuX = bubbleButton.getX() - menuLayout.getMeasuredWidth() - gap;
        }
        if (menuX < margin) menuX = margin;

        float menuY = bubbleButton.getY() + (bubbleSize - menuLayout.getMeasuredHeight()) / 2f;
        if (menuY < margin) menuY = margin;
        if (menuLayout.getMeasuredHeight() > 0 && menuY + menuLayout.getMeasuredHeight() > parentHeight - margin) {
            menuY = parentHeight - menuLayout.getMeasuredHeight() - margin;
        }

        menuLayout.setX(menuX);
        menuLayout.setY(menuY);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            if (centeredExpandedPlainMode) {
                updateMenuPosition();
                return;
            }
            View parent = (View) getParent();
            if (parent != null) {
                int parentWidth = parent.getWidth();
                int parentHeight = parent.getHeight();
                int bubbleSize = dpToPx(BUBBLE_SIZE_DP);
                int margin = dpToPx(EDGE_MARGIN_DP);

                float x = bubbleButton.getX();
                float y = bubbleButton.getY();
                if (x + bubbleSize > parentWidth - margin) x = parentWidth - bubbleSize - margin;
                if (y + bubbleSize > parentHeight - margin) y = parentHeight - bubbleSize - margin;
                if (x < margin) x = margin;
                if (y < margin) y = margin;
                bubbleButton.setX(x);
                bubbleButton.setY(y);
                updateMenuPosition();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        idleHandler.removeCallbacks(fadeBubbleRunnable);
        super.onDetachedFromWindow();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public interface QuickHudListener {
        void onKeyboard();
        void onInputControls();
        void onPipMode();
        void onClose();
    }
}
