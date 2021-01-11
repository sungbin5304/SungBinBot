package me.sungbin.sungbinbot.bot

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import org.jsoup.Jsoup

object Bot {

    lateinit var context: Context
    val winSubApiKey by lazy { Firebase.remoteConfig.getString("winSubApiKey") }
    private val koreanApiKey by lazy { Firebase.remoteConfig.getString("koreanApiKey") }


    val commends = arrayOf(".끝말잇기", ".초성퀴즈 / .초성게임", ".한강", ".가르치기", ".살았니 / .죽었니", ".배터리")
    val commendsDescription = arrayOf("")

    fun init(context: Context) {
        this.context = context
    }

    fun jsoupOf(address: String) = Jsoup.connect(address).ignoreContentType(true)
        .ignoreHttpErrors(true)

    fun getHtml(address: String): String {
        var data = jsoupOf(address).get().toString().trim()
        if (data.contains("<body>")) data =
            data.split("<body>")[1].split("</body>")[0]
        return data
    }

    fun Notification.Action.reply(message: String) {
        try {
            val sendIntent = Intent()
            val msg = Bundle()
            for (inputable in this.remoteInputs) msg.putCharSequence(
                inputable.resultKey,
                message.trim()
            )
            RemoteInput.addResultsToIntent(this.remoteInputs, sendIntent, msg)
            this.actionIntent.send(context, 0, sendIntent)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

}