package com.winlator.cmod.core;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.winlator.cmod.R;
import com.winlator.cmod.math.Mathf;
import com.winlator.cmod.widget.InstallMini2048View;

public class DownloadProgressDialog {
    private final Activity activity;
    private Dialog dialog;

    public DownloadProgressDialog(Activity activity) {
        this.activity = activity;
    }

    private void create() {
        if (dialog != null) return;
        dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.download_progress_dialog);

        Window window = dialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    public void show() {
        showImpl(0, null, false);
    }

    public void show(int textResId) {
        showImpl(textResId, null, false);
    }

    /** First-time system image install: progress bar + optional mini 2048. */
    public void show(int textResId, boolean mini2048) {
        showImpl(textResId, null, mini2048);
    }

    public void show(Runnable onCancelCallback) {
        showImpl(0, onCancelCallback, false);
    }

    public void show(int textResId, final Runnable onCancelCallback) {
        showImpl(textResId, onCancelCallback, false);
    }

    private void showImpl(int textResId, Runnable onCancelCallback, boolean mini2048) {
        if (isShowing()) return;
        close();
        if (dialog == null) create();

        if (textResId > 0) ((TextView)dialog.findViewById(R.id.TextView)).setText(textResId);

        setProgress(0);
        if (onCancelCallback != null) {
            dialog.findViewById(R.id.BTCancel).setOnClickListener((v) -> onCancelCallback.run());
            dialog.findViewById(R.id.LLBottomBar).setVisibility(View.VISIBLE);
        } else {
            dialog.findViewById(R.id.LLBottomBar).setVisibility(View.GONE);
        }

        View mini = dialog.findViewById(R.id.LLMini2048);
        if (mini != null) {
            mini.setVisibility(mini2048 ? View.VISIBLE : View.GONE);
            if (mini2048) {
                View game = dialog.findViewById(R.id.Mini2048);
                if (game instanceof InstallMini2048View) {
                    ((InstallMini2048View) game).newGame();
                }
            }
        }

        dialog.show();
    }

    public void setProgress(int progress) {
        if (dialog == null) return;
        progress = Mathf.clamp(progress, 0, 100);
        ((CircularProgressIndicator)dialog.findViewById(R.id.CircularProgressIndicator)).setProgress(progress);
        ((TextView)dialog.findViewById(R.id.TVProgress)).setText(progress+"%");
    }

    public void setStatusText(int textResId) {
        if (dialog == null || textResId <= 0) return;
        ((TextView)dialog.findViewById(R.id.TVStatus)).setText(textResId);
    }

    public void setStatusText(String text) {
        if (dialog == null || text == null) return;
        ((TextView)dialog.findViewById(R.id.TVStatus)).setText(text);
    }

    public void close() {
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
}
