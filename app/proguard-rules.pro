# Keep performance monitoring classes
-keep class com.universal.performance.** { *; }

# Keep reflection methods
-keepclassmembers class * {
    public static void set(java.lang.String, java.lang.String);
}

# Keep system properties
-keep class android.os.SystemProperties { *; }

# Keep SurfaceControl
-keep class android.view.SurfaceControl { *; }

# Keep DisplayManager
-keep class android.hardware.display.DisplayManager { *; }

# Keep WindowManager
-keep class android.view.WindowManager { *; }

# Keep PowerManager
-keep class android.os.PowerManager { *; }

# Keep ActivityManager
-keep class android.app.ActivityManager { *; }

# Keep battery classes
-keep class android.os.BatteryManager { *; }

# Keep GPU info classes
-keep class android.graphics.SurfaceControl { *; }

# Keep notification classes
-keep class android.app.Notification { *; }
-keep class android.app.NotificationManager { *; }

# Keep sensor classes
-keep class android.hardware.Sensor { *; }
-keep class android.hardware.SensorManager { *; }
