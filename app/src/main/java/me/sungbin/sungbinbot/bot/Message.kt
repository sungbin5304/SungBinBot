package me.sungbin.sungbinbot.bot

import android.app.Notification
import me.sungbin.sungbinbot.bot.Bot.context
import me.sungbin.sungbinbot.bot.Bot.reply
import me.sungbin.sungbinbot.util.Util


/**
 * Created by SungBin on 2021-01-11.
 */

class Message : BotWrapper() {

    fun live(action: Notification.Action) {
        action.reply(
            arrayOf(
                "죽었다!",
                "살았다!"
            ).random()
        )
    }

    fun version(action: Notification.Action, version: String) {
        action.reply("즈모봇 버전 $version 가동중...")
    }

    fun battery(action: Notification.Action) {
        action.reply(
            "현재 저의 수명은 ${
                Util.getBatteryPercentage(
                    context
                )
            }% 만큼 남았어요!"
        )
    }

}