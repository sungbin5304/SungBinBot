package me.sungbin.sungbinbot.bot

import android.app.Notification
import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import me.sungbin.gamepack.library.game.wordchain.Word
import me.sungbin.kakaotalkbotbasemodule.library.KakaoBot
import org.jsoup.Jsoup

open class Bot {

    lateinit var context: Context
    private lateinit var bot: KakaoBot
    private val koreanApiKey by lazy { Firebase.remoteConfig.getString("koreanApiKey") }

    val winSubApiKey by lazy { Firebase.remoteConfig.getString("winSubApiKey") }

    val commends = arrayOf(".끝말잇기", ".초성퀴즈 / .초성게임", ".한강", ".가르치기", ".살았니 / .죽었니", ".배터리")
    val commendsDescription = arrayOf("")

    fun init(context: Context) {
        this.context = context
        Word.init(context, koreanApiKey)
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
        bot.reply(this, message.trim())
    }

}