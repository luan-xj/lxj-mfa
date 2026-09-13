package com.lxj.mfa

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.KeyStore
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 全局偏好（凭据、Git 配置、密码哈希等），均存放在 EncryptedSharedPreferences 中，
 * 文件本身由 Android 主密钥加密，进一步保护 Token / 密码哈希。
 */
object Prefs {
    private const val NAME = "lxj_mfa_prefs"

    private fun sp(ctx: Context) = EncryptedSharedPreferences.create(
        NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        ctx,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ---------- 主密码 ----------
    fun hasPassword(ctx: Context) = sp(ctx).contains("pw_hash")

    fun setPassword(ctx: Context, password: String) {
        val salt = Crypto.randomSalt(16)
        val hash = pbkdf(password, salt)
        sp(ctx).edit()
            .putString("pw_salt", Base64.getEncoder().encodeToString(salt))
            .putString("pw_hash", Base64.getEncoder().encodeToString(hash))
            .apply()
    }

    fun verifyPassword(ctx: Context, password: String): Boolean {
        val s = sp(ctx)
        val salt = Base64.getDecoder().decode(s.getString("pw_salt", "") ?: return false)
        val hash = Base64.getDecoder().decode(s.getString("pw_hash", "") ?: return false)
        return pbkdf(password, salt).contentEquals(hash)
    }

    private fun pbkdf(pw: String, salt: ByteArray): ByteArray {
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return f.generateSecret(PBEKeySpec(pw.toCharArray(), salt, 120_000, 256)).encoded
    }

    // ---------- Git 配置 ----------
    fun getRepo(ctx: Context) = sp(ctx).getString("git_repo", "") ?: ""
    fun setRepo(ctx: Context, v: String) = sp(ctx).edit().putString("git_repo", v).apply()
    fun getBranch(ctx: Context) = sp(ctx).getString("git_branch", "main") ?: "main"
    fun setBranch(ctx: Context, v: String) = sp(ctx).edit().putString("git_branch", v).apply()
    // Git 用户名：HTTPS Token 认证时多数平台忽略此值，可自定义；
    // 默认 "lxj-mfa" 以兼容旧版写死行为。留空时同步逻辑会回退到该默认值。
    fun getGitUser(ctx: Context) = sp(ctx).getString("git_user", "lxj-mfa") ?: "lxj-mfa"
    fun setGitUser(ctx: Context, v: String) = sp(ctx).edit().putString("git_user", v).apply()
    // Git Token 用 AndroidKeyStore(AES) 加密后再存，不以明文落盘。
    // 兼容旧版明文：若解密失败，视为旧数据，迁移为密文后返回明文。
    fun getToken(ctx: Context): String {
        val raw = sp(ctx).getString("git_token", "") ?: ""
        if (raw.isEmpty()) return ""
        return try {
            Crypto.decrypt(raw)
        } catch (_: Exception) {
            setToken(ctx, raw) // 旧版明文 → 迁移为加密
            raw
        }
    }

    fun setToken(ctx: Context, v: String) {
        val enc = if (v.isEmpty()) "" else Crypto.encrypt(v)
        sp(ctx).edit().putString("git_token", enc).apply()
    }
    // 默认开启加密备份，避免账号密钥以明文落入 Git 历史（旧安装如果已存过 false，会保留原值）
    fun getEncryptBackup(ctx: Context) = sp(ctx).getBoolean("encrypt_backup", true)
    fun setEncryptBackup(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("encrypt_backup", v).apply()

    // ---------- 同步密码（独立于主密码，仅用于 Git 备份加解密） ----------
    // 同样用 Keystore 加密存储，不以明文落盘；旧版明文自动迁移。
    fun hasSyncPassword(ctx: Context) = sp(ctx).contains("sync_pw")

    fun setSyncPassword(ctx: Context, pw: String) {
        val enc = if (pw.isEmpty()) "" else Crypto.encrypt(pw)
        sp(ctx).edit().putString("sync_pw", enc).apply()
    }

    fun getSyncPassword(ctx: Context): String {
        val raw = sp(ctx).getString("sync_pw", "") ?: ""
        if (raw.isEmpty()) return ""
        return try {
            Crypto.decrypt(raw)
        } catch (_: Exception) {
            raw // 极旧兜底：若不是密文则当作明文直接返回（不会再写回明文）
        }
    }

    fun clearSyncPassword(ctx: Context) = sp(ctx).edit().remove("sync_pw").apply()

    // ---------- 备份加密 salt（与主密码独立的一份） ----------
    fun getBackupSalt(ctx: Context): ByteArray {
        val s = sp(ctx)
        s.getString("backup_salt", null)?.let { return Base64.getDecoder().decode(it) }
        val salt = Crypto.randomSalt(16)
        s.edit().putString("backup_salt", Base64.getEncoder().encodeToString(salt)).apply()
        return salt
    }

    // ---------- 后台锁定超时（分钟）：-1 从不，0 立即，其余为分钟数；默认 0（立即） ----------
    fun getBgLockTimeout(ctx: Context) = sp(ctx).getInt("bg_lock_timeout", 0)
    fun setBgLockTimeout(ctx: Context, minutes: Int) =
        sp(ctx).edit().putInt("bg_lock_timeout", minutes).apply()

    // ---------- 已忽略的更新版本（自动检查时不再提示） ----------
    fun getIgnoredVersion(ctx: Context) = sp(ctx).getString("ignored_update_version", "") ?: ""
    fun setIgnoredVersion(ctx: Context, v: String) =
        sp(ctx).edit().putString("ignored_update_version", v).apply()

    // ---------- 自动更新检查时间戳（24 小时节流，减少冷启动网络请求） ----------
    fun getLastUpdateCheck(ctx: Context) = sp(ctx).getLong("last_update_check", 0L)
    fun setLastUpdateCheck(ctx: Context, ms: Long) =
        sp(ctx).edit().putLong("last_update_check", ms).apply()

    // ---------- 列表排序方式：0=按最近访问，1=按名称（默认 0=最近访问） ----------
    fun getSortMode(ctx: Context) = sp(ctx).getInt("sort_mode", 0)
    fun setSortMode(ctx: Context, mode: Int) =
        sp(ctx).edit().putInt("sort_mode", mode).apply()
}
