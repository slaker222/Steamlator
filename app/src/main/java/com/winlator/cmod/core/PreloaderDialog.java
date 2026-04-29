package com.winlator.cmod.core;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

import com.winlator.cmod.R;

public class PreloaderDialog {
    private final Activity activity;
    private Dialog dialog;
    private boolean transparentBackdrop = false;

    public PreloaderDialog(Activity activity) {
        this.activity = activity;
    }

    private void create() {
        if (dialog != null) return;
        dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.preloader_dialog);

        Window window = dialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            if (transparentBackdrop) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setDimAmount(0f);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }

    public synchronized PreloaderDialog setTransparentBackdrop(boolean transparentBackdrop) {
        this.transparentBackdrop = transparentBackdrop;
        return this;
    }

    public synchronized void show(int textResId) {
        if (isShowing()) return;
        close();
        if (dialog == null) create();
        ((TextView)dialog.findViewById(R.id.TextView)).setText(textResId);
        setDetailsVisible(false);
        clearDetails();
        dialog.show();
    }

    public void showOnUiThread(final int textResId) {
        activity.runOnUiThread(() -> show(textResId));
    }

    public synchronized void close() {
        try {
            if (dialog != null) {
                dialog.dismiss();
            }
        }
        catch (Exception e) {}
    }

    public void closeOnUiThread() {
        activity.runOnUiThread(this::close);
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public synchronized void setDetailsVisible(boolean visible) {
        if (dialog == null) return;
        int visibility = visible ? android.view.View.VISIBLE : android.view.View.GONE;
        dialog.findViewById(R.id.TVDetailsTitle).setVisibility(visibility);
        dialog.findViewById(R.id.SVDetails).setVisibility(visibility);
    }

    public synchronized void clearDetails() {
        if (dialog == null) return;
        ((TextView) dialog.findViewById(R.id.TVDetails)).setText("");
    }

    public synchronized void appendDetailLine(String line) {
        if (dialog == null || line == null) return;
        TextView details = dialog.findViewById(R.id.TVDetails);
        ScrollView scrollView = dialog.findViewById(R.id.SVDetails);
        String current = details.getText().toString();
        if (!current.isEmpty()) current += "\n";
        details.setText(current + line);
        if (scrollView != null) scrollView.post(() -> scrollView.fullScroll(android.view.View.FOCUS_DOWN));
    }
}
