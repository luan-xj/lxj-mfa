package com.lxj.mfa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一个 MFA 账号。secretEnc 为已加密的 Base32 密钥（本地加密存储）。
 */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val issuer: String = "",
    val label: String = "",
    val secretEnc: String = "",
    /** 类型：TOTP / HOTP / MOTP / STEAM，默认 TOTP */
    val type: String = "TOTP",
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
    /** 标签（自定义分组/备注），可搜索 */
    val tag: String = "",
    /** HOTP 计数器，每次使用自增 */
    val counter: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
