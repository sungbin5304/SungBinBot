package me.sungbin.sungbinbot.bot

import android.app.Notification
import com.sungbin.androidutils.extensions.join
import me.sungbin.gamepack.library.game.wordchain.Word
import me.sungbin.sungbinbot.bot.Bot.reply
import me.sungbin.sungbinbot.util.KoreanUtil
import java.util.*


/**
 * Created by SungBin on 2021-01-11.
 */

class WcGame : BotWrapper() {

    private var wcIsGaming = false
    private var wcLastWords = ArrayList<String>()
    private var wcTurn = 0

    fun power(action: Notification.Action) {
        if (wcIsGaming) {
            action.reply("끝말잇기가 종료되었어요.")
            wcIsGaming = false
            wcTurn = 0
            wcLastWords.clear()
            Word.clearUseWord()
        } else {
            wcIsGaming = true
            action.reply("끝말잇기가 시작되었어요!\n\n`wc단어`로 게임을 진행해 주세요.")
        }
    }

    fun game(action: Notification.Action, message: String) {
        if (!wcIsGaming) return
        val input = message.replace("wc", "")
        if (message.length >= 4) {
            val firstWord = input.first().toString()
            if (Word.isRealWord(input)) {
                if (!Word.checkIsUsed(input)) {
                    if (wcLastWords.isNotEmpty() && wcLastWords.contains(firstWord)) { // 두 번째 턴이고 끝말이 일치 할 때
                        wcTurn++
                        Word.useWord(input)
                        val replyWord = Word.loadUseableWord(input)
                        if (replyWord != null) { // 사용 가능한 단어가 있을 때
                            val duum = Word.checkDuum(replyWord)
                            wcLastWords = if (duum != null) { // 두음 사용 가능할 때
                                arrayListOf(replyWord.last().toString(), duum)
                            } else {
                                arrayListOf(replyWord.last().toString())
                            }
                            wcTurn++
                            action.reply(
                                "저는 $replyWord${
                                    KoreanUtil.getJongsung(
                                        replyWord,
                                        "을",
                                        "를"
                                    )
                                } 쓸게요!\n\n- ${getWordMean(replyWord)}\n\n${
                                    wcLastWords.join(" 또는 ")
                                }${
                                    KoreanUtil.getJongsung(
                                        wcLastWords.last(),
                                        "으로",
                                        "로"
                                    )
                                } 계속 진행해주세요 :)"
                            )
                        } else { // 사용 가능한 단어가 없을 때
                            action.reply("사용 가능한 단어가 없어요 :(\n${wcTurn}턴 만에 제가 졌어요!\n\n끝말잇기가 종료됩니다.")
                            clearGame()
                        }
                    } else { // 첫 번째 턴 이거나 끝말이 일치하지 않을 때
                        if (wcLastWords.isNotEmpty()) { // 끝말이 일치하지 않을 때
                            action.reply(
                                "${wcLastWords.join(" 또는 ")} ${
                                    KoreanUtil.getJongsung(
                                        wcLastWords.last(),
                                        "으로",
                                        "로"
                                    )
                                } 시작하는 단어를 입력해 주세요!"
                            )
                        } else { // 첫 번째 턴
                            wcTurn++
                            Word.useWord(input)
                            val replyWord = Word.loadUseableWord(input)
                            if (replyWord != null) { // 사용 가능한 단어가 있을 때
                                val duum = Word.checkDuum(replyWord)
                                wcLastWords = if (duum != null) { // 두음 사용 가능함
                                    arrayListOf(replyWord.last().toString(), duum)
                                } else {
                                    arrayListOf(replyWord.last().toString())
                                }
                                wcTurn++
                                action.reply(
                                    "저는 $replyWord${
                                        KoreanUtil.getJongsung(
                                            replyWord,
                                            "을",
                                            "를"
                                        )
                                    } 쓸게요!\n\n- ${getWordMean(replyWord)}\n\n${
                                        wcLastWords.join(" 또는 ")
                                    }${
                                        KoreanUtil.getJongsung(
                                            wcLastWords.last(),
                                            "으로",
                                            "로"
                                        )
                                    } 계속 진행해주세요 :)"
                                )
                            } else { // 사용 가능한 단어가 없을 때
                                action.reply("사용 가능한 단어가 없어요 :(\n${wcTurn}턴 만에 제가 졌어요!\n\n끝말잇기가 종료됩니다.")
                                clearGame()
                            }
                        }
                    }
                } else {
                    action.reply("헤당 단어($input)는 이미 사용되었어요!\n다른 단어를 입력해 주세요 :)")
                }
            } else {
                action.reply("해당 단어($input)는 없는 단어에요!\n다른 단어를 입력해 주세요 :)")
            }
        } else {
            action.reply("사용하실 단어($input)가 너무 짧아요!\n2글자 이상인 단어를 사용해 주세요 :)")
        }
    }

    private fun clearGame() {
        wcIsGaming = false
        wcLastWords.clear()
        wcTurn = 0
        Word.clearUseWord()
    }

    private fun getWordMean(fullword: String) = Word.getWordMean(fullword)
        ?: "[단어 뜻 추출 실패]\nhttps://opendic.korean.go.kr/search/searchResult?focus_name_top=query&query=$fullword"

}