package me.sungbin.sungbinbot.viewmodel

import androidx.lifecycle.ViewModel
import me.sungbin.sungbinbot.bot.BotWrapper


/**
 * Created by SungBin on 2021-01-11.
 */

class BotViewModel : ViewModel() {

    val bots = HashMap<String, Array<BotWrapper>>()

}