package me.sungbin.sungbinbot

import android.app.Application
import android.os.StrictMode
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.sungbin.androidutils.util.NotificationUtil
import me.sungbin.gamepack.library.game.wordchain.Word


/**
 * Created by SungBin on 2020-12-08.
 */

class SungBinBot : Application() {

    override fun onCreate() {
        super.onCreate()
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build() // 이거 하면 안되는데...
        StrictMode.setThreadPolicy(policy)
        Word.init(applicationContext)
        Firebase.remoteConfig.fetchAndActivate()
        NotificationUtil.createChannel(
            applicationContext,
            getString(R.string.app_name),
            getString(R.string.main_bot_running)
        )
    }

}