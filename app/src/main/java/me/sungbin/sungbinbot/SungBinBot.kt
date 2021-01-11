package me.sungbin.sungbinbot

import android.app.Application
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.sungbin.androidutils.util.NotificationUtil
import me.sungbin.sungbinbot.bot.Bot


/**
 * Created by SungBin on 2020-12-08.
 */

class SungBinBot : Application() {

    override fun onCreate() {
        super.onCreate()
        Firebase.remoteConfig.fetchAndActivate()
        Bot.init(applicationContext)
        NotificationUtil.createChannel(
            applicationContext,
            getString(R.string.app_name),
            getString(R.string.main_bot_running)
        )
    }

}