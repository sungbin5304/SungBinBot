package me.sungbin.sungbinbot

import android.app.Application
import android.os.StrictMode
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import me.sungbin.sungbinbot.datastore.DataManager


/**
 * Created by SungBin on 2020-12-08.
 */

class SungBinBot : Application() {

    override fun onCreate() {
        super.onCreate()
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build() // 이거 하면 안되는데...
        StrictMode.setThreadPolicy(policy)
        Firebase.remoteConfig.fetchAndActivate()
        DataManager.init(applicationContext)
    }

}