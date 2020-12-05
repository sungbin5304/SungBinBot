package me.sungbin.sungbinbot.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sungbin.androidutils.util.DataUtil
import me.sungbin.sungbinbot.databinding.ActivityMainBinding
import me.sungbin.sungbinbot.util.PathManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.swPower.setOnCheckedChangeListener { _, isChecked ->
            DataUtil.saveData(applicationContext, PathManager.POWER, isChecked.toString())
        }


    }
}