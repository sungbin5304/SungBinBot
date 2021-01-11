package me.sungbin.sungbinbot.bot

import android.app.Notification
import com.sungbin.androidutils.extensions.join
import me.sungbin.gamepack.library.game.chosung.ChosungType
import me.sungbin.sungbinbot.bot.Bot.reply
import me.sungbin.sungbinbot.util.KoreanUtil


/**
 * Created by SungBin on 2021-01-11.
 */

class CsGame : BotWrapper() {

    private var csAnwser = ""
    private var csHintCount = 0
    private var csTurn = 0

    fun hint(action: Notification.Action) {
        if (csAnwser.isNotBlank()) {
            if (csHintCount == csAnwser.length - 1) {
                action.reply("마지막 단어는 혼자서 해봐요!")
            } else {
                action.reply("정답의 ${csHintCount + 1}번째 글자는 ${csAnwser[csHintCount]} 이에요.")
                csHintCount++
            }
        } else {
            action.reply("초성게임이 진행중이지 않아요 :(")
        }
    }

    fun gg(action: Notification.Action) {
        if (csAnwser.isNotBlank()) {
            action.reply("${csTurn}턴 시도 끝에 포기하셨군요!\n초성게임의 정답은 $csAnwser 이였어요.\n\n초성게임이 종료됩니다.")
            clearGame()
        } else {
            action.reply("초성게임이 진행중이지 않아요 :(")
        }
    }

    fun game(action: Notification.Action, message: String) {
        if (csAnwser.isNotBlank()) {
            val input = message.replace("cs", "")
            if (input == csAnwser) {
                action.reply("정답이에요!\n\n${csTurn}턴 만에 정답을 맞추셨어요.")
                clearGame()
            } else {
                csTurn++
                action.reply(
                    "땡! $input ${
                        KoreanUtil.getJongsung(
                            input,
                            "은",
                            "는"
                        )
                    } 정답이 아니에요.\n\n정답 유사도: ${
                        KoreanUtil.checkSameWord(
                            input,
                            csAnwser
                        )
                    }%"
                )
            }
        } else {
            action.reply("초성게임이 진행중이지 않아요 :(")
        }
    }

    fun power(action: Notification.Action, message: String) {
        if (message.length == 5) {
            if (csAnwser.isBlank()) {
                val quiz = me.sungbin.gamepack.library.Game.chosungQuiz()
                val type = quiz[0] as String
                val answer = quiz[1] as String
                val chosung = (quiz[2] as ArrayList<*>).join("")
                val value =
                    "${type}에 대한 초성입니다!\n\n- $chosung\n\n`cs정답`으로 정답을 입력해 주세요!"
                csAnwser = answer
                action.reply(value)
            } else {
                action.reply("이미 게임이 시작되어 있어요.")
            }
        } else {
            val typeList = arrayOf(
                "간식",
                "국내가수",
                "국가",
                "도시",
                "수학",
                "스포츠",
                "브랜드",
                "원소",
                "포켓몬",
                "화학",
                "단어"
            )
            val type = message.split(" ").last()
            if (typeList.contains(type)) {
                if (csAnwser.isBlank()) {
                    val quiz = when (typeList.indexOf(type)) {
                        0 -> me.sungbin.gamepack.library.Game.chosungQuiz(ChosungType.FOOD())
                        1 -> me.sungbin.gamepack.library.Game.chosungQuiz(ChosungType.ARTIST())
                        2 -> me.sungbin.gamepack.library.Game.chosungQuiz(ChosungType.COUNTRY())
                        3 -> me.sungbin.gamepack.library.Game.chosungQuiz(ChosungType.LOCATION())
                        4 -> me.sungbin.gamepack.library.Game.chosungQuiz(ChosungType.MATH())
                        5 -> me.sungbin.gamepack.library.Game.chosungQuiz(ChosungType.SPORT())
                        6 -> me.sungbin.gamepack.library.Game.chosungQuiz(ChosungType.BRAND())
                        7 -> me.sungbin.gamepack.library.Game.chosungQuiz(ChosungType.ELEMENT())
                        8 -> me.sungbin.gamepack.library.Game.chosungQuiz(ChosungType.POCKETMON())
                        9 -> me.sungbin.gamepack.library.Game.chosungQuiz(ChosungType.CHEMISTRY())
                        else -> me.sungbin.gamepack.library.Game.chosungQuiz(ChosungType.WORDS())
                    }
                    val answer = quiz[1] as String
                    val chosung = (quiz[2] as ArrayList<*>).join("")
                    val value =
                        "${quiz[0] as String}에 대한 초성입니다!\n\n- $chosung\n\n`cs정답`으로 정답을 입력해 주세요!"
                    csAnwser = answer
                    action.reply(value)
                } else {
                    action.reply("이미 게임이 시작되어 있어요.")
                }
            } else {
                action.reply("${type}은 존재하지 않는 타입이에요!\n\n[사용 가능 타입]\n${
                    typeList.joinToString("\n")
                }")
            }
        }
    }

    private fun clearGame() {
        csAnwser = ""
        csHintCount = 0
        csTurn = 0
    }

}