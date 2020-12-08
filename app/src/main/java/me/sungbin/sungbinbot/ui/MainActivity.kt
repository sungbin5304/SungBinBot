package me.sungbin.sungbinbot.ui

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.sungbin.androidutils.extensions.hide
import com.sungbin.androidutils.extensions.show
import com.sungbin.androidutils.util.DataUtil
import com.sungbin.androidutils.util.ToastLength
import com.sungbin.androidutils.util.ToastType
import com.sungbin.androidutils.util.ToastUtil
import me.sungbin.kakaotalkbotbasemodule.library.KakaoBot
import me.sungbin.sungbinbot.R
import me.sungbin.sungbinbot.databinding.ActivityMainBinding
import me.sungbin.sungbinbot.datastore.DataManager
import me.sungbin.sungbinbot.util.PathManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val bot = KakaoBot()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        DataManager.init(applicationContext)

        bot.requestReadNotification()

        binding.swPower.setOnCheckedChangeListener { _, isChecked ->
            DataUtil.saveData(applicationContext, PathManager.POWER, isChecked.toString())
            bot.setPower(isChecked)
        }

        binding.fabClearLog.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.main_clear_log))
                .setMessage(getString(R.string.main_question_clear_log))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    // todo
                    hideLog()
                    ToastUtil.show(
                        applicationContext,
                        getString(R.string.main_cleared_log),
                        ToastLength.LONG,
                        ToastType.SUCCESS
                    )
                }
                .show()
        }

        bot.setMessageReceiveListener { sender, message, room, isGroupChat, action, profileImage, packageName, bot ->
            with(message) {
                when {
                    contains(".살았니") -> bot.reply(action, "죽었다!")
                }
            }
        }
    }

    private fun showLog() {
        binding.svLogContainer.show()
        binding.fblContainer.hide(true)
        binding.lavEmpty.cancelAnimation()
    }

    private fun hideLog() {
        binding.svLogContainer.hide(true)
        binding.fblContainer.show()
        binding.lavEmpty.playAnimation()
    }
}