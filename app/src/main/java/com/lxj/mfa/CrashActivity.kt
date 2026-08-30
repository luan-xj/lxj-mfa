package com.lxj.mfa

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * 上次崩溃后展示崩溃日志，方便无 adb 环境下排查。
 */
class CrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val logText = CrashHandler.readCrash(this)

        val tv = TextView(this).apply {
            text = logText
            setPadding(32, 32, 32, 32)
            textSize = 11f
            setTextIsSelectable(true)
            typeface = Typeface.MONOSPACE
        }

        val copy = Button(this).apply {
            text = "复制日志"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", logText))
                Toast.makeText(this@CrashActivity, "已复制，可粘贴发给开发者", Toast.LENGTH_LONG).show()
            }
        }

        val dismiss = Button(this).apply {
            text = "我知道了"
            setOnClickListener {
                CrashHandler.clearCrash(this@CrashActivity)
                finish()
            }
        }

        val scroll = ScrollView(this).apply {
            addView(tv)
            setPadding(16, 16, 16, 16)
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            addView(copy)
            addView(dismiss)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@CrashActivity, android.R.color.white))
            addView(scroll)
            addView(bar)
        }

        setContentView(root)
    }
}
