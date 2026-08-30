package com.lxj.mfa

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局未捕获异常处理器：
 * 把崩溃堆栈写到 App 私有目录 crash.log，下次冷启动时由 App 弹出展示，
 * 方便在没有 adb 的环境下定位问题。
 */
object CrashHandler {
    private const val TAG = "LXJCrash"
    private const val FILE = "crash.log"

    fun init(context: Context) {
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrash(context.applicationContext, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "write crash failed", e)
            }
            def?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrash(ctx: Context, t: Throwable) {
        val sw = StringWriter()
        sw.append("=== LXJ-MFA Crash ===\n")
        sw.append("Time: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())).append("\n")
        runCatching {
            sw.append("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
        }
        sw.append("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        sw.append("Device: ${Build.MANUFACTURER} ${Build.BRAND} ${Build.MODEL}\n")
        sw.append("Thread: ${threadName(t)}\n\n")
        t.printStackTrace(PrintWriter(sw))
        sw.append("\n")
        val file = File(ctx.getExternalFilesDir(null), FILE)
        file.appendText(sw.toString())
    }

    private fun threadName(t: Throwable): String {
        val element = t.stackTrace?.firstOrNull()
        return element?.className?.let { "${it}.${element.methodName}" } ?: "(unknown)"
    }

    fun hasCrash(ctx: Context): Boolean {
        val file = File(ctx.getExternalFilesDir(null), FILE)
        return file.exists() && file.length() > 0
    }

    fun readCrash(ctx: Context): String {
        val file = File(ctx.getExternalFilesDir(null), FILE)
        return runCatching { file.readText() }.getOrDefault("(无法读取崩溃日志)")
    }

    fun clearCrash(ctx: Context) {
        runCatching { File(ctx.getExternalFilesDir(null), FILE).delete() }
    }
}
