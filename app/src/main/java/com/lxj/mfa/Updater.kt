package com.lxj.mfa

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String,
    val forceUpdate: Boolean
)

/**
 * 应用内更新：从 Gitee 仓库读取 update.json，比较版本后一键下载安装。
 * 调用 check(auto) 即可；auto=true 时静默（仅新版本且未被忽略才提示）。
 */
class Updater(private val activity: AppCompatActivity) {

    private val updateUrl = "https://gitee.com/luan_xiaojian/lxj-mfa/raw/master/update.json"

    private var pendingInstall: (() -> Unit)? = null
    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null

    private val permLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (canInstall()) pendingInstall?.invoke()
            else Toast.makeText(activity, R.string.update_no_permission, Toast.LENGTH_LONG).show()
        }

    init {
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = unregisterReceiver()
        })
    }

    fun check(auto: Boolean) {
        activity.lifecycleScope.launch {
            val info = fetchUpdateInfo()
            if (info == null) {
                if (!auto) Toast.makeText(activity, R.string.update_check_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (info.versionCode > BuildConfig.VERSION_CODE) {
                if (auto && Prefs.getIgnoredVersion(activity) == info.versionName) return@launch
                showUpdateDialog(info, auto)
            } else if (!auto) {
                Toast.makeText(activity, R.string.update_latest, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchUpdateInfo(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(updateUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                instanceFollowRedirects = true
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            if (!text.trimStart().startsWith("{")) return@withContext null
            val j = JSONObject(text)
            UpdateInfo(
                versionCode = j.optInt("versionCode", 0),
                versionName = j.optString("versionName", ""),
                apkUrl = j.optString("apkUrl", ""),
                changelog = j.optString("changelog", ""),
                forceUpdate = j.optBoolean("forceUpdate", false)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun showUpdateDialog(info: UpdateInfo, auto: Boolean) {
        val msg = buildString {
            append("最新版本：v${info.versionName}\n\n")
            if (info.changelog.isNotEmpty()) {
                append(activity.getString(R.string.update_changelog_label))
                append("\n").append(info.changelog)
            }
        }
        val builder = AlertDialog.Builder(activity)
            .setTitle(R.string.update_found_title)
            .setMessage(msg)
            .setPositiveButton(R.string.update_now) { _, _ -> startUpdate(info) }
        if (info.forceUpdate) {
            builder.setCancelable(false)
        } else {
            builder.setNegativeButton(R.string.update_later, null)
            if (auto) builder.setNeutralButton(R.string.update_ignore) { _, _ ->
                Prefs.setIgnoredVersion(activity, info.versionName)
            }
        }
        builder.show()
    }

    private fun startUpdate(info: UpdateInfo) {
        if (!canInstall()) {
            pendingInstall = { download(info) }
            permLauncher.launch(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
            )
            return
        }
        download(info)
    }

    private fun canInstall(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
        activity.packageManager.canRequestPackageInstalls()

    private fun download(info: UpdateInfo) {
        val dir = File(activity.getExternalFilesDir(null), "updates")
        dir.mkdirs()
        val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(info.apkUrl)).apply {
            setTitle(activity.getString(R.string.app_name) + " 更新")
            setDescription("v" + info.versionName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setMimeType("application/vnd.android.package-archive")
            setDestinationInExternalFilesDir(activity, null, "updates/LXJ-MFA-update.apk")
        }
        downloadId = dm.enqueue(req)
        Toast.makeText(activity, R.string.update_downloading, Toast.LENGTH_SHORT).show()
        registerReceiver()
    }

    private fun registerReceiver() {
        if (downloadReceiver != null) return
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (id != downloadId) return
                val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val cur = dm.query(DownloadManager.Query().setFilterById(downloadId))
                if (cur.moveToFirst()) {
                    val status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) installApk()
                    else Toast.makeText(activity, R.string.update_download_failed, Toast.LENGTH_LONG).show()
                }
                cur.close()
                unregisterReceiver()
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            activity.registerReceiver(downloadReceiver, filter)
        }
    }

    private fun unregisterReceiver() {
        downloadReceiver?.let {
            try { activity.unregisterReceiver(it) } catch (_: Exception) {}
        }
        downloadReceiver = null
    }

    private fun isZipFile(file: File): Boolean = try {
        val head = ByteArray(2)
        val n = file.inputStream().use { it.read(head) }
        n == 2 && head[0] == 0x50.toByte() && head[1] == 0x4B.toByte()
    } catch (_: Exception) { false }

    private fun installApk() {
        val file = File(activity.getExternalFilesDir(null), "updates/LXJ-MFA-update.apk")
        when {
            !file.exists() || file.length() < 1_000_000 -> {
                Toast.makeText(activity, R.string.update_download_failed, Toast.LENGTH_LONG).show()
            }
            !isZipFile(file) -> {
                Toast.makeText(activity, "下载文件不是有效的安装包，请重试", Toast.LENGTH_LONG).show()
            }
            else -> {
                try {
                    val uri = FileProvider.getUriForFile(activity, activity.packageName + ".fileprovider", file)
                    val intent = Intent(Intent.ACTION_INSTALL_PACKAGE, uri).apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(activity, "安装失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
