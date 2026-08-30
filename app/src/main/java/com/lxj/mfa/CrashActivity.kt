package com.lxj.mfa

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * 崩溃日志查看器：由设置页「查看崩溃日志」进入。
 * 展示历史崩溃堆栈，可复制并提交到本项目 Issues，也可清除。
 */
class CrashActivity : AppCompatActivity() {

    private val issuesUrl = "https://gitee.com/luan_xiaojian/lxj-mfa/issues"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val logText = CrashHandler.readCrash(this)
        val hasLog = CrashHandler.hasCrash(this)

        val banner = TextView(this).apply {
            text = "💡 提示：如遇到崩溃，请先「复制日志」，再点「清除日志」删除本机记录，最后点「前往提交 Issue」把日志贴到项目 Issues 反馈给开发者。"
            setPadding(24, 18, 24, 18)
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@CrashActivity, android.R.color.black))
            setBackgroundColor(ContextCompat.getColor(this@CrashActivity, R.color.primary_light))
        }

        val tv = TextView(this).apply {
            text = if (hasLog) logText else getString(R.string.crash_no_log)
            setPadding(32, 32, 32, 32)
            textSize = 11f
            setTextIsSelectable(true)
            typeface = Typeface.MONOSPACE
        }

        val scroll = ScrollView(this).apply {
            addView(tv)
            setPadding(16, 16, 16, 16)
        }

        val hint = TextView(this).apply {
            text = getString(R.string.crash_submit_hint)
            setPadding(24, 8, 24, 8)
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@CrashActivity, android.R.color.black))
        }

        val btnCopy = Button(this).apply {
            text = getString(R.string.crash_copy)
            isEnabled = hasLog
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", logText))
                Toast.makeText(this@CrashActivity, R.string.crash_copied, Toast.LENGTH_LONG).show()
            }
        }

        val btnClear = Button(this).apply {
            text = getString(R.string.crash_clear)
            isEnabled = hasLog
            setBackgroundColor(ContextCompat.getColor(this@CrashActivity, android.R.color.white))
            setTextColor(ContextCompat.getColor(this@CrashActivity, R.color.primary))
            setOnClickListener {
                CrashHandler.clearCrash(this@CrashActivity)
                Toast.makeText(this@CrashActivity, R.string.crash_cleared, Toast.LENGTH_SHORT).show()
                tv.text = getString(R.string.crash_no_log)
                btnCopy.isEnabled = false
                this.isEnabled = false
            }
        }

        val btnOpenIssue = Button(this).apply {
            text = getString(R.string.crash_open_issue)
            setTextColor(ContextCompat.getColor(this@CrashActivity, android.R.color.white))
            setBackgroundColor(ContextCompat.getColor(this@CrashActivity, R.color.primary))
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(issuesUrl)))
            }
        }

        val btnBack = Button(this).apply {
            text = getString(R.string.back)
            setOnClickListener { finish() }
        }

        val barTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 12, 16, 4)
            addView(btnCopy)
            addView(btnClear)
        }
        val barBottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 4, 16, 12)
            addView(btnOpenIssue)
            addView(btnBack)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@CrashActivity, android.R.color.white))
            addView(banner)
            addView(scroll)
            addView(hint)
            addView(barTop)
            addView(barBottom)
        }

        setContentView(root)
    }
}
