package me.sungbin.sungbinbot.ui

import android.Manifest
import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.sungbin.androidutils.extensions.join
import com.sungbin.androidutils.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import me.sungbin.gamepack.library.Game
import me.sungbin.gamepack.library.game.chosung.ChosungType
import me.sungbin.gamepack.library.game.wordchain.Word
import me.sungbin.kakaotalkbotbasemodule.library.KakaoBot
import me.sungbin.sungbinbot.R
import me.sungbin.sungbinbot.databinding.ActivityMainBinding
import me.sungbin.sungbinbot.service.ForgroundService
import me.sungbin.sungbinbot.util.KoreanUtil
import me.sungbin.sungbinbot.util.PathManager
import me.sungbin.sungbinbot.util.Util
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val bot by lazy { KakaoBot() }
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val licenseKey by lazy { Firebase.remoteConfig.getString("licenseKey") }
    private val winSubApiKey by lazy { Firebase.remoteConfig.getString("winSubApiKey") }
    private val koreanApiKey by lazy { Firebase.remoteConfig.getString("koreanApiKey") }

    private val showAll = "\u200b".repeat(500)
    private val timeFormat = SimpleDateFormat("yyMMdd.kkmmss", Locale.KOREA)
    private val version by lazy { timeFormat.format(Date()) }

    // 초성게임
    private var chosungAnswer = ""
    private var chosungHintCount = 0

    // 끝말잇기
    private var isWordChaining = false
    private var lastWord = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        Logger.w(koreanApiKey)
        Word.init(applicationContext, koreanApiKey)
        binding.tvVersion.text = getString(R.string.main_version, version)

        bot.init(applicationContext)
        bot.requestReadNotification()

        BatteryUtil.requestIgnoreBatteryOptimization(applicationContext)
        PermissionUtil.request(
            this,
            getString(R.string.main_request_permission),
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.INTERNET
            )
        )

        if (DataUtil.readData(applicationContext, PathManager.POWER, "false").toBoolean()) {
            bot.setPower(true)
            startService(Intent(this, ForgroundService::class.java))
            binding.swPower.isChecked = true
        }

        binding.swPower.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!DataUtil.readData(applicationContext, PathManager.LICENSE, "false")
                        .toBoolean()
                ) {
                    val etPassword = EditText(this)
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.main_input_password))
                        .setView(etPassword)
                        .setPositiveButton(getString(R.string.ok)) { _, _ ->
                            if (etPassword.text.toString() != licenseKey) {
                                finish()
                                ToastUtil.show(
                                    applicationContext,
                                    getString(R.string.main_wrong_password),
                                    ToastLength.SHORT,
                                    ToastType.WARNING
                                )
                            } else { // 라이선스 확인 완료
                                ToastUtil.show(
                                    applicationContext,
                                    getString(R.string.main_hello),
                                    ToastLength.SHORT,
                                    ToastType.SUCCESS
                                )
                                bot.setPower(true)
                                startService(Intent(this, ForgroundService::class.java))
                                DataUtil.saveData(applicationContext, PathManager.LICENSE, "true")
                                DataUtil.saveData(applicationContext, PathManager.POWER, "true")
                            }
                        }
                        .setCancelable(false)
                        .show()
                } else { // 켜짐
                    bot.setPower(true)
                    startService(Intent(this, ForgroundService::class.java))
                    DataUtil.saveData(applicationContext, PathManager.POWER, "true")
                }
            } else { // 꺼짐
                bot.setPower(false)
                stopService(Intent(this, ForgroundService::class.java))
                DataUtil.saveData(applicationContext, PathManager.POWER, "false")
            }
        }

        // todo: 엑티비티에서 다 처리하면 안되는데!!!!
        bot.setMessageReceiveListener { sender, message, room, isGroupChat, action, profileImage, packageName, bot ->
            try {
                with(message) {
                    when {
                        equals(".즈모봇") -> action.reply("즈모봇 버전 $version 가동중...")
                        equals(".살았니") || equals(".죽었니") -> action.reply(
                            arrayOf(
                                "죽었다!",
                                "살았다!"
                            ).random()
                        )
                        equals(".배터리") -> action.reply(
                            "현재 저의 수명은 ${
                                Util.getBatteryPercentage(
                                    applicationContext
                                )
                            }% 만큼 남았어요!"
                        )
                        equals(".한강") -> {
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
                        equals(".끝말잇기") -> {
                            if (isWordChaining) {
                                action.reply("끝말잇기가 종료되었어요.")
                                isWordChaining = false
                                lastWord.clear()
                                Word.clearUseWord()
                            } else {
                                isWordChaining = true
                                action.reply("끝말잇기가 시작되었어요!\n\n,단어 로 게임을 진행해 주세요.")
                            }
                        }
                        startsWith(",") && isWordChaining && message.length >= 3 -> {
                            val input = message.replace(",", "")
                            val firstWord = input.first().toString()
                            if (Word.isRealWord(input)) {
                                if (!Word.checkIsUsed(input)) {
                                    if (lastWord.isNotEmpty() && lastWord.contains(firstWord)) {
                                        Word.useWord(input)
                                        val replyWord = Word.loadUseableWord(input)
                                        if (replyWord != null) { // 사용 가능한 단어가 있을 때
                                            val duum = Word.checkDuum(replyWord)
                                            lastWord = if (duum != null) { // 두음 사용 가능함
                                                arrayListOf(replyWord.last().toString(), duum)
                                            } else {
                                                arrayListOf(replyWord.last().toString())
                                            }
                                            action.reply(
                                                "저는 $replyWord${
                                                    KoreanUtil.getJongsung(
                                                        replyWord,
                                                        "을",
                                                        "를"
                                                    )
                                                } 쓸게요!\n\n- ${Word.getWordMean(replyWord) ?: "[단어 뜻 추출 실패]\nhttps://opendic.korean.go.kr/search/searchResult?focus_name_top=query&query=$replyWord"}\n\n${
                                                    lastWord.join(
                                                        " 또는 "
                                                    )
                                                }${
                                                    KoreanUtil.getJongsung(
                                                        lastWord.last(),
                                                        "으로",
                                                        "로"
                                                    )
                                                } 계속 진행해주세요 :)"
                                            )
                                        } else { // 사용 가능한 단어가 없을 때
                                            action.reply("사용 가능한 단어가 없어요 :(\n제가 졌어요!\n\n끝말잇기가 종료됩니다.")
                                            isWordChaining = false
                                            lastWord.clear()
                                            Word.clearUseWord()
                                        }
                                    } else {
                                        if (lastWord.isNotEmpty()) {
                                            action.reply(
                                                "${lastWord.join(" 또는 ")} ${
                                                    KoreanUtil.getJongsung(
                                                        lastWord.last(),
                                                        "으로",
                                                        "로"
                                                    )
                                                } 시작하는 단어를 입력해 주세요!"
                                            )
                                        } else {
                                            Word.useWord(input)
                                            val replyWord = Word.loadUseableWord(input)
                                            if (replyWord != null) { // 사용 가능한 단어가 있을 때
                                                val duum = Word.checkDuum(replyWord)
                                                lastWord = if (duum != null) { // 두음 사용 가능함
                                                    arrayListOf(replyWord.last().toString(), duum)
                                                } else {
                                                    arrayListOf(replyWord.last().toString())
                                                }
                                                action.reply(
                                                    "저는 $replyWord${
                                                        KoreanUtil.getJongsung(
                                                            replyWord,
                                                            "을",
                                                            "를"
                                                        )
                                                    } 쓸게요!\n\n- ${Word.getWordMean(replyWord) ?: "[단어 뜻 추출 실패]\nhttps://opendic.korean.go.kr/search/searchResult?focus_name_top=query&query=$replyWord"}\n\n${
                                                        lastWord.join(
                                                            " 또는 "
                                                        )
                                                    }${
                                                        KoreanUtil.getJongsung(
                                                            lastWord.last(),
                                                            "으로",
                                                            "로"
                                                        )
                                                    } 계속 진행해주세요 :)"
                                                )
                                            } else { // 사용 가능한 단어가 없을 때
                                                action.reply("사용 가능한 단어가 없어요 :(\n제가 졌어요!\n\n끝말잇기가 종료됩니다.")
                                                isWordChaining = false
                                                lastWord.clear()
                                                Word.clearUseWord()
                                            }
                                        }
                                    }
                                } else {
                                    action.reply("헤당 단어($input)는 이미 사용되었어요!\n다른 단어를 입력해 주세요 :)")
                                }
                            } else {
                                action.reply("해당 단어($input)는 없는 단어에요!\n다른 단어를 입력해 주세요 :)")
                            }
                        }
                        contains(".초성게임") || contains(".초성퀴즈") -> {
                            if (length == 5) {
                                if (chosungAnswer.isBlank()) {
                                    val quiz = Game.chosungQuiz()
                                    val type = quiz[0] as String
                                    val answer = quiz[1] as String
                                    val chosung = (quiz[2] as ArrayList<*>).join("")
                                    val value =
                                        "$type 에 대한 초성입니다!\n\n- $chosung\n\n.정답 으로 정답을 입력해 주세요!"
                                    chosungAnswer = answer
                                    action.reply(value)
                                } else {
                                    action.reply("이미 게임이 시작되어 있어요.")
                                }
                            } else {
                                val typeList = arrayOf(
                                    "음식",
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
                                val type = split(" ")[1]
                                if (typeList.contains(type)) {
                                    if (chosungAnswer.isBlank()) {
                                        val quiz = when (typeList.indexOf(type)) {
                                            0 -> Game.chosungQuiz(ChosungType.FOOD())
                                            1 -> Game.chosungQuiz(ChosungType.ARTIST())
                                            2 -> Game.chosungQuiz(ChosungType.COUNTRY())
                                            3 -> Game.chosungQuiz(ChosungType.LOCATION())
                                            4 -> Game.chosungQuiz(ChosungType.MATH())
                                            5 -> Game.chosungQuiz(ChosungType.SPORT())
                                            6 -> Game.chosungQuiz(ChosungType.BRAND())
                                            7 -> Game.chosungQuiz(ChosungType.ELEMENT())
                                            8 -> Game.chosungQuiz(ChosungType.POCKETMON())
                                            9 -> Game.chosungQuiz(ChosungType.CHEMISTRY())
                                            else -> Game.chosungQuiz(ChosungType.WORDS())
                                        }
                                        val answer = quiz[1] as String
                                        val chosung = (quiz[2] as ArrayList<*>).join("")
                                        val value =
                                            "${quiz[0] as String} 에 대한 초성입니다!\n\n- $chosung\n\n.정답 으로 정답을 입력해 주세요!"
                                        chosungAnswer = answer
                                        action.reply(value)
                                    } else {
                                        action.reply("이미 게임이 시작되어 있어요.")
                                    }
                                } else {
                                    action.reply("$type 은 존재하지 않는 타입이에요!\n\n[사용 가능 타입]\n${
                                        typeList.joinToString("\n")
                                    }")
                                }
                            }
                        }
                        equals(".초성정답") && chosungAnswer.isNotBlank() -> action.reply("초성게임의 정답은 $chosungAnswer 이였어요!")
                        equals(".초성힌트") && chosungAnswer.isNotBlank() -> {
                            if (chosungHintCount == chosungAnswer.length - 1) {
                                action.reply("마지막 단어는 혼자서 해봐요!")
                            } else {
                                action.reply("정답의 ${chosungHintCount + 1}번째 글자는 ${chosungAnswer[chosungHintCount]} 이에요.")
                                chosungHintCount++
                            }
                        }
                        startsWith(".") && chosungAnswer.isNotBlank() -> {
                            val input = message.replace(".", "")
                            if (input == chosungAnswer) {
                                action.reply("정답이에요!")
                                chosungAnswer = ""
                                chosungHintCount = 0
                            } else {
                                action.reply(
                                    "땡! $input ${
                                        KoreanUtil.getJongsung(
                                            input,
                                            "은",
                                            "는"
                                        )
                                    } 정답이 아니에요.\n\n정답 유사도 : ${
                                        KoreanUtil.checkSameWord(
                                            input,
                                            chosungAnswer
                                        )
                                    }%"
                                )
                            }
                        }
                        else -> Unit
                    }
                }
            } catch (exception: Exception) {
                action.reply("봇 작동중 오류가 발생했어요 \uD83D\uDE2D\n\n$exception")
                exception.printStackTrace()
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