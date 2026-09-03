package com.lxj.mfa.ui

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lxj.mfa.Prefs
import com.lxj.mfa.R
import com.lxj.mfa.git.GitSync
import kotlinx.coroutines.launch

/**
 * 统一的 Git 同步入口：弹进度框，必要时询问备份密码。
 * 主界面的快捷同步与设置页的同步按钮共用此逻辑。
 */
object SyncRunner {

    fun push(activity: AppCompatActivity, onDone: (String) -> Unit) {
        if (Prefs.getRepo(activity).isBlank() || Prefs.getToken(activity).isBlank()) {
            onDone("请先在设置中填写 Git 仓库地址与 Token"); return
        }
        if (Prefs.getEncryptBackup(activity)) {
            if (!Prefs.hasSyncPassword(activity)) {
                onDone(activity.getString(R.string.sync_password_needed)); return
            }
            doPush(activity, onDone)
        } else {
            // 明文备份：密钥将永久留存于 Git 提交历史，推送前强制警告
            AlertDialog.Builder(activity)
                .setTitle("明文备份警告")
                .setMessage("当前未开启「同步时加密备份」，账号密钥将以明文保存到 Git 仓库，并永久保留在提交历史中，仓库或令牌泄露即导致全部 2FA 失守。强烈建议先开启加密备份并设置同步密码。\n\n是否仍要明文同步？")
                .setPositiveButton("仍要明文同步") { _, _ -> doPush(activity, onDone) }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun doPush(activity: AppCompatActivity, onDone: (String) -> Unit) {
        val dialog = AlertDialog.Builder(activity).setMessage("正在同步到 Git…").setCancelable(false).create()
        dialog.show()
        activity.lifecycleScope.launch {
            try {
                GitSync.push(activity)
                dialog.dismiss(); onDone(activity.getString(R.string.sync_success))
            } catch (e: Exception) {
                dialog.dismiss(); onDone(activity.getString(R.string.sync_failed, e.message ?: e.toString()))
            }
        }
    }

    fun pull(activity: AppCompatActivity, onDone: (String) -> Unit) {
        if (Prefs.getRepo(activity).isBlank() || Prefs.getToken(activity).isBlank()) {
            onDone("请先在设置中填写 Git 仓库地址与 Token"); return
        }
        if (Prefs.getEncryptBackup(activity) && !Prefs.hasSyncPassword(activity)) {
            onDone(activity.getString(R.string.sync_password_needed)); return
        }
        val dialog = AlertDialog.Builder(activity).setMessage("正在从 Git 拉取…").setCancelable(false).create()
        dialog.show()
        activity.lifecycleScope.launch {
            try {
                GitSync.pull(activity)
                dialog.dismiss(); onDone(activity.getString(R.string.pull_success))
            } catch (e: Exception) {
                dialog.dismiss(); onDone(activity.getString(R.string.sync_failed, e.message ?: e.toString()))
            }
        }
    }
}
