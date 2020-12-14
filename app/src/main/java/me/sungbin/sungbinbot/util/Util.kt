package me.sungbin.sungbinbot.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

object Util {

    fun getBatteryPercentage(context: Context): Int {
        val intentBattery =
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))!!
        val level = intentBattery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intentBattery.getIntExtra(BatteryManager.EXTRA_SCALE, -1).toFloat()
        val batteryPct = level / scale
        return (batteryPct * 100).toInt()
    }

}