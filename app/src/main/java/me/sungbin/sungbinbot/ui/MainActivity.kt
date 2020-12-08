package me.sungbin.sungbinbot.ui

import android.Manifest
import android.app.Notification
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.sungbin.androidutils.extensions.hide
import com.sungbin.androidutils.extensions.show
import com.sungbin.androidutils.util.*
import me.sungbin.kakaotalkbotbasemodule.library.KakaoBot
import me.sungbin.sungbinbot.R
import me.sungbin.sungbinbot.databinding.ActivityMainBinding
import me.sungbin.sungbinbot.datastore.DataManager
import me.sungbin.sungbinbot.util.PathManager
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var preLog: String
    private lateinit var binding: ActivityMainBinding
    private val bot = KakaoBot()
    private val password = Firebase.remoteConfig.getString("password")
    private val apiKey = Firebase.remoteConfig.getString("apiKey")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PermissionUtil.request(
            this,
            null,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
        bot.requestReadNotification()

        binding.swPower.isChecked =
            DataUtil.readData(applicationContext, PathManager.POWER, "false").toBoolean()
        binding.swPower.setOnCheckedChangeListener { _, isChecked ->
            DataUtil.saveData(applicationContext, PathManager.POWER, isChecked.toString())
            bot.setPower(isChecked)
        }

        binding.fabClearLog.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.main_clear_log))
                .setMessage(getString(R.string.main_question_clear_log))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    lifecycleScope.launchWhenCreated {
                        DataManager.setLog("")
                    }
                    ToastUtil.show(
                        applicationContext,
                        getString(R.string.main_cleared_log),
                        ToastLength.LONG,
                        ToastType.SUCCESS
                    )
                }
                .show()
        }

        DataManager.logFlow.asLiveData().observe(this) {
            preLog = it
            if (it.isEmpty()) {
                hideLog()
            } else {
                binding.tvLog.text = it
            }
        }

        bot.setMessageReceiveListener { sender, message, room, isGroupChat, action, profileImage, packageName, bot ->
            with(message) {
                when {
                    contains(".살았니") -> action.reply("죽었다!")
                    contains("한강") ->
                }
            }
        }
    }

    private fun Notification.Action.reply(message: String) {
        bot.reply(this, message)
        lifecycleScope.launchWhenCreated {
            DataManager.setLog("$preLog\n${message.toLog()}")
        }
    }

    private fun String.toLog(): String {
        val format = SimpleDateFormat("M월 dd일 HH시 mm분 ss초", Locale.KOREA)
        return "[${format.format(Date())}] $this"
    }

    private fun showLog() {
        binding.svLogContainer.show()
        binding.fblContainer.hide(true)
        binding.lavEmpty.cancelAnimation()
        binding.fabClearLog.show()
    }

    private fun hideLog() {
        binding.svLogContainer.hide(true)
        binding.fblContainer.show()
        binding.lavEmpty.playAnimation()
        binding.fabClearLog.hide()
    }
}