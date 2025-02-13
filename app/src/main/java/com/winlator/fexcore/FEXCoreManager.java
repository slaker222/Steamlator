package com.winlator.fexcore;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.winlator.R;

import android.content.Context;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;
import android.widget.ArrayAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.winlator.ShortcutsFragment;
import com.winlator.container.Container;
import com.winlator.core.FileUtils;
import com.winlator.xenvironment.ImageFs;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public final class FEXCoreManager {
    
    private static File configFile;
    private static List<String> tsoPresets;
    private static ImageFs imageFS;
    private static List<String> values;
    private static ArrayList<String> spinnersCurrentSelection = new ArrayList<>();
    
    FEXCoreManager() {
    }
    
    private static String presetFromTSOValues(String tsoEnabled, String vectorTsoEnabled, String memcpysetTSOEnabled, String halfbarrierTSOEnabled, String paranoidTSO) {
        String ret;
        
        if (tsoEnabled.contains("1")) {
            if (vectorTsoEnabled.contains("1")) {
                if (paranoidTSO.contains("1"))
                    ret = "Slowest";
                else
                    ret = "Slow";
            }
            else
                ret = "Fast";
        }
        else
            ret = "Disabled";
        
        return ret;
    }
    
    private static void writeToConfigFile(String tsoPreset, String mblockValue, String X87ReducedPrecisionValue) {
        String tsoEnabled = "0";
        String vectorTSOEnabled = "0" ;
        String memcpysetTSOEnabled = "0";
        String halfbarrierTSOEnabled = "0";
        String paranoidTSO = "0";
        
        switch (tsoPreset) {
            case "Disabled":
                tsoEnabled = "0";
                vectorTSOEnabled = "0";
                memcpysetTSOEnabled = "0";
                halfbarrierTSOEnabled = "0";
                paranoidTSO = "0";
                break;
            case "Fast":
                tsoEnabled = "1";
                vectorTSOEnabled = "0";
                memcpysetTSOEnabled = "0";
                halfbarrierTSOEnabled = "0";
                paranoidTSO = "0";
                break;
            case "Slow":
                tsoEnabled = "1";
                vectorTSOEnabled = "1";
                memcpysetTSOEnabled = "1";
                halfbarrierTSOEnabled = "1";
                paranoidTSO = "0";
                break;
            case "Slowest":
                tsoEnabled = "1";
                vectorTSOEnabled = "1";
                memcpysetTSOEnabled = "1";
                halfbarrierTSOEnabled = "1";
                paranoidTSO = "1";
                break;
        }
        
        try {
            JSONObject config = new JSONObject();
            JSONObject opts = new JSONObject()
                .put("Multiblock", mblockValue)
                .put("TSOEnabled", tsoEnabled)
                .put("VectorTSOEnabled", vectorTSOEnabled)
                .put("MemcpySetTSOEnabled", memcpysetTSOEnabled)
                .put("HalfBarrierTSOEnabled", halfbarrierTSOEnabled)
                .put("X87ReducedPrecision", X87ReducedPrecisionValue)
                .put("ParanoidTSO", paranoidTSO);
            config.put("Config", opts);
            String json = config.toString();
            FileUtils.writeString(configFile, json);
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        } 
    }
    
    private static String readFromConfigFile(String option) {
        String ret = "";
        
        try {
            Gson gson = new Gson();
            HashMap<String, LinkedTreeMap<String, String>> jsonMap = gson.fromJson(new FileReader(configFile), HashMap.class);
            
            LinkedTreeMap<String, String> optionsMap = jsonMap.get("Config");
           
            ret = optionsMap.get(option);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return ret;
        
    }
    
   private static void selectSpinnerItemByValue(Spinner spnr, List<String> values, String value) {
        int position = values.indexOf(value);
        spnr.setSelection(position);
    }
    
    public static void loadFEXCoreSpinners(Context ctx, Container container, Spinner fexcoreTSOSpinner, Spinner fexcoreMultiblockSpinner, Spinner fexcoreX87ReducedPrecisionSpinner) {
        File imageFsRoot = new File(ctx.getFilesDir(), "imagefs");
        imageFS = ImageFs.find(imageFsRoot);
        String tsoPresetValue = "";
        String multiBlockValue = "";
        String X87ReducedPrecisionValue = "";
        ContainerManager containerManager = new ContainerManager(ctx);
        
        tsoPresets = new ArrayList<>(Arrays.asList(ctx.getResources().getStringArray(R.array.fexcore_presets_entries)));
        values = new ArrayList<>(Arrays.asList(ctx.getResources().getStringArray(R.array.TwoInt_values)));
        
        fexcoreTSOSpinner.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, tsoPresets));
        fexcoreMultiblockSpinner.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, values));
        fexcoreX87ReducedPrecisionSpinner.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, values));
        
        if (container != null) 
            configFile = new File(imageFS.home_path + "-" + container.id + "/.fex-emu/Config.json");
        else
            configFile = new File(imageFS.home_path + "-" + containerManager.getNextContainerId() + "/.fex-emu/Config.json");
            
        tsoPresetValue = (configFile != null && configFile.exists()) ? presetFromTSOValues(readFromConfigFile("TSOEnabled"), readFromConfigFile("VectorTSOEnabled"), readFromConfigFile("MemcpySetTsoEnabled"), readFromConfigFile("HalfBarrierTSOEnabled"), readFromConfigFile("ParanoidTSO")) : "Disabled";
        multiBlockValue = (configFile != null && configFile.exists()) ? readFromConfigFile("Multiblock") : "1";
        X87ReducedPrecisionValue = (configFile != null && configFile.exists()) ? readFromConfigFile("X87ReducedPrecision") : "1";
        
        selectSpinnerItemByValue(fexcoreTSOSpinner, tsoPresets, tsoPresetValue);
        selectSpinnerItemByValue(fexcoreMultiblockSpinner, values, multiBlockValue);
        selectSpinnerItemByValue(fexcoreX87ReducedPrecisionSpinner, values, X87ReducedPrecisionValue);
        
        spinnersCurrentSelection.add((String)fexcoreTSOSpinner.getSelectedItem());
        spinnersCurrentSelection.add((String)fexcoreMultiblockSpinner.getSelectedItem());
        spinnersCurrentSelection.add((String)fexcoreX87ReducedPrecisionSpinner.getSelectedItem());
    }
    
    public static void loadFEXCoreSpinners(Context ctx, Shortcut shortcut, Spinner fexcoreTSOSpinner, Spinner fexcoreMultiblockSpinner, Spinner fexcoreX87ReducedPrecisionSpinner) {
        File imageFsRoot = new File(ctx.getFilesDir(), "imagefs");
        imageFS = ImageFs.find(imageFsRoot);
        String tsoPresetValue = "";
        String multiBlockValue = "";
        String X87ReducedPrecisionValue = "";
        
        tsoPresets = new ArrayList<>(Arrays.asList(ctx.getResources().getStringArray(R.array.fexcore_presets_entries)));
        values = new ArrayList<>(Arrays.asList(ctx.getResources().getStringArray(R.array.TwoInt_values)));
        
        fexcoreTSOSpinner.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, tsoPresets));
        fexcoreMultiblockSpinner.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, values));
        fexcoreX87ReducedPrecisionSpinner.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, values));
        
         configFile = new File(imageFS.home_path + "-" + shortcut.container.id + "/.fex-emu/AppConfig/" + shortcut.getExecutable() + ".json");
            
        tsoPresetValue = (configFile != null && configFile.exists()) ? presetFromTSOValues(readFromConfigFile("TSOEnabled"), readFromConfigFile("VectorTSOEnabled"), readFromConfigFile("MemcpySetTsoEnabled"), readFromConfigFile("HalfBarrierTSOEnabled"), readFromConfigFile("ParanoidTSO")) : "Disabled";
        multiBlockValue = (configFile != null && configFile.exists()) ? readFromConfigFile("Multiblock") : "1";
        X87ReducedPrecisionValue = (configFile != null && configFile.exists()) ? readFromConfigFile("X87ReducedPrecision") : "1";
        
        selectSpinnerItemByValue(fexcoreTSOSpinner, tsoPresets, tsoPresetValue);
        selectSpinnerItemByValue(fexcoreMultiblockSpinner, values, multiBlockValue);
        selectSpinnerItemByValue(fexcoreX87ReducedPrecisionSpinner, values, X87ReducedPrecisionValue);
        
        spinnersCurrentSelection.add((String)fexcoreTSOSpinner.getSelectedItem());
        spinnersCurrentSelection.add((String)fexcoreMultiblockSpinner.getSelectedItem());
        spinnersCurrentSelection.add((String)fexcoreX87ReducedPrecisionSpinner.getSelectedItem());
    }
    
    public static void saveFEXCoreSpinners(Container container, Spinner fexcoreTSOSpinner, Spinner fexcoreMultiblockSpinner, Spinner fexcoreX87ReducedPrecisionSpinner) {
        String preset = (String)fexcoreTSOSpinner.getSelectedItem();
        String multiBlockValue = (String)fexcoreMultiblockSpinner.getSelectedItem();
        String x87ReducedPrecisionValue = (String)fexcoreX87ReducedPrecisionSpinner.getSelectedItem();
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            writeToConfigFile(preset, multiBlockValue, x87ReducedPrecisionValue);
        }
        else {
            if (!spinnersCurrentSelection.get(0).equals(preset) || !spinnersCurrentSelection.get(1).equals(multiBlockValue) || !spinnersCurrentSelection.get(2).equals(x87ReducedPrecisionValue)) {
                writeToConfigFile(preset, multiBlockValue,x87ReducedPrecisionValue);
            }
        }
        spinnersCurrentSelection.clear();     
    }
}
