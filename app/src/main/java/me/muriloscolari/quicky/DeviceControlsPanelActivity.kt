package me.muriloscolari.quicky

/**
 * Embedded Activity displayed inside Android 14+ / HyperOS / OneUI Device Controls
 * via ControlsProviderService.META_DATA_PANEL_ACTIVITY.
 *
 * Protected by android.permission.BIND_CONTROLS in AndroidManifest.xml so only SystemUI
 * can embed it, without restricting launcher access to MainActivity.
 */
class DeviceControlsPanelActivity : MainActivity() {
    override val isEmbeddedPanel: Boolean = true
}
