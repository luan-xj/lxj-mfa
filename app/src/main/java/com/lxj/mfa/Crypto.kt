package com.lxj.mfa

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * 加密工具：
 * 1) 本地数据库字段使用 AndroidKeyStore 中的 AES 密钥加密（密钥由系统硬件/沙箱保护）。
 * 2) Git 备份文件可再使用「主密码派生密钥」加密，导出到远端后即使泄露也无法解密。
 */
object Crypto {
    private const val KS = "AndroidKeyStore"
    private const val ALIAS = "lxj_mfa_db"
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val IV_LEN = 12

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance(KS).apply { load(null) }
        if (!ks.containsAlias(ALIAS)) {
            val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KS)
            gen.init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            gen.generateKey()
        }
        return (ks.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun encrypt(plain: String): String {
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val iv = c.iv
        val enc = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        val out = ByteArray(iv.size + enc.size)
        System.arraycopy(iv, 0, out, 0, iv.size)
        System.arraycopy(enc, 0, out, iv.size, enc.size)
        return Base64.getEncoder().encodeToString(out)
    }

    fun decrypt(data: String): String {
        val raw = Base64.getDecoder().decode(data)
        val iv = raw.copyOfRange(0, IV_LEN)
        val enc = raw.copyOfRange(IV_LEN, raw.size)
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
        return String(c.doFinal(enc), Charsets.UTF_8)
    }

    fun encryptWithPassword(plain: String, password: String, salt: ByteArray): Pair<String, String> {
        val key = deriveKey(password, salt)
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.ENCRYPT_MODE, key)
        val iv = c.iv
        val enc = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv) to Base64.getEncoder().encodeToString(enc)
    }

    fun decryptWithPassword(ivB64: String, dataB64: String, password: String, salt: ByteArray): String {
        val iv = Base64.getDecoder().decode(ivB64)
        val enc = Base64.getDecoder().decode(dataB64)
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        return String(c.doFinal(enc), Charsets.UTF_8)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun randomSalt(len: Int = 16): ByteArray = Random.nextBytes(len)
}
