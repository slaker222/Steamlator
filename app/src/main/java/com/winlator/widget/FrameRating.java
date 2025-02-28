package com.winlator.widget;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.winlator.R;

import com.winlator.core.FileUtils;
import java.io.File;
import java.util.Locale;

public class FrameRating extends FrameLayout implements Runnable {
    private long lastTime = 0;
    private int frameCount = 0;
    private File appInfo = null;
    private float lastFPS = 0;
    private String renderer = null;
    private final TextView tvFPS;
    private final TextView tvRenderer;

    public FrameRating(Context context) {
        this(context, null);
    }

    public FrameRating(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrameRating(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        View view = LayoutInflater.from(context).inflate(R.layout.frame_rating, this, false);
        tvFPS = view.findViewById(R.id.TVFPS);
        tvRenderer = view.findViewById(R.id.TVRenderer);
        appInfo = new File(context.getFilesDir(), "imagefs/tmp/app_info.txt");
        addView(view);
    }
    
    public void reset() {
        Log.d("FrameRating", "Resetting renderer and FPS");
        appInfo.delete();
        renderer = null;
        lastFPS = 0;
    }

    public void update() {
        if (lastTime == 0) lastTime = SystemClock.elapsedRealtime();
        if (appInfo.exists() && renderer == null) renderer = new String(FileUtils.read(appInfo));
        long time = SystemClock.elapsedRealtime();
        if (time >= lastTime + 500) {
            lastFPS = ((float)(frameCount * 1000) / (time - lastTime));
            post(this);
            lastTime = time;
            frameCount = 0;
        }

        frameCount++;
    }

    @Override
    public void run() {
        if (getVisibility() == GONE) setVisibility(View.VISIBLE);
        tvFPS.setText(String.format(Locale.ENGLISH, "%.1f", lastFPS));
        tvRenderer.setText(String.format(Locale.ENGLISH, "%s", renderer));
    }
}