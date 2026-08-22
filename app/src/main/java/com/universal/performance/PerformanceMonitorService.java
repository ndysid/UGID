package com.universal.performance;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

public class PerformanceMonitorService extends Service {
    private static final String TAG = "PerfMonitor";
    private static final String CHANNEL_ID = "PerformanceMonitorChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    private Timer timer;
    private Handler handler;
    private FPSOverlay fpsOverlay;
    private BatteryOptimizer batteryOptimizer;
    
    // Performance metrics
    private float currentFPS = 0;
    private float maxFPS = 120;
    private float minFPS = 0;
    private float avgFPS = 0;
    private int frameCount = 0;
    private long lastFrameTime = 0;
    
    // CPU metrics
    private float cpuUsage = 0;
    private float gpuUsage = 0;
    private int cpuCoreCount = 0;
    private long[] cpuTimes;
    private long[] cpuIdleTimes;
    
    // Memory metrics
    private long totalRAM = 0;
    private long usedRAM = 0;
    private float ramUsage = 0;
    
    // Temperature metrics
    private float cpuTemp = 0;
    private float gpuTemp = 0;
    private float batteryTemp = 0;
    
    private boolean isMonitoring = false;
    private DecimalFormat df = new DecimalFormat("#.##");

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        initializeMetrics();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Initialize components
        fpsOverlay = new FPSOverlay(this);
        batteryOptimizer = new BatteryOptimizer(this);
        
