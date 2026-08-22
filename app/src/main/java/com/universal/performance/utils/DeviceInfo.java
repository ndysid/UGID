package com.universal.performance.utils;

import android.content.Context;
import android.os.Build;
import android.app.ActivityManager;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class DeviceInfo {
    
    public static String getCpuInfo() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
            String cpuInfo = "";
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Processor") || line.startsWith("model name")) {
                    cpuInfo = line.substring(line.indexOf(":") + 2);
                    break;
                }
            }
            reader.close();
            return cpuInfo.isEmpty() ? "Unknown CPU" : cpuInfo;
        } catch (IOException e) {
            return "Unknown CPU";
        }
    }
    
    public static String getGpuInfo() {
        try {
            String[] gpuPaths = {
                "/sys/class/kgsl/kgsl-3d0/device_name",
                "/sys/class/misc/mali0/device/name",
                "/sys/devices/platform/gpu/device/name"
            };
            
            for (String path : gpuPaths) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(path));
                    String gpuName = reader.readLine();
                    reader.close();
                    if (gpuName != null && !gpuName.isEmpty()) {
                        return gpuName;
                    }
                } catch (Exception ignored) {}
            }
            
            return "Unknown GPU";
        } catch (Exception e) {
            return "Unknown GPU";
        }
    }
    
    public static long getTotalRam(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.totalMem / (1024 * 1024 * 1024); // Convert to GB
    }
    
    public static String getArchitecture() {
        if (Build.SUPPORTED_ABIS.length > 0) {
            return Build.SUPPORTED_ABIS[0];
        }
        return "Unknown";
    }
    
    public static String getDeviceModel() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }
    
    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
    }
    
    public static boolean is64Bit() {
        return Build.SUPPORTED_64_BIT_ABIS.length > 0;
    }
}
