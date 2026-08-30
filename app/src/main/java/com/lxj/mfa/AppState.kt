package com.lxj.mfa

/** 简单的会话级解锁状态：应用冷启动后需先解锁。 */
object AppState {
    var unlocked: Boolean = false
    /** 应用最后一次完全退到后台的时间戳（毫秒）；0 表示从未退到后台。 */
    var backgroundAt: Long = 0L
}
