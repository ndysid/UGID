package com.universal.performance;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import java.text.DecimalFormat;

public class FPSOverlay {
    private Context context;
    private WindowManager windowManager;
    private View overlayView;
    private TextView fpsText, cpuText, gpuText, ramText, tempText;
    private boolean isShowing = false;
    private DecimalFormat df = new DecimalFormat("#.##");
    
    public FPSOverlay(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        createOverlayView();
    }
    
    private void createOverlayView() {
        LayoutInflater inflater = LayoutInflater.from(context);
        overlayView = inflater.inflate(R.layout.overlay_fps, null);
        
        fpsText = overlayView.findViewById(R.id.fpsText);
        cpuText = overlayView.findViewById(R.id.cpuText);
        gpuText = overlayView.findViewById(R.id.gpuText);
        ramText = overlayView.findViewById(R.id.ramText);
        tempText = overlayView.findViewById(R.id.tempText);
    }
    
    public void showOverlay() {
        if (isShowing) return;
        
        int layoutFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                         WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                         WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                         WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN;
        }
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            layoutFlags,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 100;
        
        windowManager.addView(overlayView, params);
        isShowing = true;
    }
    
    public void hideOverlay() {
        if (isShowing && overlayView != null) {
            windowManager.removeView(overlayView);
            isShowing = false;
        }
    }
    
    public void updateStats(float fps, float cpu, float gpu, float ram, 
                           float cpuTemp, float gpuTemp, float batteryTemp) {
        if (!isShowing) return;
        
        // Color coding based on performance
        String fpsColor = getFPSColor(fps);
        String cpuColor = getUsageColor(cpu);
        String gpuColor = getUsageColor(gpu);
        String ramColor = getUsageColor(ram);
        String tempColor = getTemperatureColor(cpuTemp);
        
        fpsText.setText("FPS: " + df.format(fps) + "fps");
        fpsText.setTextColor(Color.parseColor(fpsColor));
        
        cpuText.setText("CPU: " + df.format(cpu) + "%");
        cpuText.setTextColor(Color.parseColor(cpuColor));
        
        gpuText.setText("GPU: " + df.format(gpu) + "%");
        gpuText.setTextColor(Color.parseColor(gpuColor));
        
        ramText.setText("RAM: " + df.format(ram) + "%");
        ramText.setTextColor(Color.parseColor(ramColor));
        
        tempText.setText("🌡️ " + df.format(cpuTemp) + "°C");
        tempText.setTextColor(Color.parseColor(tempColor));
    }
    
    private String getFPSColor(float fps) {
        if (fps >= 60) return "#00FF00"; // Green
        if (fps >= 30) return "#FFFF00"; // Yellow
        return "#FF0000"; // Red
    }
    
    private String getUsageColor(float usage) {
        if (usage < 50) return "#00FF00";
        if (usage < 80) return "#FFFF00";
        return "#FF0000";
    }
    
    private String getTemperatureColor(float temp) {
        if (temp < 40) return "#00FF00";
        if (temp < 55) return "#FFFF00";
        if (temp < 65) return "#FFA500";
        return "#FF0000";
    }
    
    public void reduceOverlayLoad() {
        // Reduce update frequency or hide non-critical stats
        // Implementation can be added here
    }
}
