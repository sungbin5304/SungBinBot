package me.sungbin.sungbinbot.bot

import android.app.Notification
import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import me.sungbin.gamepack.library.game.wordchain.Word
import me.sungbin.kakaotalkbotbasemodule.library.KakaoBot
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.*
import android.app.Notification.Action as A

object Bot {

    private lateinit var bot: KakaoBot
    private lateinit var context: Context
    private val winSubApiKey by lazy { Firebase.remoteConfig.getString("winSubApiKey") }
    private val koreanApiKey by lazy { Firebase.remoteConfig.getString("koreanApiKey") }

    fun init(context: Context) {
        this.context = context
        Word.init(context, koreanApiKey)
    }

    val commends = arrayOf(".끝말잇기", ".초성퀴즈 / .초성게임", ".한강", ".가르치기", ".살았니 / .죽었니", ".배터리")
    val commendsDescription = arrayOf("")

    object Game {
        // 초성게임
        private var chosungAnswer = ""
        private var chosungHintCount = 0

        // 끝말잇기
        private var isWordChaining = false
        private var lastWord = ArrayList<String>()


    }

    object Message {
        fun live(a: A) {
            a.reply(
                arrayOf(
                    "죽었다!",
                    "살았다!"
                ).random()
            )
        }
    }

    object Tool {
        fun hangal(a: A) {
            CoroutineScope(Dispatchers.IO).launch {
                val data = async {
                    getHtml("https://api.winsub.kr/hangang/?key=$winSubApiKey")
                }
                val json = JSONObject(data.await())
                val temp = json.getString("temp")
                val time = json.getString("time").split("년 ")[1]
                val value = "$time 기준 현재 한강은 $temp 이에요!"
                a.reply(value)
            }
        }
    }

    private fun jsoupOf(address: String) = Jsoup.connect(address).ignoreContentType(true)
        .ignoreHttpErrors(true)

    private fun getHtml(address: String): String {
        var data = jsoupOf(address).get().toString().trim()
        if (data.contains("<body>")) data =
            data.split("<body>")[1].split("</body>")[0]
        return data
    }

    private fun Notification.Action.reply(message: String) {
        bot.reply(this, message.trim())
    }

}