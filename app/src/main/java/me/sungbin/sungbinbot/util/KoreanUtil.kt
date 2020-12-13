package me.sungbin.sungbinbot.util

object KoreanUtil {

    // From https://gun0912.tistory.com/65
    fun getJongsung(word: String, firstValue: String, secondValue: String) =
        if ((word.last().toInt() - 0xAC00) % 28 > 0) firstValue else secondValue

}