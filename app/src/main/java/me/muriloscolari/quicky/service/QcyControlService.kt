package me.muriloscolari.quicky.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.muriloscolari.quicky.MainActivity
import me.muriloscolari.quicky.R
import me.muriloscolari.quicky.ble.QcyBleManager

class QcyControlService : Service() {

    companion object {
        const val CHANNEL_ID = "quicky_control_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "me.muriloscolari.quicky.ACTION_START"
        const val ACTION_CYCLE_ANC = "me.muriloscolari.quicky.ACTION_CYCLE_ANC"
        const val ACTION_TOGGLE_SLEEP = "me.muriloscolari.quicky.ACTION_TOGGLE_SLEEP"
        const val EXTRA_DEVICE_ADDRESS = "extra_device_address"

        fun start(context: Context, deviceAddress: String? = null) {
            QcyBleManager.init(context.applicationContext)
            if (!QcyBleManager.state.value.notificationsEnabled) {
                stop(context)
                QcyBleManager.connectOrAutoScan(deviceAddress)
                return
            }
            val intent = Intent(context, QcyControlService::class.java).apply {
                action = ACTION_START
                if (!deviceAddress.isNullOrBlank()) {
                    putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching { context.startForegroundService(intent) }
            } else {
                runCatching { context.startService(intent) }
            }
        }

        fun stop(context: Context) {
            runCatching {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(NOTIFICATION_ID)
                context.stopService(Intent(context, QcyControlService::class.java))
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        QcyBleManager.init(applicationContext)
        if (!QcyBleManager.state.value.notificationsEnabled) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIFICATION_ID)
            stopSelf()
            return
        }
        createChannel()
        startForegroundCompat(buildNotification())

        serviceScope.launch {
            QcyBleManager.state.collectLatest { st ->
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (!st.notificationsEnabled) {
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } else {
                            @Suppress("DEPRECATION")
                            stopForeground(true)
                        }
                    }
                    nm?.cancel(NOTIFICATION_ID)
                    stopSelf()
                } else {
                    nm?.notify(NOTIFICATION_ID, buildNotification())
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CYCLE_ANC -> {
                QcyBleManager.cycleAncMode()
            }
            ACTION_TOGGLE_SLEEP -> {
                val next = !QcyBleManager.state.value.sleepModeEnabled
                QcyBleManager.setSleepMode(next)
            }
            else -> {
                val addr = intent?.getStringExtra(EXTRA_DEVICE_ADDRESS)
                QcyBleManager.connectOrAutoScan(addr)
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat(notification: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Controle Rápido QCY",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ciclo rápido de ANC e bloqueio automático de toques (Modo Sono)"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val state = QcyBleManager.state.value
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cycleIntent = Intent(this, QcyControlService::class.java).apply {
            action = ACTION_CYCLE_ANC
        }
        val cyclePending = PendingIntent.getService(
            this,
            1,
            cycleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sleepIntent = Intent(this, QcyControlService::class.java).apply {
            action = ACTION_TOGGLE_SLEEP
        }
        val sleepPending = PendingIntent.getService(
            this,
            2,
            sleepIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bat = state.battery
        val batteryText = if (bat.left >= 0 && bat.right >= 0) {
            "L ${bat.left}% · R ${bat.right}%"
        } else {
            state.statusMessage
        }
        val sleepLabel = if (state.sleepModeEnabled) "Toques: Bloqueados" else "Toques: Ativos"

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_headphones)
            .setContentTitle("${state.modelTitle} · ${state.ancCycleMode.label}")
            .setContentText("$batteryText · $sleepLabel")
            .setContentIntent(openPending)
            .setOngoing(state.connected)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Ruído: ${state.ancCycleMode.label} ↻",
                    cyclePending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    if (state.sleepModeEnabled) "Liberar Toques" else "Modo Sono",
                    sleepPending
                ).build()
            )
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
