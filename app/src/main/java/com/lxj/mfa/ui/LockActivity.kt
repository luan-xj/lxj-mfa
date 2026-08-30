package com.lxj.mfa.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.lxj.mfa.AppState
import com.lxj.mfa.Prefs
import com.lxj.mfa.R
import com.lxj.mfa.databinding.ActivityLockBinding
import java.util.concurrent.Executor

/**
 * 启动拦截：首次设置主密码，之后支持「指纹」或「密码」解锁。
 */
class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private lateinit var executor: Executor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        executor = ContextCompat.getMainExecutor(this)

        val hasPw = Prefs.hasPassword(this)
        if (!hasPw) {
            binding.tvTitle.text = getString(R.string.create_password_title)
            binding.tvSubtitle.text = getString(R.string.create_password_hint)
            binding.tilConfirm.visibility = View.VISIBLE
            binding.btnFingerprint.visibility = View.GONE
            binding.btnUnlock.text = getString(R.string.save)
        }

        binding.btnUnlock.setOnClickListener { onUnlockClicked(hasPw) }
        binding.btnFingerprint.setOnClickListener { showBiometric() }

        if (hasPw && canUseBiometric()) showBiometric()
    }

    private fun canUseBiometric(): Boolean {
        return try {
            val bm = BiometricManager.from(this)
            bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    private fun onUnlockClicked(hasPw: Boolean) {
        val pw = binding.etPassword.text.toString()
        if (!hasPw) {
            val confirm = binding.etConfirm.text.toString()
            if (pw.length < 4) return showError("密码至少 4 位")
            if (pw != confirm) return showError(getString(R.string.password_mismatch))
            Prefs.setPassword(this, pw)
            Toast.makeText(this, R.string.password_set, Toast.LENGTH_SHORT).show()
            return finishUnlock()
        }
        if (Prefs.verifyPassword(this, pw)) finishUnlock()
        else showError(getString(R.string.wrong_password))
    }

    private fun showBiometric() {
        if (!canUseBiometric()) return
        try {
            val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = finishUnlock()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 用户点击「使用密码」，保持密码输入框可用即可
                }
            })
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.lock_title))
                .setSubtitle(getString(R.string.lock_subtitle))
                .setNegativeButtonText(getString(R.string.use_password))
                .build()
            prompt.authenticate(info)
        } catch (e: Exception) {
            // 生物识别不可用或初始化失败，退回密码解锁
        }
    }

    private fun finishUnlock() {
        AppState.unlocked = true
        finish()
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }
}
