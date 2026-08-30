package com.lxj.mfa

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle

/**
 * 全局 Application：通过 Activity 生命周期计数判断应用是否「真正退到后台」，
 * 回到前台时按设定的超时判断是否重新锁定（App 内跳转不会误锁）。
 */
class App : Application() {

    private var activeCount = 0

    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
        if (CrashHandler.hasCrash(this)) {
            val intent = Intent(this, CrashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                activeCount++
                if (activeCount == 1) {
                    // 从后台返回前台：检查是否超时锁定
                    val timeoutMin = Prefs.getBgLockTimeout(activity)
                    val bg = AppState.backgroundAt
                    if (bg > 0 && timeoutMin >= 0 &&
                        System.currentTimeMillis() - bg > timeoutMin * 60_000L
                    ) {
                        AppState.unlocked = false
                    }
                }
            }

            override fun onActivityStopped(activity: Activity) {
                activeCount--
                if (activeCount == 0) {
                    // 应用完全退到后台，记录时刻
                    AppState.backgroundAt = System.currentTimeMillis()
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
