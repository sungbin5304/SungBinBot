package me.sungbin.sungbinbot.ui

import android.Manifest
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
import me.sungbin.sungbinbot.bot.Bot
import me.sungbin.sungbinbot.databinding.ActivityMainBinding
import me.sungbin.sungbinbot.service.ForgroundService
import me.sungbin.sungbinbot.util.PathManager
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val bot by lazy { KakaoBot() }
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val licenseKey by lazy { Firebase.remoteConfig.getString("licenseKey") }

    private val showAll = "\u200b".repeat(500)
    private val timeFormat = SimpleDateFormat("yyMMdd.kkmmss", Locale.KOREA)
    private val version by lazy { timeFormat.format(Date()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

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

        bot.setMessageReceiveListener { sender, message, room, isGroupChat, a, profileImage, packageName, bot ->
            try {
                with(message) {
                    when {
                        equals(".즈모봇") -> Bot.Message.version(a, version)
                        equals(".살았니") || equals(".죽었니") -> Bot.Message.live(a)
                        equals(".배터리") -> Bot.Message.battery(a)
                        equals(".한강") -> Bot.Tool.hangang(a)
                        equals(".끝말잇기") -> Bot.Game.Wc.power(a)
                        startsWith("wc") -> Bot.Game.Wc.game(a, message)
                        contains(".초성게임") || contains(".초성퀴즈") -> Bot.Game.Cs.power(a, message)
                        equals(".초성정답") -> Bot.Game.Cs.gg(a)
                        equals(".초성힌트") -> Bot.Game.Cs.hint(a)
                        startsWith("cs") -> Bot.Game.Cs.game(a, message)
                        else -> Unit
                    }
                }
            } catch (exception: Exception) {
                Bot.Tool.exception(a, exception)
            }
        }
    }
}