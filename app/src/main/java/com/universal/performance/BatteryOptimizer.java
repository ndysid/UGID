package com.universal.performance;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;
import androidx.core.content.ContextCompat;

public class BatteryOptimizer {
    private static final String TAG = "BatteryOptimizer";
    private Context context;
    private PowerManager powerManager;
    private boolean isPowerSaving = false;
    
    public BatteryOptimizer(Context context) {
        this.context = context;
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }
    
    public BatteryStatus checkBatteryStatus() {
        BatteryStatus status = new BatteryStatus();
        
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
                
                status.level = (int) ((level / (float) scale) * 100);
                status.temperature = temperature / 10.0f;
                status.voltage = voltage;
                status.isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                   status == BatteryManager.BATTERY_STATUS_FULL;
                status.health = health;
                
                // Log battery status
                Log.d(TAG, String.format("Battery: %d%%, Temp: %.1f°C, Voltage: %d, Charging: %b",
                    status.level, status.temperature, status.voltage, status.isCharging));
                
                // Check for battery issues
                if (status.temperature > 45.0f) {
                    Log.w(TAG, "⚠️ Battery overheating: " + status.temperature + "°C");
                    applyBatteryCooling();
                }
                
                if (status.level < 15) {
                    Log.w(TAG, "⚠️ Low battery: " + status.level + "%");
                    applyPowerSaving();
                }
                
                if (status.isCharging && status.temperature > 35.0f) {
                    Log.w(TAG, "⚠️ Charging while hot: " + status.temperature + "°C");
                    suggestStopCharging();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking battery status", e);
        }
        
        return status;
    }
    
    public void applyPowerSaving() {
        if (isPowerSaving) return;
        
        try {
            // Reduce performance
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method setMethod = systemProperties.getMethod("set", 
                String.class, String.class);
            setMethod.invoke(null, "sys.cpu.set.governor", "powersave");
            setMethod.invoke(null, "sys.gpu.set.governor", "powersave");
            
            // Reduce brightness
            android.provider.Settings.System.putInt(
                context.getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                50
            );
            
            // Disable background processes
            android.app.ActivityManager am = (android.app.ActivityManager) 
                context.getSystemService(Context.ACTIVITY_SERVICE);
            am.setProcessLimit(1);
            
            isPowerSaving = true;
            Log.i(TAG, "Power saving mode enabled");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply power saving", e);
        }
    }
    
    public void applyBatteryCooling() {
        try {
            // Reduce CPU frequency
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method setMethod = systemProperties.getMethod("set", 
                String.class, String.class);
            setMethod.invoke(null, "sys.cpu.set.frequency", "1100000");
            setMethod.invoke(null, "sys.gpu.set.frequency", "500000000");
            
            // Lower brightness
            android.provider.Settings.System.putInt(
                context.getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                30
            );
            
            Log.i(TAG, "Battery cooling applied");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply battery cooling", e);
        }
    }
    
    private void suggestStopCharging() {
        Intent intent = new Intent("com.universal.performance.BATTERY_WARNING");
        intent.putExtra("message", "Device is charging and overheating. Please disconnect.");
        context.sendBroadcast(intent);
    }
    
    public boolean isPowerSaving() {
        return isPowerSaving;
    }
    
    public void resetPowerSaving() {
        isPowerSaving = false;
    }
    
    public static class BatteryStatus {
        public int level;
        public float temperature;
        public int voltage;
        public boolean isCharging;
        public int health;
    }
}
