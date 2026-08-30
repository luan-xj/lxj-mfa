package com.lxj.mfa.totp

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 6238 TOTP 实现（默认 SHA1，兼容 Google Authenticator）。
 */
object Totp {

    fun generate(
        secretBase32: String,
        period: Int,
        digits: Int,
        algorithm: String,
        timeMillis: Long = System.currentTimeMillis()
    ): String {
        val key = base32Decode(secretBase32.replace(" ", ""))
        val counter = timeMillis / 1000 / period
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

    fun remaining(period: Int, timeMillis: Long = System.currentTimeMillis()): Int {
        val sec = (timeMillis / 1000) % period
        return (period - sec).toInt()
    }

    fun base32Decode(input: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val clean = input.uppercase().replace("=", "").replace("\\s".toRegex(), "")
        var bits = 0
        var value = 0
        val out = java.io.ByteArrayOutputStream()
        for (c in clean) {
            val idx = alphabet.indexOf(c)
            if (idx < 0) continue
            value = (value shl 5) or idx
            bits += 5
            if (bits >= 8) {
                out.write((value ushr (bits - 8)) and 0xFF)
                bits -= 8
            }
        }
        return out.toByteArray()
    }
}
