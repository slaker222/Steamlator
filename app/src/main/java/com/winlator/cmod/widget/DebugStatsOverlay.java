package com.winlator.cmod.widget;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.winlator.cmod.R;
import com.winlator.cmod.core.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

public class DebugStatsOverlay extends FrameLayout implements Runnable {
    private static final long UPDATE_INTERVAL_MS = 1000L;
    private final ActivityManager activityManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            post(DebugStatsOverlay.this);
            handler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };
    private final TextView tvDebugStats;
    private long lastCpuTotal;
    private long lastCpuIdle;

    public DebugStatsOverlay(@NonNull Context context) {
        super(context);
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        View view = LayoutInflater.from(context).inflate(R.layout.debug_stats_overlay, this, false);
        tvDebugStats = view.findViewById(R.id.TVDebugStats);
        addView(view);
    }

    public void startMonitoring() {
        handler.removeCallbacks(updateRunnable);
        handler.post(updateRunnable);
    }

    public void stopMonitoring() {
        handler.removeCallbacks(updateRunnable);
    }

    public void reset() {
        lastCpuTotal = 0;
        lastCpuIdle = 0;
    }

    @Override
    public void run() {
        if (getVisibility() == GONE) setVisibility(View.VISIBLE);

        StringBuilder stats = new StringBuilder();
        stats.append(getContext().getString(R.string.memory)).append(": ")
                .append(getUsedRam());
        stats.append('\n').append(getContext().getString(R.string.cpu)).append(": ")
                .append(formatCpuUsage());
        stats.append('\n').append(getContext().getString(R.string.temperature)).append(": ")
                .append(formatTemperature());
        tvDebugStats.setText(stats.toString());
    }

    private String getUsedRam() {
        if (activityManager == null) return "--";
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        long usedMem = memoryInfo.totalMem - memoryInfo.availMem;
        return StringUtils.formatBytes(usedMem, false) + " / " + StringUtils.formatBytes(memoryInfo.totalMem);
    }

    private String formatCpuUsage() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = reader.readLine();
            if (line == null || !line.startsWith("cpu ")) return "--";

            String[] parts = line.trim().split("\\s+");
            long user = Long.parseLong(parts[1]);
            long nice = Long.parseLong(parts[2]);
            long system = Long.parseLong(parts[3]);
            long idle = Long.parseLong(parts[4]);
            long iowait = parts.length > 5 ? Long.parseLong(parts[5]) : 0;
            long irq = parts.length > 6 ? Long.parseLong(parts[6]) : 0;
            long softirq = parts.length > 7 ? Long.parseLong(parts[7]) : 0;
            long steal = parts.length > 8 ? Long.parseLong(parts[8]) : 0;

            long total = user + nice + system + idle + iowait + irq + softirq + steal;
            long idleTotal = idle + iowait;

            if (lastCpuTotal == 0) {
                lastCpuTotal = total;
                lastCpuIdle = idleTotal;
                return "--";
            }

            long totalDiff = total - lastCpuTotal;
            long idleDiff = idleTotal - lastCpuIdle;
            lastCpuTotal = total;
            lastCpuIdle = idleTotal;

            if (totalDiff <= 0) return "--";
            float cpuUsage = ((float) (totalDiff - idleDiff) / totalDiff) * 100f;
            return String.format(Locale.ENGLISH, "%.0f%%", Math.max(0f, cpuUsage));
        } catch (IOException | NumberFormatException e) {
            return "--";
        }
    }

    private String formatTemperature() {
        Float temperature = readTemperatureCelsius();
        if (temperature == null) return "--";
        return String.format(Locale.ENGLISH, "%.1f°C", temperature);
    }

    private Float readTemperatureCelsius() {
        File thermalDir = new File("/sys/class/thermal");
        File[] files = thermalDir.listFiles((dir, name) -> name.startsWith("thermal_zone"));
        if (files == null) return null;

        Float fallback = null;
        for (File file : files) {
            File tempFile = new File(file, "temp");
            if (!tempFile.isFile()) continue;

            try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
                String line = reader.readLine();
                if (line == null) continue;

                float raw = Float.parseFloat(line.trim());
                float value = raw > 1000 ? raw / 1000f : raw;
                if (value < -20f || value > 150f) continue;
                if (value >= 20f && value <= 120f) return value;
                fallback = value;
            } catch (IOException | NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    @Override
    protected void onDetachedFromWindow() {
        stopMonitoring();
        super.onDetachedFromWindow();
    }
}
