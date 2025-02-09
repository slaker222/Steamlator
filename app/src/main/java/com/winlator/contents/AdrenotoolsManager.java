package com.winlator.contents;
import android.content.Context;
import android.util.Log;
import com.winlator.core.EnvVars;
import com.winlator.core.FileUtils;
import com.winlator.core.TarCompressorUtils;
import com.winlator.xenvironment.ImageFs;
import java.io.File;
import org.json.JSONException;
import org.json.JSONObject;

public class AdrenotoolsManager {
    
    private File adrenotoolsContentDir;
    private Context mContext;
    
    public AdrenotoolsManager(Context context) {
        this.mContext = context;
        this.adrenotoolsContentDir = new File(mContext.getFilesDir(), "imagefs/contents/adrenotools");
        
    }
        
    public String getLibraryName(Context ctx, String adrenoToolsDriverId) {
        String libraryName = "";
        File driverPath = new File(adrenotoolsContentDir, adrenoToolsDriverId);
        try {
            File metaProfile = new File(driverPath, "meta.json");
            JSONObject jsonObject = new JSONObject(FileUtils.readString(metaProfile));
            libraryName = jsonObject.getString("libraryName");
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return libraryName;
    }
        
    public void extractDriver(Context ctx, String adrenotoolsDriverId) {
        File dst = new File(adrenotoolsContentDir, adrenotoolsDriverId);
        String src = "graphics_driver/" + adrenotoolsDriverId + ".tzst";
        if (!dst.exists()) {
            dst.mkdirs();
            Log.d("AdrenotoolsManager", "Extracting " + src + " to " + dst.getAbsolutePath());
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, mContext, src, dst);
        }    
    }
    
    public void setDriverById(EnvVars envVars, ImageFs imagefs, String adrenotoolsDriverId) {
        extractDriver(mContext, adrenotoolsDriverId);
        String driverPath = adrenotoolsContentDir.getAbsolutePath() + "/" + adrenotoolsDriverId + "/";
        envVars.put("ADRENOTOOLS_DRIVER_PATH", driverPath);
        envVars.put("ADRENOTOOLS_HOOKS_PATH", imagefs.getLibDir());
        envVars.put("ADRENOTOOLS_DRIVER_NAME", getLibraryName(mContext, adrenotoolsDriverId));
    }
 }