        // Start monitoring
        startMonitoring();
    }

    private void initializeMetrics() {
        cpuCoreCount = Runtime.getRuntime().availableProcessors();
        totalRAM = DeviceInfo.getTotalRam(this);
        cpuTimes = new long[cpuCoreCount];
        cpuIdleTimes = new long[cpuCoreCount];
        
        // Initialize CPU times
        for (int i = 0; i < cpuCoreCount; i++) {
            cpuTimes[i] = 0;
            cpuIdleTimes[i] = 0;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START_MONITORING".equals(action)) {
                startMonitoring();
            } else if ("STOP_MONITORING".equals(action)) {
                stopMonitoring();
            } else if ("SHOW_OVERLAY".equals(action)) {
                fpsOverlay.showOverlay();
            } else if ("HIDE_OVERLAY".equals(action)) {
                fpsOverlay.hideOverlay();
            }
        }
        return START_STICKY;
    }

    private void startMonitoring() {
        if (isMonitoring) return;
        
        isMonitoring = true;
        lastFrameTime = System.currentTimeMillis();
        
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> updateMetrics());
            }
        }, 0, 500); // Update every 500ms
    }

    private void updateMetrics() {
        try {
            updateFPS();
            updateCPUUsage();
            updateGPUUsage();
            updateRAMUsage();
            updateTemperatures();
            updateBatteryInfo();
            
            // Log metrics
            String stats = String.format(
                "FPS: %.1f | CPU: %.1f%% | GPU: %.1f%% | RAM: %.1f%% | Temp: %.1f°C",
                currentFPS, cpuUsage, gpuUsage, ramUsage, cpuTemp
            );
            Log.d(TAG, stats);
            
            // Update FPS overlay
            fpsOverlay.updateStats(
                currentFPS, cpuUsage, gpuUsage, ramUsage, 
                cpuTemp, gpuTemp, batteryTemp
            );
            
            // Apply performance optimizations if needed
            applyPerformanceOptimizations();
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating metrics", e);
        }
    }

    private void updateFPS() {
        long currentTime = System.currentTimeMillis();
        long timeDelta = currentTime - lastFrameTime;
        
        if (timeDelta > 0) {
            currentFPS = 1000.0f / timeDelta;
            if (currentFPS > 120) currentFPS = 120;
            
            if (currentFPS > maxFPS) maxFPS = currentFPS;
            if (currentFPS < minFPS || minFPS == 0) minFPS = currentFPS;
            
            avgFPS = ((avgFPS * frameCount) + currentFPS) / (frameCount + 1);
            frameCount++;
        }
        
        lastFrameTime = currentTime;
        
        // Check for frame drops
        if (currentFPS < 30 && frameCount > 10) {
            Log.w(TAG, "⚠️ Frame drop detected: " + currentFPS + " FPS");
            // Apply anti-frame drop
            applyAntiFrameDrop();
        }
        
        // Check for lag
        if (currentFPS < 15) {
            Log.e(TAG, "🔥 Severe lag detected: " + currentFPS + " FPS");
            applyAntiLag();
        }
    }

    private void updateCPUUsage() {
        try {
            BufferedReader reader = new BufferedReader(
                new FileReader("/proc/stat"));
            String line = reader.readLine();
            reader.close();
            
            if (line != null) {
                String[] parts = line.split("\\s+");
                if (parts.length > 8) {
                    long user = Long.parseLong(parts[1]);
                    long nice = Long.parseLong(parts[2]);
                    long system = Long.parseLong(parts[3]);
                    long idle = Long.parseLong(parts[4]);
                    long iowait = Long.parseLong(parts[5]);
                    long irq = Long.parseLong(parts[6]);
                    long softirq = Long.parseLong(parts[7]);
                    long steal = Long.parseLong(parts[8]);
                    
                    long total = user + nice + system + idle + iowait + irq + softirq + steal;
                    long idleTime = idle + iowait;
                    
                    // Calculate CPU usage
                    cpuUsage = ((float)(total - idleTime) / total) * 100;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading CPU info", e);
        }
    }

    private void updateGPUUsage() {
        try {
            // Try different GPU stats paths
            String[] gpuPaths = {
                "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
                "/sys/class/kgsl/kgsl-3d0/gpu_clock_stats",
                "/sys/kernel/gpu/gpu_busy",
                "/sys/class/misc/mali0/device/utilization"
            };
            
            for (String path : gpuPaths) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(path));
                    String line = reader.readLine();
                    reader.close();
                    if (line != null) {
                        gpuUsage = Float.parseFloat(line.trim().replace("%", ""));
                        return;
                    }
                } catch (Exception ignored) {}
            }
            
            // If no GPU stats, estimate from CPU usage
            gpuUsage = cpuUsage * 0.7f;
        } catch (Exception e) {
            Log.e(TAG, "Error reading GPU info", e);
        }
    }

    private void updateRAMUsage() {
        try {
            BufferedReader reader = new BufferedReader(
                new FileReader("/proc/meminfo"));
            String line;
            long memTotal = 0;
            long memFree = 0;
            long memAvailable = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    memTotal = Long.parseLong(line.split("\\s+")[1]);
                } else if (line.startsWith("MemFree:")) {
                    memFree = Long.parseLong(line.split("\\s+")[1]);
                } else if (line.startsWith("MemAvailable:")) {
                    memAvailable = Long.parseLong(line.split("\\s+")[1]);
                }
            }
            reader.close();
            
            if (memTotal > 0) {
                usedRAM = memTotal - (memAvailable > 0 ? memAvailable : memFree);
                ramUsage = ((float) usedRAM / memTotal) * 100;
                totalRAM = memTotal / 1024; // Convert to MB
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading RAM info", e);
        }
    }

    private void updateTemperatures() {
        try {
            // CPU Temperature
            String[] cpuTempPaths = {
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp",
                "/sys/class/hwmon/hwmon0/temp1_input"
            };
            
            for (String path : cpuTempPaths) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(path));
                    String line = reader.readLine();
                    reader.close();
                    if (line != null) {
                        cpuTemp = Float.parseFloat(line.trim()) / 1000.0f;
                        break;
                    }
                } catch (Exception ignored) {}
            }
            
            // GPU Temperature
            String[] gpuTempPaths = {
                "/sys/class/kgsl/kgsl-3d0/temp",
                "/sys/class/thermal/thermal_zone1/temp"
            };
            
            for (String path : gpuTempPaths) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(path));
                    String line = reader.readLine();
                    reader.close();
                    if (line != null) {
                        gpuTemp = Float.parseFloat(line.trim()) / 1000.0f;
                        break;
                    }
                } catch (Exception ignored) {}
            }
            
            // Battery Temperature
            try {
                BufferedReader reader = new BufferedReader(
                    new FileReader("/sys/class/power_supply/battery/temp"));
                String line = reader.readLine();
                reader.close();
                if (line != null) {
                    batteryTemp = Float.parseFloat(line.trim()) / 10.0f;
                }
            } catch (Exception ignored) {}
            
            // Check for overheating
            if (cpuTemp > 70.0f || gpuTemp > 65.0f || batteryTemp > 45.0f) {
                Log.w(TAG, "🔥 Overheating detected!");
                applyThermalManagement();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading temperatures", e);
        }
    }

    private void updateBatteryInfo() {
        try {
            batteryOptimizer.checkBatteryStatus();
        } catch (Exception e) {
            Log.e(TAG, "Error checking battery", e);
        }
    }

    private void applyPerformanceOptimizations() {
        // Anti-freeze lag
        if (currentFPS < 20) {
            applyAntiFreezeLag();
        }
        
        // Anti-frame rate freezer
        if (currentFPS < 10 && frameCount > 50) {
            applyAntiFrameRateFreezer();
        }
        
        // FPS stability
        if (Math.abs(currentFPS - avgFPS) > 30) {
            applyFPSStability();
        }
        
        // Performance spikes
        if (cpuUsage > 90 || gpuUsage > 85) {
            applyPerformanceSpikeControl();
        }
        
        // Battery drain prevention
        if (batteryTemp > 40.0f) {
            applyBatteryDrainProtection();
        }
    }

    private void applyAntiFrameDrop() {
        // Increase CPU performance
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method setMethod = systemProperties.getMethod("set", 
                String.class, String.class);
            setMethod.invoke(null, "sys.cpu.set.governor", "performance");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set CPU governor", e);
        }
        
        // Reduce rendering load
        fpsOverlay.reduceOverlayLoad();
    }

    private void applyAntiLag() {
        try {
            // Clear memory
            System.gc();
            System.runFinalization();
            
            // Kill background processes
            android.app.ActivityManager am = (android.app.ActivityManager) 
                getSystemService(Context.ACTIVITY_SERVICE);
            java.util.List<android.app.ActivityManager.RunningAppProcessInfo> processes = 
                am.getRunningAppProcesses();
            
            for (android.app.ActivityManager.RunningAppProcessInfo process : processes) {
                if (process.importance > android.app.ActivityManager.RunningAppProcessInfo
                    .IMPORTANCE_FOREGROUND) {
                    android.os.Process.killProcess(process.pid);
                }
            }
            
            // Reset frame counter
            frameCount = 0;
            avgFPS = currentFPS;
            
        } catch (Exception e) {
            Log.e(TAG, "Anti-lag failed", e);
        }
    }

    private void applyAntiFreezeLag() {
        // Increase thread priority
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
        
        // Wake lock
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        android.os.PowerManager.WakeLock wakeLock = pm.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK, "PerformanceMonitor");
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(10000);
        }
    }

    private void applyAntiFrameRateFreezer() {
        // Reset GPU
        try {
            java.lang.reflect.Method resetGPU = Class.forName("android.graphics.SurfaceControl")
                .getMethod("resetGPU");
            resetGPU.invoke(null);
        } catch (Exception ignored) {}
    }

    private void applyFPSStability() {
        // Lock frame rate
        try {
            Class<?> displayManager = Class.forName("android.hardware.display.DisplayManager");
            Method setFrameRate = displayManager.getMethod("setFrameRate", float.class);
            setFrameRate.invoke(null, 60.0f);
        } catch (Exception ignored) {}
    }

    private void applyPerformanceSpikeControl() {
        // Reduce background processes
        android.app.ActivityManager am = (android.app.ActivityManager) 
            getSystemService(Context.ACTIVITY_SERVICE);
        am.setProcessLimit(2);
        am.setMemoryClass(192);
    }

    private void applyThermalManagement() {
        // Reduce performance to cool down
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method setMethod = systemProperties.getMethod("set", 
                String.class, String.class);
            setMethod.invoke(null, "sys.cpu.set.governor", "powersave");
            setMethod.invoke(null, "sys.gpu.set.governor", "powersave");
        } catch (Exception ignored) {}
        
        // Notify user
        Intent intent = new Intent("com.universal.performance.OVERHEATING");
        sendBroadcast(intent);
    }

    private void applyBatteryDrainProtection() {
        batteryOptimizer.applyPowerSaving();
    }

    private void stopMonitoring() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        isMonitoring = false;
        fpsOverlay.hideOverlay();
        stopForeground(true);
        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Performance Monitor",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Performance monitoring service");
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎮 Performance Monitor")
            .setContentText("Monitoring FPS, CPU, GPU, RAM, Temperature")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopMonitoring();
        super.onDestroy();
    }
}
