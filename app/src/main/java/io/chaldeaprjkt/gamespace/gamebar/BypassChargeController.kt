package io.chaldeaprjkt.gamespace.gamebar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File

class BypassChargeController(
    private val context: Context
) {
    private val resolver = context.contentResolver
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            applyTargetLogic()
            handler.postDelayed(this, 30_000L)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED,
                Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED -> applyTargetLogic()
            }
        }
    }

    fun isSupported(): Boolean = File(NODE_INPUT_SUSPEND).canWrite()

    fun startIfEnabled() {
        val enabled = Settings.System.getIntForUser(
            resolver, KEY_BYPASS_ENABLED, 0, android.os.UserHandle.USER_CURRENT
        ) == 1
        if (!enabled || !isSupported()) return
        val level = currentBatteryPercent()
        if (level > 0) {
            Settings.System.putIntForUser(
                resolver, KEY_BYPASS_TARGET, level, android.os.UserHandle.USER_CURRENT
            )
        }
        if (!running) {
            running = true
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                    addAction(Intent.ACTION_POWER_CONNECTED)
                    addAction(Intent.ACTION_POWER_DISCONNECTED)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            handler.post(monitorRunnable)
        }
        applyTargetLogic()
    }

    fun stop() {
        if (!running) {
            setInputSuspend(false)
            return
        }
        running = false
        runCatching { context.unregisterReceiver(receiver) }
        handler.removeCallbacks(monitorRunnable)
        setInputSuspend(false)
    }

    private fun applyTargetLogic() {
        if (!isPluggedIn()) {
            setInputSuspend(false)
            return
        }
        val current = currentBatteryPercent()
        val target = Settings.System.getIntForUser(
            resolver, KEY_BYPASS_TARGET, -1, android.os.UserHandle.USER_CURRENT
        )
        if (current < 0 || target < 0) return
        val suspended = readInputSuspend()
        if (current >= target && !suspended) {
            setInputSuspend(true)
        } else if (current < target && suspended) {
            setInputSuspend(false)
        }
    }

    private fun currentBatteryPercent(): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return -1
        return (level * 100f / scale).toInt()
    }

    private fun isPluggedIn(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        return plugged == BatteryManager.BATTERY_PLUGGED_AC ||
            plugged == BatteryManager.BATTERY_PLUGGED_USB ||
            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
    }

    private fun readInputSuspend(): Boolean {
        return runCatching { File(NODE_INPUT_SUSPEND).readText().trim() == "1" }.getOrDefault(false)
    }

    private fun setInputSuspend(enable: Boolean) {
        runCatching { File(NODE_INPUT_SUSPEND).writeText(if (enable) "1" else "0") }
            .onFailure { Log.w(TAG, "Failed to write input_suspend", it) }
    }

    companion object {
        private const val TAG = "BypassChargeController"
        private const val NODE_INPUT_SUSPEND = "/sys/class/qcom-battery/input_suspend"
        private const val KEY_BYPASS_ENABLED = "bypass_charge_enabled"
        private const val KEY_BYPASS_TARGET = "bypass_charge_target_percentage"
    }
}
