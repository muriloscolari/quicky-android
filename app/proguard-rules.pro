# R8 Maximum Performance & Size Optimization Rules
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'me.muriloscolari.quicky.r8'
-overloadaggressively

# Strip verbose and debug logs in Release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Keep Android components referenced in AndroidManifest.xml
-keep public class me.muriloscolari.quicky.MainActivity
-keep public class me.muriloscolari.quicky.service.QcyControlService
-keep public class me.muriloscolari.quicky.service.QcyAncTileService
-keep public class me.muriloscolari.quicky.service.QcyCompanionService
-keep public class me.muriloscolari.quicky.service.QcyDeviceControlsService
-keep public class me.muriloscolari.quicky.service.QcyBluetoothReceiver
