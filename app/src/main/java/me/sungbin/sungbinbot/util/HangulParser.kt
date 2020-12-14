package me.sungbin.sungbinbot.util

import java.util.*

// From https://github.com/kimkevin/HangulParser - MIT License.
internal object HangulParser {

    private const val FIRST_HANGUL = 44032

    private val CHOSUNG_LIST = charArrayOf(
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    )
    private const val JUNGSUNG_COUNT = 21

    private val JUNGSUNG_LIST = charArrayOf(
        'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
        'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ',
        'ㅣ'
    )
    private const val JONGSUNG_COUNT = 28

    private val JONGSUNG_LIST = charArrayOf(
        ' ', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
        'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    )

    @Throws(Exception::class)
    fun disassemble(hangul: Char): List<String> {
        val jasoList: MutableList<String> = ArrayList()
        val hangulStr = hangul.toString()
        if (hangulStr.matches(".*[가-힣]+.*".toRegex())) {
            val baseCode = hangulStr[0].toInt() - FIRST_HANGUL
            val chosungIndex = baseCode / (JONGSUNG_COUNT * JUNGSUNG_COUNT)
            jasoList.add(CHOSUNG_LIST[chosungIndex].toString())
            val jungsungIndex =
                (baseCode - JONGSUNG_COUNT * JUNGSUNG_COUNT * chosungIndex) / JONGSUNG_COUNT
            jasoList.add(JUNGSUNG_LIST[jungsungIndex].toString())
            val jongsungIndex =
                baseCode - JONGSUNG_COUNT * JUNGSUNG_COUNT * chosungIndex - JONGSUNG_COUNT * jungsungIndex
            if (jongsungIndex > 0) {
                jasoList.add(JONGSUNG_LIST[jongsungIndex].toString())
            }
        } else if (hangulStr.matches(".*[ㄱ-ㅎ]+.*".toRegex())) {
            throw Exception("음절이 아닌 자음입니다")
        } else if (hangulStr.matches(".*[ㅏ-ㅣ]+.*".toRegex())) {
            throw Exception("음절이 아닌 모음입니다")
        } else {
            throw Exception("한글이 아닙니다")
        }
        return jasoList
    }

    @Throws(Exception::class)
    fun disassemble(hangul: String): List<String> {
        val jasoList: MutableList<String> = ArrayList()
        var i = 0
        val li = hangul.length
        while (i < li) {
            try {
                jasoList.addAll(disassemble(hangul[i]))
            } catch (e: Exception) {
                throw Exception((i + 1).toString() + "번째 글자 분리 오류 : " + e.message)
            }
            i++
        }
        return jasoList
    }

    @Throws(Exception::class)
    fun assemble(jasoList: List<String>): String {
        return if (jasoList.isNotEmpty()) {
            var result = ""
            var startIdx = 0
            while (true) {
                if (startIdx < jasoList.size) {
                    val assembleSize = getNextAssembleSize(jasoList, startIdx)
                    result += assemble(jasoList, startIdx, assembleSize)
                    startIdx += assembleSize
                } else {
                    break
                }
            }
            result
        } else {
            throw Exception("자소가 없습니다")
        }
    }

    @Throws(Exception::class)
    private fun assemble(jasoList: List<String>, startIdx: Int, assembleSize: Int): String {
        var unicode = FIRST_HANGUL
        val chosungIndex = String(CHOSUNG_LIST).indexOf(jasoList[startIdx])
        unicode += if (chosungIndex >= 0) {
            JONGSUNG_COUNT * JUNGSUNG_COUNT * chosungIndex
        } else {
            throw Exception((startIdx + 1).toString() + "번째 자소가 한글 초성이 아닙니다")
        }
        val jungsungIndex = String(JUNGSUNG_LIST).indexOf(jasoList[startIdx + 1])
        unicode += if (jungsungIndex >= 0) {
            JONGSUNG_COUNT * jungsungIndex
        } else {
            throw Exception((startIdx + 2).toString() + "번째 자소가 한글 중성이 아닙니다")
        }
        if (assembleSize > 2) {
            val jongsungIndex = String(JONGSUNG_LIST).indexOf(jasoList[startIdx + 2])
            unicode += if (jongsungIndex >= 0) {
                jongsungIndex
            } else {
                throw Exception((startIdx + 3).toString() + "번째 자소가 한글 종성이 아닙니다")
            }
        }
        return unicode.toChar().toString()
    }

    @Throws(Exception::class)
    private fun getNextAssembleSize(jasoList: List<String>, startIdx: Int): Int {
        val remainJasoLength = jasoList.size - startIdx
        val assembleSize: Int
        assembleSize = if (remainJasoLength > 3) {
            if (String(JUNGSUNG_LIST).contains(jasoList[startIdx + 3])) {
                2
            } else {
                3
            }
        } else if (remainJasoLength == 3 || remainJasoLength == 2) {
            remainJasoLength
        } else {
            throw Exception("한글을 구성할 자소가 부족하거나 한글이 아닌 문자가 있습니다")
        }
        return assembleSize
    }
}