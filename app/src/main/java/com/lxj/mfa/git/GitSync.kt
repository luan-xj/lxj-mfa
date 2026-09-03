package com.lxj.mfa.git

import android.content.Context
import com.lxj.mfa.Crypto
import com.lxj.mfa.Prefs
import com.lxj.mfa.data.Account
import com.lxj.mfa.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Base64

/**
 * 基于 JGit 的 Git 同步（HTTPS + Personal Access Token）。
 * 备份文件固定为仓库根目录下的 lxj-mfa-backup.json。
 */
object GitSync {
    private const val FILE = "lxj-mfa-backup.json"

    private fun repoDir(ctx: Context) = File(ctx.filesDir, "lxj_mfa_repo")

    suspend fun push(ctx: Context) = withContext(Dispatchers.IO) {
        val repoUrl = Prefs.getRepo(ctx)
        val branch = Prefs.getBranch(ctx)
        val token = Prefs.getToken(ctx)
        val user = Prefs.getGitUser(ctx).ifBlank { "lxj-mfa" }
        val creds = UsernamePasswordCredentialsProvider(user, token)
        val dir = repoDir(ctx)
        val git: Git = if (dir.exists() && File(dir, ".git").exists()) {
            Git.open(dir)
        } else {
            Git.cloneRepository().setURI(repoUrl).setDirectory(dir).setBranch(branch)
                .setCredentialsProvider(creds).call()
        }
        try {
            git.fetch().setCredentialsProvider(creds).call()
            try {
                git.pull().setRemote("origin").setRemoteBranchName(branch)
                    .setCredentialsProvider(creds).call()
            } catch (_: Exception) { /* 可能没有上游分支，忽略 */ }

            val list = AppDatabase.get(ctx).dao().getAllList()
            var content = buildJson(list)
            if (Prefs.getEncryptBackup(ctx)) {
                val pwd = Prefs.getSyncPassword(ctx)
                if (pwd.isEmpty()) throw IllegalArgumentException("请先在设置中设置同步密码")
                // 关键：salt 随备份文件一起导出，换设备/重装后用同一密码即可解密
                val salt = Crypto.randomSalt(16)
                val (iv, data) = Crypto.encryptWithPassword(content, pwd, salt)
                content = JSONObject().apply {
                    put("encrypted", true)
                    put("version", 1)
                    put("salt", Base64.getEncoder().encodeToString(salt))
                    put("iv", iv)
                    put("data", data)
                }.toString(2)
            }
            val file = File(dir, FILE)
            file.writeText(content)
            git.add().addFilepattern(FILE).call()
            git.commit().setMessage("LXJ-MFA sync ${System.currentTimeMillis()}").call()
            git.push().setCredentialsProvider(creds).setPushAll().call()
        } finally {
            git.close()
        }
    }

    suspend fun pull(ctx: Context) = withContext(Dispatchers.IO) {
        val branch = Prefs.getBranch(ctx)
        val token = Prefs.getToken(ctx)
        val user = Prefs.getGitUser(ctx).ifBlank { "lxj-mfa" }
        val creds = UsernamePasswordCredentialsProvider(user, token)
        val dir = repoDir(ctx)
        val git: Git = if (dir.exists() && File(dir, ".git").exists()) {
            Git.open(dir)
        } else {
            Git.cloneRepository().setURI(Prefs.getRepo(ctx)).setDirectory(dir).setBranch(branch)
                .setCredentialsProvider(creds).call()
        }
        try {
            git.fetch().setCredentialsProvider(creds).call()
            try {
                git.pull().setRemote("origin").setRemoteBranchName(branch)
                    .setCredentialsProvider(creds).call()
            } catch (_: Exception) { /* ignore */ }
            val file = File(dir, FILE)
            if (!file.exists()) return@withContext
            val items = parseJson(file.readText(), ctx)
            val dao = AppDatabase.get(ctx).dao()
            for (a in items) dao.upsert(a)
        } finally {
            git.close()
        }
    }

    private fun buildJson(list: List<Account>): String {
        val arr = JSONArray()
        for (a in list) {
            arr.put(
                JSONObject().apply {
                    put("id", a.id)
                    put("issuer", a.issuer)
                    put("label", a.label)
                    put("secret", Crypto.decrypt(a.secretEnc))
                    put("type", a.type)
                    put("algorithm", a.algorithm)
                    put("digits", a.digits)
                    put("period", a.period)
                    put("tag", a.tag)
                    put("counter", a.counter)
                    put("createdAt", a.createdAt)
                }
            )
        }
        return JSONObject().put("version", 1).put("accounts", arr).toString(2)
    }

    private fun parseJson(raw: String, ctx: Context): List<Account> {
        val root = JSONObject(raw)
        val obj = if (root.optBoolean("encrypted", false)) {
            val pwd = Prefs.getSyncPassword(ctx)
            if (pwd.isEmpty()) throw IllegalArgumentException("需要同步密码才能解密备份")
            // 优先用文件内携带的 salt；旧备份无 salt 字段时回退到本地存储的 salt
            val salt = if (root.has("salt")) {
                Base64.getDecoder().decode(root.getString("salt"))
            } else {
                Prefs.getBackupSalt(ctx)
            }
            try {
                JSONObject(Crypto.decryptWithPassword(root.getString("iv"), root.getString("data"), pwd, salt))
            } catch (e: java.security.GeneralSecurityException) {
                throw IllegalArgumentException("同步密码错误，无法解密备份文件", e)
            }
        } else root
        val arr = obj.getJSONArray("accounts")
        val out = mutableListOf<Account>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Account(
                    id = o.optLong("id", 0),
                    issuer = o.optString("issuer", ""),
                    label = o.optString("label", ""),
                    secretEnc = Crypto.encrypt(o.getString("secret")),
                    type = o.optString("type", "TOTP"),
                    algorithm = o.optString("algorithm", "SHA1"),
                    digits = o.optInt("digits", 6),
                    period = o.optInt("period", 30),
                    tag = o.optString("tag", ""),
                    counter = o.optInt("counter", 0),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
        return out
    }
}
