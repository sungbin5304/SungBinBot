package me.sungbin.sungbinbot.bot

import android.app.Notification
import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.sungbin.androidutils.extensions.join
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import me.sungbin.gamepack.library.game.chosung.ChosungType
import me.sungbin.gamepack.library.game.wordchain.Word
import me.sungbin.kakaotalkbotbasemodule.library.KakaoBot
import me.sungbin.sungbinbot.util.KoreanUtil
import me.sungbin.sungbinbot.util.Util
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
        private var csAnwser = ""
        private var csHintCount = 0
        private var csTurn = 0

        // 끝말잇기
        private var wcIsGaming = false
        private var wcLastWords = ArrayList<String>()
        private var wcTurn = 0

        object Cs {
            fun hint(a: A) {
                if (csAnwser.isNotBlank()) {
                    if (csHintCount == csAnwser.length - 1) {
                        a.reply("마지막 단어는 혼자서 해봐요!")
                    } else {
                        a.reply("정답의 ${csHintCount + 1}번째 글자는 ${csAnwser[csHintCount]} 이에요.")
                        csHintCount++
                    }
                }
            }

            fun gg(a: A) {
                if (csAnwser.isNotBlank()) {
                    a.reply("${csTurn}턴 시도 끝에 포기하셨군요!\n초성게임의 정답은 $csAnwser 이였어요.\n\n초성게임이 종료됩니다.")
                    clearGame()
                }
            }

            fun game(a: A, message: String) {
                if (csAnwser.isNotBlank()) {
                    val input = message.replace("cs", "")
                    if (input == csAnwser) {
                        a.reply("정답이에요!\n\n${csTurn}턴 만에 정답을 맞추셨어요.")
                        clearGame()
                    } else {
                        csTurn++
                        a.reply(
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
                }
            }

            fun power(a: A, message: String) {
                if (message.length == 5) {
                    if (csAnwser.isBlank()) {
                        val quiz = me.sungbin.gamepack.library.Game.chosungQuiz()
                        val type = quiz[0] as String
                        val answer = quiz[1] as String
                        val chosung = (quiz[2] as ArrayList<*>).join("")
                        val value =
                            "${type}에 대한 초성입니다!\n\n- $chosung\n\n`cs정답`으로 정답을 입력해 주세요!"
                        csAnwser = answer
                        a.reply(value)
                    } else {
                        a.reply("이미 게임이 시작되어 있어요.")
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
                            a.reply(value)
                        } else {
                            a.reply("이미 게임이 시작되어 있어요.")
                        }
                    } else {
                        a.reply("${type}은 존재하지 않는 타입이에요!\n\n[사용 가능 타입]\n${
                            typeList.joinToString("\n")
                        }")
                    }
                }
            }

            private fun clearGame() {
                csAnwser = ""
                csHintCount = 0
                wcTurn = 0
            }
        }

        object Wc {
            fun power(a: A) {
                if (wcIsGaming) {
                    a.reply("끝말잇기가 종료되었어요.")
                    wcIsGaming = false
                    wcLastWords.clear()
                    Word.clearUseWord()
                } else {
                    wcIsGaming = true
                    a.reply("끝말잇기가 시작되었어요!\n\n`wc단어`로 게임을 진행해 주세요.")
                }
            }

            fun game(a: A, message: String) {
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
                                    a.reply(
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
                                    a.reply("사용 가능한 단어가 없어요 :(\n${a}턴 만에 제가 졌어요!\n\n끝말잇기가 종료됩니다.")
                                    clearGame()
                                }
                            } else { // 첫 번째 턴 이거나 끝말이 일치하지 않을 때
                                if (wcLastWords.isNotEmpty()) { // 끝말이 일치하지 않을 때
                                    a.reply(
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
                                        a.reply(
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
                                        a.reply("사용 가능한 단어가 없어요 :(\n${a}턴 만에 제가 졌어요!\n\n끝말잇기가 종료됩니다.")
                                        clearGame()
                                    }
                                }
                            }
                        } else {
                            a.reply("헤당 단어($input)는 이미 사용되었어요!\n다른 단어를 입력해 주세요 :)")
                        }
                    } else {
                        a.reply("해당 단어($input)는 없는 단어에요!\n다른 단어를 입력해 주세요 :)")
                    }
                } else {
                    a.reply("사용하실 단어($input)가 너무 짧아요!\n2글자 이상인 단어를 사용해 주세요 :)")
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

        fun version(a: A, version: String) {
            a.reply("즈모봇 버전 $version 가동중...")
        }

        fun battery(a: A) {
            a.reply(
                "현재 저의 수명은 ${
                    Util.getBatteryPercentage(
                        context
                    )
                }% 만큼 남았어요!"
            )
        }
    }

    object Tool {
        fun hangang(a: A) {
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

        fun exception(a: A, exception: Exception) {
            a.reply("봇 작동중 오류가 발생했어요 \uD83D\uDE2D\n\n${exception.localizedMessage}")
            exception.printStackTrace()
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