package me.sungbin.sungbinbot.util

import kotlin.math.floor
import kotlin.math.max

object KoreanUtil {

    // From https://gun0912.tistory.com/65
    fun getJongsung(word: String, firstValue: String, secondValue: String) =
        if ((word.last().toInt() - 0xAC00) % 28 > 0) firstValue else secondValue

    fun checkSameWord(firstValue: String, secondValue: String): Int {
        var comp = HangulParser.disassemble(secondValue)
        var res = HangulParser.disassemble(firstValue)
        var j = 0
        var sim = 0
        val percent = ArrayList<Double>()
        for (rp in 0 until 2) {
            for (i in comp.indices) {
                for (k in j until res.size) {
                    if (k - j >= 2) break
                    if (comp[i] == res[k]) {
                        sim++
                        j = k + 1
                        break
                    }
                }
            }
            percent.add(sim / max(comp.size, res.size).toDouble() * 100)
            val temp = comp
            comp = res
            res = temp
            sim = 0
        }
        return floor(max(percent.first(), percent.last())).toInt()
    }

}