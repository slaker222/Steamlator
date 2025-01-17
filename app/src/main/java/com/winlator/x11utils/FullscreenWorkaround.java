package com.winlator.x11utils;

import android.util.Log;
import com.winlator.R;
import android.view.ViewGroup;
import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.graphics.Rect;
import android.widget.FrameLayout;
import android.view.View;
import android.app.Activity;
import androidx.drawerlayout.widget.DrawerLayout;

@SuppressWarnings("deprecation")
public class FullscreenWorkaround {
    // For more information, see https://issuetracker.google.com/issues/36911528
    // To use this class, simply invoke assistActivity() on an Activity that already has its content view set.

    public static void assistActivity(Activity activity) {
        new FullscreenWorkaround(activity);
    }

    private final Activity mActivity;
    private int usableHeightPrevious;

    private FullscreenWorkaround(Activity activity) {
        mActivity = activity;
        FrameLayout content = activity.findViewById(android.R.id.content);
        content.getViewTreeObserver().addOnGlobalLayoutListener(this::possiblyResizeChildOfContent);
    }

    private void possiblyResizeChildOfContent() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        if (
                !mActivity.hasWindowFocus() ||
                !((mActivity.getWindow().getAttributes().flags & FLAG_FULLSCREEN) == FLAG_FULLSCREEN) ||
                !preferences.getBoolean("Reseed", true) ||
                !preferences.getBoolean("fullscreen", false) ||
                SamsungDexUtils.checkDeXEnabled(mActivity)
        )
            return;
        ViewGroup content = (ViewGroup) ((ViewGroup)mActivity.findViewById(android.R.id.content)).getChildAt(0);
        ViewGroup.LayoutParams viewGroupLayoutParams = content.getLayoutParams();

        int usableHeightNow = computeUsableHeight(content);
        if (usableHeightNow != usableHeightPrevious) {
            int usableHeightSansKeyboard = content.getRootView().getHeight();
            int heightDifference = usableHeightSansKeyboard - usableHeightNow;
            if (heightDifference > (usableHeightSansKeyboard/4)) {
                // keyboard probably just became visible
                viewGroupLayoutParams.height = usableHeightSansKeyboard - heightDifference;
            } else {
                // keyboard probably just became hidden
                viewGroupLayoutParams.height = usableHeightSansKeyboard;
            }
            content.requestLayout();
            usableHeightPrevious = usableHeightNow;
        }
    }

    private int computeUsableHeight(View v) {
        Rect r = new Rect();
        v.getWindowVisibleDisplayFrame(r);
        return (r.bottom - r.top);
    }
}