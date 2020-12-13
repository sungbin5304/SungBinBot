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
import com.sungbin.androidutils.util.*
import me.sungbin.kakaotalkbotbasemodule.library.KakaoBot
import me.sungbin.sungbinbot.R
import me.sungbin.sungbinbot.databinding.ActivityMainBinding
import me.sungbin.sungbinbot.service.ForgroundService
import me.sungbin.sungbinbot.util.PathManager
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val bot by lazy { KakaoBot() }
    private val password by lazy { Firebase.remoteConfig.getString("password") }
    private val apiKey by lazy { Firebase.remoteConfig.getString("apiKey") }
    private var runTime = System.currentTimeMillis()
    private val showAll = "\u200b".repeat(500)

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
                Manifest.permission.READ_EXTERNAL_STORAGE
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
                            val quote = json.getString("quote")
                            val value = "$time 기준 현재 한강은 $temp 이에요!\n\n- $quote"
                            action.reply(value)
                        }
                        contains("섹스") -> action.reply(arrayOf("할래?", "하자").random())
                        contains(".채팅로그") -> {
                            val path = PathManager.LOG.replace("room", room)
                            action.reply(
                                "$room 방의 채팅로그에요!\n전체보기를 눌러주세요 :)$showAll" + (StorageUtil.read(
                                    path,
                                    "기록된 채팅로그가 없어요 :("
                                ) ?: "기록된 채팅로그가 없어요 :(")
                            )
                        }
                    }
                }
            } catch (exception: Exception) {
                action.reply("봇 작동중 오류가 발생했어요 ㅠㅠ\n\n$exception")
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
        if (System.currentTimeMillis() - runTime >= 1000) {
            bot.reply(this, message.trim())
            runTime = System.currentTimeMillis()
        }
    }
}