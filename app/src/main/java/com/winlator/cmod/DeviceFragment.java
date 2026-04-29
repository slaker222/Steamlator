package com.winlator.cmod;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DeviceFragment extends Fragment {
    private boolean isDarkMode;

    // ── Chip model → marketing name lookup ───────────────────────────────────
    private static final HashMap<String, String> CHIP_NAMES = new HashMap<>();
    static {
        // Qualcomm Snapdragon — Flagship 8xx
        CHIP_NAMES.put("SM8650", "Snapdragon 8 Gen 3");
        CHIP_NAMES.put("SM8550", "Snapdragon 8 Gen 2");
        CHIP_NAMES.put("SM8550-AB", "Snapdragon 8 Gen 2");
        CHIP_NAMES.put("SM8475", "Snapdragon 8+ Gen 1");
        CHIP_NAMES.put("SM8450", "Snapdragon 8 Gen 1");
        CHIP_NAMES.put("SM8350", "Snapdragon 888 5G");
        CHIP_NAMES.put("SM8350-AC", "Snapdragon 888+ 5G");
        CHIP_NAMES.put("SM8250", "Snapdragon 865");
        CHIP_NAMES.put("SM8250-AB", "Snapdragon 865+");
        CHIP_NAMES.put("SM8150", "Snapdragon 855");
        CHIP_NAMES.put("SM8150-AC", "Snapdragon 855+");
        CHIP_NAMES.put("SM8100", "Snapdragon 850");
        CHIP_NAMES.put("MSM8998", "Snapdragon 835");
        CHIP_NAMES.put("MSM8996", "Snapdragon 820/821");
        // Qualcomm Snapdragon — Mid 7xx
        CHIP_NAMES.put("SM7675", "Snapdragon 7s Gen 3");
        CHIP_NAMES.put("SM7550", "Snapdragon 7 Gen 3");
        CHIP_NAMES.put("SM7475", "Snapdragon 7+ Gen 2");
        CHIP_NAMES.put("SM7450", "Snapdragon 7 Gen 1");
        CHIP_NAMES.put("SM7435", "Snapdragon 7s Gen 2");
        CHIP_NAMES.put("SM7325", "Snapdragon 778G 5G");
        CHIP_NAMES.put("SM7325-AE", "Snapdragon 778G+ 5G");
        CHIP_NAMES.put("SM7250", "Snapdragon 765G");
        CHIP_NAMES.put("SM7225", "Snapdragon 750G");
        CHIP_NAMES.put("SM7150", "Snapdragon 730/730G");
        CHIP_NAMES.put("SM7150-AA", "Snapdragon 730");
        CHIP_NAMES.put("SM7150-AB", "Snapdragon 730G");
        CHIP_NAMES.put("SM7125", "Snapdragon 720G");
        // Qualcomm Snapdragon — Mid 6xx
        CHIP_NAMES.put("SM6550", "Snapdragon 6 Gen 1");
        CHIP_NAMES.put("SM6450", "Snapdragon 6s Gen 3");
        CHIP_NAMES.put("SM6375", "Snapdragon 695 5G");
        CHIP_NAMES.put("SM6350", "Snapdragon 690 5G");
        CHIP_NAMES.put("SM6250", "Snapdragon 750");
        CHIP_NAMES.put("SM6225", "Snapdragon 680");
        CHIP_NAMES.put("SM6115", "Snapdragon 662");
        CHIP_NAMES.put("SM6115P", "Snapdragon 662");
        CHIP_NAMES.put("SM6150", "Snapdragon 675");
        CHIP_NAMES.put("SM6125", "Snapdragon 665");
        CHIP_NAMES.put("SDM660", "Snapdragon 660");
        CHIP_NAMES.put("SDM636", "Snapdragon 636");
        CHIP_NAMES.put("SDM630", "Snapdragon 630");
        // Qualcomm Snapdragon — Entry 4xx
        CHIP_NAMES.put("SM4450", "Snapdragon 4 Gen 2");
        CHIP_NAMES.put("SM4375", "Snapdragon 480+ 5G");
        CHIP_NAMES.put("SM4350", "Snapdragon 480 5G");
        CHIP_NAMES.put("SM4250", "Snapdragon 460");
        CHIP_NAMES.put("SM4150", "Snapdragon 439");
        CHIP_NAMES.put("SDM450", "Snapdragon 450");
        CHIP_NAMES.put("SDM429", "Snapdragon 429");
        CHIP_NAMES.put("SDM439", "Snapdragon 439");
        // MediaTek Dimensity
        CHIP_NAMES.put("MT6989", "Dimensity 9300");
        CHIP_NAMES.put("MT6985", "Dimensity 9200+");
        CHIP_NAMES.put("MT6983", "Dimensity 9200");
        CHIP_NAMES.put("MT6982", "Dimensity 9000+");
        CHIP_NAMES.put("MT6983", "Dimensity 9000");
        CHIP_NAMES.put("MT6893", "Dimensity 1200");
        CHIP_NAMES.put("MT6891", "Dimensity 1100");
        CHIP_NAMES.put("MT6885", "Dimensity 1000+");
        CHIP_NAMES.put("MT6889", "Dimensity 1000L");
        CHIP_NAMES.put("MT6873", "Dimensity 800");
        CHIP_NAMES.put("MT6875", "Dimensity 820");
        CHIP_NAMES.put("MT6853", "Dimensity 720");
        CHIP_NAMES.put("MT6833", "Dimensity 700");
        CHIP_NAMES.put("MT6769", "Dimensity 700 (Helio G85)");
        CHIP_NAMES.put("MT6768", "Helio G85");
        CHIP_NAMES.put("MT6765", "Helio G35");
        CHIP_NAMES.put("MT6762", "Helio G25");
        CHIP_NAMES.put("MT6761", "Helio A20");
        CHIP_NAMES.put("MT6771", "Helio P60");
        CHIP_NAMES.put("MT6785", "Helio G90T");
        CHIP_NAMES.put("MT6779", "Helio G80");
        // Samsung Exynos
        CHIP_NAMES.put("exynos2400", "Exynos 2400");
        CHIP_NAMES.put("exynos2200", "Exynos 2200");
        CHIP_NAMES.put("exynos2100", "Exynos 2100");
        CHIP_NAMES.put("exynos990", "Exynos 990");
        CHIP_NAMES.put("exynos980", "Exynos 980");
        CHIP_NAMES.put("exynos9825", "Exynos 9825");
        CHIP_NAMES.put("exynos9820", "Exynos 9820");
        CHIP_NAMES.put("exynos9810", "Exynos 9810");
        CHIP_NAMES.put("exynos7885", "Exynos 7885");
        CHIP_NAMES.put("exynos850", "Exynos 850");
        // Google Tensor
        CHIP_NAMES.put("gs101", "Google Tensor G1");
        CHIP_NAMES.put("gs201", "Google Tensor G2");
        CHIP_NAMES.put("gs301", "Google Tensor G3");
        // Kirin (Huawei)
        CHIP_NAMES.put("kirin9000", "Kirin 9000");
        CHIP_NAMES.put("kirin985", "Kirin 985");
        CHIP_NAMES.put("kirin980", "Kirin 980");
        CHIP_NAMES.put("kirin970", "Kirin 970");
        CHIP_NAMES.put("kirin810", "Kirin 810");
    }

    private String resolveChipName(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        // Exact match first
        String name = CHIP_NAMES.get(raw);
        if (name != null) return name;
        // Case-insensitive match
        String rawUpper = raw.toUpperCase();
        for (Map.Entry<String, String> e : CHIP_NAMES.entrySet()) {
            if (e.getKey().toUpperCase().equals(rawUpper)) return e.getValue();
        }
        // Prefix match (e.g. "SM6375-AC" → SM6375)
        for (Map.Entry<String, String> e : CHIP_NAMES.entrySet()) {
            if (rawUpper.startsWith(e.getKey().toUpperCase())) return e.getValue();
        }
        return raw; // fallback: show raw model code
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.device);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.device_fragment, container, false);
        isDarkMode = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean("dark_mode", true);
        applyTheme(view);
        populateCpuInfo(view);
        populateRamInfo(view);
        populateVulkanInfo(view);
        // GPU and OpenGL require EGL — run on background thread
        new Thread(() -> {
            GlInfo glInfo = queryGlInfo();
            requireActivity().runOnUiThread(() -> {
                populateGpuInfo(view, glInfo);
                populateOpenGLInfo(view, glInfo);
            });
        }).start();
        return view;
    }

    private void applyTheme(View view) {
        view.setBackgroundResource(isDarkMode ? R.color.window_background_color_dark : R.color.window_background_color);
        applyThemeRecursively(view);
    }

    private void applyThemeRecursively(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            if (view instanceof FrameLayout && group.getChildCount() >= 2) {
                View panel = group.getChildAt(0);
                View label = group.getChildAt(1);
                panel.setBackgroundResource(isDarkMode ? R.drawable.bordered_panel_dark : R.drawable.bordered_panel);
                if (label instanceof TextView) {
                    TextView textView = (TextView) label;
                    textView.setBackgroundResource(isDarkMode ? R.color.window_background_color_dark : R.color.window_background_color);
                    textView.setTextColor(isDarkMode ? 0xffcccccc : 0xffbdbdbd);
                }
            }

            for (int i = 0; i < group.getChildCount(); i++) {
                applyThemeRecursively(group.getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTextColor(isDarkMode ? 0xffffffff : 0xff000000);
        }
    }

    // ── CPU ──────────────────────────────────────────────────────────────────

    private void populateCpuInfo(View view) {
        TextView tvCpuName  = view.findViewById(R.id.TVCpuName);
        TextView tvCpuCores = view.findViewById(R.id.TVCpuCores);

        // CPU name: SOC_MODEL (API 31+), else Build.HARDWARE, else /proc/cpuinfo
        String rawModel = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rawModel = Build.SOC_MODEL;
        }
        if (rawModel == null || rawModel.isEmpty() || rawModel.equals(Build.UNKNOWN)) {
            rawModel = readCpuInfoField("Hardware");
        }
        if (rawModel == null || rawModel.isEmpty()) {
            rawModel = Build.HARDWARE;
        }

        // Resolve to marketing name
        String friendlyName = resolveChipName(rawModel != null ? rawModel.trim() : "");
        String displayName;
        if (friendlyName != null && !friendlyName.equals(rawModel)) {
            displayName = friendlyName + " (" + rawModel + ")";
        } else {
            displayName = friendlyName != null ? friendlyName : "—";
        }

        int cores = Runtime.getRuntime().availableProcessors();
        tvCpuName.setText(displayName);
        tvCpuCores.setText(cores + " " + getString(R.string.device_cores));
    }

    private String readCpuInfoField(String field) {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(field)) {
                    int colon = line.indexOf(':');
                    if (colon >= 0) return line.substring(colon + 1).trim();
                }
            }
        } catch (IOException ignored) {}
        return null;
    }

    // ── RAM ──────────────────────────────────────────────────────────────────

    private void populateRamInfo(View view) {
        ActivityManager am = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);

        TextView tvTotal = view.findViewById(R.id.TVRamTotal);
        TextView tvAvail = view.findViewById(R.id.TVRamAvail);
        tvTotal.setText(formatBytes(mi.totalMem));
        tvAvail.setText(formatBytes(mi.availMem));
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "—";
        double gb = bytes / 1_073_741_824.0;
        if (gb >= 1.0) return String.format("%.1f GB", gb);
        double mb = bytes / 1_048_576.0;
        return String.format("%.0f MB", mb);
    }

    // ── GPU + OpenGL (via EGL) ────────────────────────────────────────────────

    private static class GlInfo {
        String renderer = "—";
        String vendor   = "—";
        String glVersion = "—";
        // OpenGL ES version levels actually present
        boolean gles20 = false;
        boolean gles30 = false;
        boolean gles31 = false;
        boolean gles32 = false;
    }

    private GlInfo queryGlInfo() {
        GlInfo info = new GlInfo();
        EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (display == EGL14.EGL_NO_DISPLAY) return info;

        int[] version = new int[2];
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) return info;

        // Choose config for GLES 2.0
        int[] attribs = {
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE,   EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, numConfigs, 0)
                || numConfigs[0] == 0) {
            EGL14.eglTerminate(display);
            return info;
        }

        // Create a tiny pbuffer surface
        int[] pbufAttribs = {EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE};
        EGLSurface surface = EGL14.eglCreatePbufferSurface(display, configs[0], pbufAttribs, 0);
        if (surface == EGL14.EGL_NO_SURFACE) {
            EGL14.eglTerminate(display);
            return info;
        }

        // GLES 2.0 context
        int[] ctxAttribs = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
        EGLContext ctx = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0);
        if (ctx == EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroySurface(display, surface);
            EGL14.eglTerminate(display);
            return info;
        }

        EGL14.eglMakeCurrent(display, surface, surface, ctx);
        info.renderer  = GLES20.glGetString(GLES20.GL_RENDERER);
        info.vendor    = GLES20.glGetString(GLES20.GL_VENDOR);
        info.glVersion = GLES20.glGetString(GLES20.GL_VERSION);
        info.gles20    = (info.renderer != null);

        // Cleanup
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        EGL14.eglDestroyContext(display, ctx);
        EGL14.eglDestroySurface(display, surface);

        // Try GLES 3.0
        info.gles30 = tryCreateGlesContext(display, configs[0], 3);
        // 3.1 / 3.2 via PackageManager features (more reliable than EGL probing)
        PackageManager pm = requireContext().getPackageManager();
        info.gles31 = pm.hasSystemFeature(PackageManager.FEATURE_OPENGLES_EXTENSION_PACK) ||
                      checkGlesVersionFeature(pm, 0x00030001);
        info.gles32 = checkGlesVersionFeature(pm, 0x00030002);

        EGL14.eglTerminate(display);
        if (info.renderer == null) info.renderer = "—";
        if (info.vendor   == null) info.vendor   = "—";
        if (info.glVersion == null) info.glVersion = "—";
        return info;
    }

    private boolean tryCreateGlesContext(EGLDisplay display, EGLConfig config, int ver) {
        int[] ctxAttribs = {EGL14.EGL_CONTEXT_CLIENT_VERSION, ver, EGL14.EGL_NONE};
        EGLContext ctx = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0);
        if (ctx != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(display, ctx);
            return true;
        }
        EGL14.eglGetError(); // clear error
        return false;
    }

    private boolean checkGlesVersionFeature(PackageManager pm, int hexVersion) {
        // Android encodes GLES version in feature name as hex
        return pm.hasSystemFeature("android.hardware.opengles.version", hexVersion);
    }

    private void populateGpuInfo(View view, GlInfo gl) {
        ((TextView) view.findViewById(R.id.TVGpuRenderer)).setText(gl.renderer);
        ((TextView) view.findViewById(R.id.TVGpuVendor)).setText(gl.vendor);
    }

    // ── Vulkan ────────────────────────────────────────────────────────────────

    private void populateVulkanInfo(View view) {
        LinearLayout ll = view.findViewById(R.id.LLVulkanInfo);
        PackageManager pm = requireContext().getPackageManager();

        // Ordered map: label → supported
        Map<String, Boolean> versions = new LinkedHashMap<>();
        versions.put("Vulkan 1.0", pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL, 0));
        versions.put("Vulkan 1.1", pm.hasSystemFeature("android.hardware.vulkan.version", 0x00401000));
        versions.put("Vulkan 1.2", pm.hasSystemFeature("android.hardware.vulkan.version", 0x00402000));
        versions.put("Vulkan 1.3", pm.hasSystemFeature("android.hardware.vulkan.version", 0x00403000));

        boolean anyVulkan = false;
        for (Map.Entry<String, Boolean> entry : versions.entrySet()) {
            addInfoRow(ll, entry.getKey(), entry.getValue());
            if (entry.getValue()) anyVulkan = true;
        }
        if (!anyVulkan) {
            addInfoRow(ll, getString(R.string.device_not_supported), null);
        }
    }

    // ── OpenGL ────────────────────────────────────────────────────────────────

    private void populateOpenGLInfo(View view, GlInfo gl) {
        LinearLayout ll = view.findViewById(R.id.LLOpenGLInfo);

        // First show the actual GL string version
        addInfoRow(ll, "GL_VERSION", gl.glVersion != null ? gl.glVersion : "—", false);

        addInfoRow(ll, "OpenGL ES 2.0", gl.gles20);
        addInfoRow(ll, "OpenGL ES 3.0", gl.gles30);
        addInfoRow(ll, "OpenGL ES 3.1", gl.gles31);
        addInfoRow(ll, "OpenGL ES 3.2", gl.gles32);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Row with ✓/✗ badge */
    private void addInfoRow(LinearLayout parent, String label, Boolean supported) {
        Context ctx = requireContext();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 4, 0, 4);
        row.setLayoutParams(params);

        TextView tvLabel = new TextView(ctx);
        tvLabel.setText(label);
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLabel.setTextColor(isDarkMode ? 0xffffffff : 0xff000000);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f);
        tvLabel.setLayoutParams(lp1);

        TextView tvVal = new TextView(ctx);
        if (supported == null) {
            tvVal.setText("—");
        } else if (supported) {
            tvVal.setText("✓ " + ctx.getString(R.string.device_supported));
            tvVal.setTextColor(0xFF4CAF50);
        } else {
            tvVal.setText("✗ " + ctx.getString(R.string.device_not_supported));
            tvVal.setTextColor(0xFFEF5350);
        }
        if (supported == null) {
            tvVal.setTextColor(isDarkMode ? 0xffffffff : 0xff000000);
        }
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvVal.setLayoutParams(lp2);
        tvVal.setGravity(android.view.Gravity.END);

        row.addView(tvLabel);
        row.addView(tvVal);
        parent.addView(row);
    }

    /** Row with plain text value */
    private void addInfoRow(LinearLayout parent, String label, String value, boolean bold) {
        Context ctx = requireContext();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 4, 0, 4);
        row.setLayoutParams(params);

        TextView tvLabel = new TextView(ctx);
        tvLabel.setText(label);
        if (bold) tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLabel.setTextColor(isDarkMode ? 0xffffffff : 0xff000000);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvLabel.setLayoutParams(lp1);

        TextView tvVal = new TextView(ctx);
        tvVal.setText(value != null ? value : "—");
        tvVal.setTextColor(isDarkMode ? 0xffffffff : 0xff000000);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f);
        tvVal.setLayoutParams(lp2);
        tvVal.setGravity(android.view.Gravity.END);

        row.addView(tvLabel);
        row.addView(tvVal);
        parent.addView(row);
    }
}
