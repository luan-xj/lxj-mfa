package com.lxj.mfa.totp

import android.net.Uri

data class OtpInfo(
    val issuer: String,
    val label: String,
    val secret: String,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30
)

/**
 * 解析 otpauth://totp/... 链接（扫码导入常用格式）。
 */
object OtpAuth {
    fun parse(uri: String): OtpInfo? {
        val u = Uri.parse(uri)
        if (u.scheme != "otpauth") return null
        val secret = u.getQueryParameter("secret") ?: return null
        var label = Uri.decode(u.lastPathSegment ?: "")
        var issuer = u.getQueryParameter("issuer") ?: ""
        if (issuer.isEmpty() && label.contains(":")) {
            val parts = label.split(":", limit = 2)
            issuer = parts[0].trim()
            label = parts[1].trim()
        }
        val algorithm = u.getQueryParameter("algorithm")
            ?.uppercase()
            ?.takeIf { it in setOf("SHA1", "SHA256", "SHA512") } ?: "SHA1"
        val digits = u.getQueryParameter("digits")?.toIntOrNull() ?: 6
        val period = u.getQueryParameter("period")?.toIntOrNull() ?: 30
        return OtpInfo(issuer, label, secret, algorithm, digits, period)
    }
}
