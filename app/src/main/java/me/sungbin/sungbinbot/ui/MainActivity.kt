package me.sungbin.sungbinbot.ui

import android.Manifest
import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.sungbin.androidutils.util.*
import me.sungbin.gamepack.library.Game
import me.sungbin.kakaotalkbotbasemodule.library.KakaoBot
import me.sungbin.sungbinbot.R
import me.sungbin.sungbinbot.databinding.ActivityMainBinding
import me.sungbin.sungbinbot.service.ForgroundService
import me.sungbin.sungbinbot.util.KoreanUtil
import me.sungbin.sungbinbot.util.PathManager
import me.sungbin.sungbinbot.util.RhinoUtil
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val bot by lazy { KakaoBot() }
    private val password by lazy { Firebase.remoteConfig.getString("password") }
    private val apiKey by lazy { Firebase.remoteConfig.getString("apiKey") }
    private var replyTime = System.currentTimeMillis()
    private val showAll = "\u200b".repeat(500)
    private var chosungAnswer = ""
    private var chosungHintCount = 0
    private val version = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.swPower.isChecked =
            DataUtil.readData(applicationContext, PathManager.POWER, "false").toBoolean()
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
                            if (etPassword.text.toString() != password) {
                                finish()
                                ToastUtil.show(
                                    applicationContext,
                                    getString(R.string.main_wrong_password),
                                    ToastLength.SHORT,
                                    ToastType.WARNING
                                )
                            } else { // 라이선스 확인 완료
                                bot.setPower(true)
                                ToastUtil.show(
                                    applicationContext,
                                    getString(R.string.main_hello),
                                    ToastLength.SHORT,
                                    ToastType.SUCCESS
                                )
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

        bot.setMessageReceiveListener { sender, message, room, isGroupChat, action, profileImage, packageName, bot ->
            try {
                chatLog(room, sender, message)
                with(message) {
                    when {
                        equals("A") -> action.reply("action.reply")
                        equals("B") -> bot.reply(action, "bot.reply")
                        equals("C") -> reply(action, "reply")
                        equals("즈모봇") -> action.reply("즈모봇 버전 $version 가동중...")
                        contains("살았니") || contains("죽었니") -> action.reply(
                            arrayOf(
                                "죽었다!",
                                "살았다!"
                            ).random()
                        )
                        contains("한강") -> {
                            val data = getHtml("https://api.winsub.kr/hangang/?key=$apiKey")
                            val json = JSONObject(data)
                            val temp = json.getString("temp")
                            val time = json.getString("time").split("년 ")[1]
                            val value = "$time 기준 현재 한강은 $temp 이에요!"
                            action.reply(value)
                        }
                        equals(".채팅로그") -> {
                            val path = PathManager.LOG.replace("room", room)
                            action.reply(
                                "$room 방의 채팅로그에요!\n전체보기를 눌러주세요 :)$showAll" + (StorageUtil.read(
                                    path,
                                    "기록된 채팅로그가 없어요 :("
                                ) ?: "기록된 채팅로그가 없어요 :(")
                            )
                        }
                        equals("초성게임") || equals("초성퀴즈") -> {
                            if (chosungAnswer.isBlank()) {
                                val quiz = Game.chosungQuiz()
                                val type = quiz[0] as String
                                val answer = quiz[1] as String
                                val chosung = (quiz[2] as ArrayList<*>).joinToString("")
                                val value =
                                    "$type 에 대한 초성입니다!\n\n- $chosung\n\n.정답 으로 정답을 입력해 주세요!"
                                chosungAnswer = answer
                                action.reply(value)
                            } else {
                                action.reply("이미 게임이 시작되어 있어요.")
                            }
                        }
                        equals("초성정답") && chosungAnswer.isNotBlank() -> action.reply("초성게임의 정답은 $chosungAnswer 이였어요!")
                        equals("초성힌트") && chosungAnswer.isNotBlank() -> {
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
                                        RhinoUtil.checkSameWord(
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
                action.reply("봇 작동중 오류가 발생했어요 ㅠㅠ\n\n$exception")
                exception.printStackTrace()
            }
        }
    }

    private fun chatLog(room: String, sender: String, message: String) {
        if (PermissionUtil.checkPermissionsAllGrant(
                applicationContext, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        ) {
            StorageUtil.createFolder("SungBinBot/ChatLog")
        }

        val path = PathManager.LOG.replace("room", room)
        val preData = StorageUtil.read(path, "")
        val timeFormat = SimpleDateFormat("MM월 dd일 kk시 mm분 ss초", Locale.KOREA)
        val time = timeFormat.format(Date())
        val value = "[$time] $sender\n$message"
        val newData = preData + "\n\n" + value
        StorageUtil.save(path, newData)
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
        if (System.currentTimeMillis() - replyTime >= 1000) {
            bot.reply(this, message.trim())
            replyTime = System.currentTimeMillis()
        }
    }

    private fun reply(
        action: Notification.Action,
        message: String,
        exception: (Exception) -> Unit = {}
    ) {
        try {
            val sendIntent = Intent()
            val msg = Bundle()
            for (inputable in action.remoteInputs) msg.putCharSequence(
                inputable.resultKey,
                message
            )
            RemoteInput.addResultsToIntent(action.remoteInputs, sendIntent, msg)
            action.actionIntent.send(applicationContext, 0, sendIntent)
        } catch (exception: Exception) {
            exception(exception)
        }
    }
}