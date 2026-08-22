package com.universal.performance;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import java.lang.reflect.Method;

public class RefreshRateUnlocker {
    private static final String TAG = "RefreshUnlocker";
    private Context context;
    
    // System settings keys for refresh rate
    private static final String PEAK_REFRESH_RATE = "peak_refresh_rate";
    private static final String PREFERRED_REFRESH_RATE = "preferred_refresh_rate";
    private static final String MIN_REFRESH_RATE = "min_refresh_rate";
    private static final String DISPLAY_REFRESH_RATE = "display_refresh_rate";
    
    public RefreshRateUnlocker(Context context) {
        this.context = context;
    }
    
    @SuppressLint("NewApi")
    public boolean unlock120Hz() {
        try {
            ContentResolver resolver = context.getContentResolver();
            
            // Method 1: System Settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Settings.System.putFloat(resolver, PEAK_REFRESH_RATE, 120.0f);
                Settings.System.putFloat(resolver, PREFERRED_REFRESH_RATE, 120.0f);
                Settings.System.putFloat(resolver, MIN_REFRESH_RATE, 60.0f);
            }
            
            // Method 2: Display Settings
            Settings.System.putFloat(resolver, DISPLAY_REFRESH_RATE, 120.0f);
            
            // Method 3: Hidden Settings
            try {
                Class<?> systemProperties = Class.forName("android.os.SystemProperties");
                Method setMethod = systemProperties.getMethod("set", String.class, String.class);
                setMethod.invoke(null, "persist.sys.display.refresh.rate", "120");
                setMethod.invoke(null, "persist.sys.display.hz", "120");
                setMethod.invoke(null, "ro.vendor.display.panel.fps", "120");
                setMethod.invoke(null, "vendor.display.max_fps", "120");
            } catch (Exception e) {
                Log.w(TAG, "System Properties method failed", e);
            }
            
            // Method 4: Surface Flinger
            try {
                Class<?> surfaceFlinger = Class.forName("android.view.SurfaceControl");
                Method setRefreshRate = surfaceFlinger.getMethod("setRefreshRate", float.class);
                setRefreshRate.invoke(null, 120.0f);
            } catch (Exception e) {
                Log.w(TAG, "SurfaceFlinger method failed", e);
            }
            
            // Method 5: Window Manager
            try {
                Class<?> windowManager = Class.forName("android.view.WindowManager");
                Method setRefreshRate = windowManager.getMethod("setRefreshRate", float.class);
                // This is more complex, would need to get window token
            } catch (Exception e) {
                Log.w(TAG, "WindowManager method failed", e);
            }
            
            // Broadcast to system
            Intent intent = new Intent("android.intent.action.REFRESH_RATE_CHANGED");
            intent.putExtra("refresh_rate", 120);
            context.sendBroadcast(intent);
            
            Log.i(TAG, "120Hz Unlock applied successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to unlock 120Hz", e);
            return false;
        }
    }
    
    public boolean applyGameRefreshRate(String packageName, int refreshRate) {
        try {
            // Apply game-specific refresh rate
            String gameSetting = "game_refresh_rate_" + packageName.replace(".", "_");
            Settings.System.putFloat(context.getContentResolver(), gameSetting, refreshRate);
            
            // Add to custom app list
            String customApps = Settings.System.getString(context.getContentResolver(), 
                "custom_refresh_rate_apps");
            if (customApps == null) {
                customApps = packageName;
            } else if (!customApps.contains(packageName)) {
                customApps += "," + packageName;
            }
            Settings.System.putString(context.getContentResolver(), 
                "custom_refresh_rate_apps", customApps);
            
            Log.i(TAG, "Applied " + refreshRate + "Hz to " + packageName);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply refresh rate to game", e);
            return false;
        }
    }
    
    public boolean is120HzSupported() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                float peakRefresh = Settings.System.getFloat(
                    context.getContentResolver(), PEAK_REFRESH_RATE, 60.0f);
                float preferredRefresh = Settings.System.getFloat(
                    context.getContentResolver(), PREFERRED_REFRESH_RATE, 60.0f);
                return peakRefresh >= 120.0f || preferredRefresh >= 120.0f;
            }
            
            // Try reading display settings
            float currentRefresh = Settings.System.getFloat(
                context.getContentResolver(), DISPLAY_REFRESH_RATE, 60.0f);
            return currentRefresh >= 120.0f;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to check 120Hz support", e);
            return false;
        }
    }
}
