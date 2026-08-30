package com.lxj.mfa.totp

import com.lxj.mfa.data.Account
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 统一 OTP 生成入口：按账号的 type 分派到 TOTP / HOTP / STEAM / MOTP，
 * 并支持 SHA1 / SHA256 / SHA512 算法（TOTP、HOTP 适用）。
 */
object Otp {

    /** 根据账号类型生成当前验证码（secret 为已解密的明文密钥）。 */
    fun code(a: Account, secret: String): String = codeAt(a, secret)

    /** 生成指定时刻的验证码（timeMillis 可指定时刻，用于预览“下一码”）。 */
    fun codeAt(a: Account, secret: String, timeMillis: Long = System.currentTimeMillis()): String {
        val t = a.type.uppercase()
        return when (t) {
            "HOTP" -> hotp(Totp.base32Decode(secret), a.counter.toLong(), a.digits, a.algorithm)
            "STEAM" -> steam(Totp.base32Decode(secret), timeMillis)
            "MOTP" -> motp(secret, timeMillis)
            else -> Totp.generate(secret, a.period, a.digits, a.algorithm, timeMillis) // TOTP
        }
    }

    /** 时间型验证码的有效周期（HOTP 返回 0 表示非时间型）。 */
    fun effectivePeriod(a: Account): Int = when (a.type.uppercase()) {
        "MOTP" -> 10
        "HOTP" -> 0
        "STEAM" -> 30
        else -> a.period
    }

    /** 是否为时间型（会随时间刷新）。 */
    fun isTimeBased(a: Account): Boolean = a.type.uppercase() != "HOTP"

    /** 距离下一次刷新的剩余秒数。 */
    fun remaining(period: Int): Int = Totp.remaining(period)

    // ---------------- HOTP (RFC 4226) ----------------
    private fun hotp(key: ByteArray, counter: Long, digits: Int, algorithm: String): String {
        val msg = ByteArray(8)
        var v = counter
        for (i in 7 downTo 0) {
            msg[i] = (v and 0xff).toByte()
            v = v ushr 8
        }
        val mac = Mac.getInstance("Hmac$algorithm")
        mac.init(SecretKeySpec(key, "Hmac$algorithm"))
        val h = mac.doFinal(msg)
        val offset = h[h.size - 1].toInt() and 0x0f
        var bin = ((h[offset].toLong() and 0x7f) shl 24) or
                ((h[offset + 1].toLong() and 0xff) shl 16) or
                ((h[offset + 2].toLong() and 0xff) shl 8) or
                (h[offset + 3].toLong() and 0xff)
        bin = bin and 0x7fffffff
        val mod = Math.pow(10.0, digits.toDouble()).toLong()
        var code = (bin % mod).toString()
        while (code.length < digits) code = "0$code"
        return code
    }

    // ---------------- STEAM (Steam Guard) ----------------
    private val STEAM_ALPHABET = "23456789BCDFGHJKMNPQRTVWXY"

    private fun steam(key: ByteArray, timeMillis: Long = System.currentTimeMillis()): String {
        val time = timeMillis / 1000 / 30
        val msg = ByteArray(8)
        var v = time
        for (i in 7 downTo 0) {
            msg[i] = (v and 0xff).toByte()
            v = v ushr 8
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val h = mac.doFinal(msg)
        val start = h[h.size - 1].toInt() and 0x0f
        var b = ((h[start].toLong() and 0xff) shl 24) or
                ((h[start + 1].toLong() and 0xff) shl 16) or
                ((h[start + 2].toLong() and 0xff) shl 8) or
                (h[start + 3].toLong() and 0xff)
        b = b and 0x7fffffff
        var code = ""
        for (i in 0 until 5) {
            code += STEAM_ALPHABET[(b % 26).toInt()]
            b /= 26
        }
        return code
    }

    // ---------------- MOTP (Mobile-OTP) ----------------
    // secret 形如 "十六进制密钥:PIN"，例如 "0123456789ABCDEF:1234"；缺省 PIN 视为空。
    private fun motp(secretWithPin: String, timeMillis: Long = System.currentTimeMillis()): String {
        val (secret, pin) = if (secretWithPin.contains(":")) {
            val p = secretWithPin.split(":", limit = 2)
            p[0] to p.getOrElse(1) { "" }
        } else secretWithPin to ""
        val t = timeMillis / 1000 / 10
        val msg = "$t$secret$pin"
        val md = MessageDigest.getInstance("MD5")
        val hash = md.digest(msg.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }.substring(0, 6)
    }
}
