package com.universal.performance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private TextView statusText;
    private Button startMonitorBtn, unlock120HzBtn, gameOptimizerBtn;
    private PerformanceMonitorService performanceService;
    private RefreshRateUnlocker refreshUnlocker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        checkAndRequestPermissions();
        initializeServices();
        
        setupListeners();
        displayDeviceInfo();
    }

    private void initializeViews() {
        statusText = findViewById(R.id.statusText);
        startMonitorBtn = findViewById(R.id.startMonitorBtn);
        unlock120HzBtn = findViewById(R.id.unlock120HzBtn);
        gameOptimizerBtn = findViewById(R.id.gameOptimizerBtn);
    }

    private void initializeServices() {
        performanceService = new PerformanceMonitorService();
        refreshUnlocker = new RefreshRateUnlocker(this);
    }

    @SuppressLint("SetTextI18n")
    private void displayDeviceInfo() {
        String info = "Device: " + Build.MODEL + "\n" +
                     "Android: " + Build.VERSION.RELEASE + "\n" +
                     "CPU: " + DeviceInfo.getCpuInfo() + "\n" +
                     "GPU: " + DeviceInfo.getGpuInfo() + "\n" +
                     "RAM: " + DeviceInfo.getTotalRam(this) + " GB\n" +
                     "Architecture: " + DeviceInfo.getArchitecture();
        statusText.setText(info);
    }

    private void setupListeners() {
        startMonitorBtn.setOnClickListener(v -> startPerformanceMonitor());
        unlock120HzBtn.setOnClickListener(v -> unlock120Hz());
        gameOptimizerBtn.setOnClickListener(v -> startGameOptimizer());
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.PACKAGE_USAGE_STATS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        };

        List<String> missingPermissions = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission);
                }
            }
        }

        if (!missingPermissions.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String[] missingArray = missingPermissions.toArray(new String[0]);
                ActivityCompat.requestPermissions(this, missingArray, PERMISSION_REQUEST_CODE);
            }
        }

        // Check for overlay permission specifically
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, PERMISSION_REQUEST_CODE + 1);
            }
        }

        // Check for usage stats permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!isUsageStatsPermissionGranted()) {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivity(intent);
            }
        }

        // Check for battery optimization exemption
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            if (!isIgnoringBatteryOptimizations()) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }

    private boolean isUsageStatsPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.app.usage.UsageStatsManager usm = 
                (android.app.usage.UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            java.util.List<android.app.usage.UsageStats> appList = 
                usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, 
                                    time - 1000 * 10, time);
            return appList != null && !appList.isEmpty();
        }
        return true;
    }

    private boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private void startPerformanceMonitor() {
        Intent intent = new Intent(this, PerformanceMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Toast.makeText(this, "Performance Monitor Started", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("SetTextI18n")
    private void unlock120Hz() {
        boolean success = refreshUnlocker.unlock120Hz();
        if (success) {
            Toast.makeText(this, "✅ 120Hz/120FPS Unlocked Successfully!", Toast.LENGTH_LONG).show();
            statusText.setText(statusText.getText() + "\n✅ 120Hz Mode: Active");
            
            // Apply to specific games
            refreshUnlocker.applyGameRefreshRate("com.tencent.tmgp.speedmobile", 120);
            refreshUnlocker.applyGameRefreshRate("com.garena.game.fctw", 120);
        } else {
            Toast.makeText(this, "❌ Could not unlock 120Hz. Check permissions.", Toast.LENGTH_LONG).show();
        }
    }

    private void startGameOptimizer() {
        Intent intent = new Intent(this, GameOptimizerService.class);
        intent.putExtra("package", "com.tencent.tmgp.speedmobile");
        startService(intent);
        Toast.makeText(this, "Game Optimizer Started for QQ飞车", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission denied: " + permissions[i], 
                        Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
                                           }
