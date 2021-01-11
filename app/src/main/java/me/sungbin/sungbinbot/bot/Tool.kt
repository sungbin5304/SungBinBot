package me.sungbin.sungbinbot.bot

import android.app.Notification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import me.sungbin.sungbinbot.bot.Bot.getHtml
import me.sungbin.sungbinbot.bot.Bot.reply
import me.sungbin.sungbinbot.bot.Bot.winSubApiKey
import org.json.JSONObject


/**
 * Created by SungBin on 2021-01-11.
 */

class Tool : BotWrapper() {

    fun information(action: Notification.Action) {
        action.reply("현재 저의 단위는 `${super.toString()}` 이에요.")
    }

    fun hangang(action: Notification.Action) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = async {
                getHtml("https://api.winsub.kr/hangang/?key=$winSubApiKey")
            }
            val json = JSONObject(data.await())
            val temp = json.getString("temp")
            val time = json.getString("time").split("년 ")[1]
            val value = "$time 기준 현재 한강은 $temp 이에요!"
            action.reply(value)
        }
    }

}