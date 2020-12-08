package me.sungbin.sungbinbot.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import kotlinx.coroutines.flow.map

object DataManager {

    private lateinit var context: Context

    private val logDataStore by lazy { dataStoreOf("bot_log") }
    private val logKey by lazy { prefKeyOf<String>("bot_log_key") }

    private fun dataStoreOf(name: String) = context.createDataStore(name = name)
    private inline fun <reified T : Any> prefKeyOf(key: String) = preferencesKey<T>(key)

    fun init(context: Context) {
        this.context = context
    }

    suspend fun setLog(logString: String) {
        logDataStore.edit { preference ->
            preference[logKey] = logString
        }
    }

    val logFlow by lazy {
        logDataStore.data.map { preference ->
            preference[logKey] ?: ""
        }
    }

}