package me.sungbin.sungbinbot.ui

import android.Manifest
import android.app.Notification
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.sungbin.androidutils.util.*
import me.sungbin.kakaotalkbotbasemodule.library.KakaoBot
import me.sungbin.sungbinbot.R
import me.sungbin.sungbinbot.databinding.ActivityMainBinding
import me.sungbin.sungbinbot.util.PathManager
import org.json.JSONObject
import org.jsoup.Jsoup

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val bot by lazy { KakaoBot() }
    private val password by lazy { Firebase.remoteConfig.getString("password") }
    private val apiKey by lazy { Firebase.remoteConfig.getString("apiKey") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bot.init(applicationContext)
        bot.requestReadNotification()

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
                            } else {
                                bot.setPower(true)
                                ToastUtil.show(
                                    applicationContext,
                                    getString(R.string.main_hello),
                                    ToastLength.SHORT,
                                    ToastType.SUCCESS
                                )
                                DataUtil.saveData(applicationContext, PathManager.LICENSE, "true")
                                DataUtil.saveData(applicationContext, PathManager.POWER, "true")
                            }
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    bot.setPower(true)
                    DataUtil.saveData(applicationContext, PathManager.POWER, "true")
                }
            } else {
                bot.setPower(false)
                DataUtil.saveData(applicationContext, PathManager.POWER, "false")
            }
        }

        bot.setMessageReceiveListener { sender, message, room, isGroupChat, action, profileImage, packageName, bot ->
            try {
                with(message) {
                    when {
                        contains("살았니") || contains("죽었니") -> action.reply(
                            arrayOf(
                                "죽었다!",
                                "살았다!"
                            )[(0..1).random()]
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
                        contains("섹스") -> action.reply("하자")
                    }
                }
            } catch (exception: Exception) {
                action.reply("봇 작동중 오류가 발생했어요 ㅠㅠ\n\n$exception")
            }
        }
    }

    private fun log(string: String) = Log.w("AAAAA", string)

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