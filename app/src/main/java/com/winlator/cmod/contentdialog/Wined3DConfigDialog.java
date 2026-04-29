package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.winlator.cmod.R;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.GPUInformation;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Wined3DConfigDialog extends ContentDialog {
    public static final String DEFAULT_CONFIG = "gpuDeviceID=1728,offscreenRenderingMode=fbo,videoMemorySize=0";
    private final Context context;
    private final JSONArray gpuCards;

    public Wined3DConfigDialog(View anchor, JSONArray gpuCards) {
        super(anchor.getContext(), R.layout.wined3d_config_dialog);
        context = anchor.getContext();
        this.gpuCards = gpuCards;
        setIcon(R.drawable.icon_settings);
        setTitle("Wined3D " + context.getString(R.string.configuration));

        Spinner sGPUName = findViewById(R.id.SGPUName);
        Spinner sOffscreenRenderingMode = findViewById(R.id.SOffscreenRenderingMode);
        Spinner sVideoMemorySize = findViewById(R.id.SVideoMemorySize);

        KeyValueSet config = parseConfig(anchor.getTag());

        loadGPUNameSpinner(sGPUName, config.get("gpuDeviceID"));
        List<String> offscreenRenderingModeList = Arrays.asList("Backbuffer", "FBO");
        sOffscreenRenderingMode.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, offscreenRenderingModeList));
        AppUtils.setSpinnerSelectionFromValue(sOffscreenRenderingMode, config.get("offscreenRenderingMode"));
        AppUtils.setSpinnerSelectionFromNumber(sVideoMemorySize, config.get("videoMemorySize"));

        setOnConfirmCallback(() -> {
            config.put("gpuDeviceID", String.valueOf(getSelectedGpuDeviceId(sGPUName)));
            config.put("offscreenRenderingMode", sOffscreenRenderingMode.getSelectedItem().toString().toLowerCase());
            String videoMemorySize = StringUtils.parseNumber(sVideoMemorySize.getSelectedItem());
            if ("0".equals(videoMemorySize)) {
                videoMemorySize = String.valueOf(GPUInformation.getMemorySize());
            }
            config.put("videoMemorySize", videoMemorySize);
            anchor.setTag(config.toString());
        });
    }

    public static KeyValueSet parseConfig(Object config) {
        String data = config != null && !config.toString().isEmpty() ? config.toString() : DEFAULT_CONFIG;
        return new KeyValueSet(data);
    }

    private void loadGPUNameSpinner(Spinner spinner, String selectedDeviceIDValue) {
        List<String> values = new ArrayList<>();
        int selectedPosition = 0;
        int selectedDeviceID = StringUtils.parseNumber(selectedDeviceIDValue).isEmpty() ? 1728 : Integer.parseInt(StringUtils.parseNumber(selectedDeviceIDValue));

        try {
            for (int i = 0; i < gpuCards.length(); i++) {
                JSONObject item = gpuCards.getJSONObject(i);
                if (item.getInt("deviceID") == selectedDeviceID) selectedPosition = i;
                values.add(item.getString("name"));
            }
        } catch (JSONException ignored) {
        }

        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, values));
        spinner.setSelection(selectedPosition);
    }

    private int getSelectedGpuDeviceId(Spinner spinner) {
        int position = spinner.getSelectedItemPosition();
        try {
            if (position >= 0 && position < gpuCards.length()) {
                return gpuCards.getJSONObject(position).getInt("deviceID");
            }
        } catch (JSONException ignored) {
        }
        return 1728;
    }
}
