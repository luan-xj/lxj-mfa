package com.lxj.mfa.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.lxj.mfa.Prefs
import com.lxj.mfa.R
import com.lxj.mfa.BuildConfig
import com.lxj.mfa.Updater
import com.lxj.mfa.CrashHandler
import com.lxj.mfa.CrashActivity
import com.lxj.mfa.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var updater: Updater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        updater = Updater(this)

        binding.etRepo.setText(Prefs.getRepo(this))
        binding.etBranch.setText(Prefs.getBranch(this))
        binding.etToken.setText(Prefs.getToken(this))
        binding.swEncrypt.isChecked = Prefs.getEncryptBackup(this)

        setupBgLockSpinner()

        binding.etRepo.setOnFocusChangeListener { _, _ -> saveGitConfig() }
        binding.etBranch.setOnFocusChangeListener { _, _ -> saveGitConfig() }
        binding.etToken.setOnFocusChangeListener { _, _ -> saveGitConfig() }
        binding.swEncrypt.setOnCheckedChangeListener { _, _ -> saveGitConfig() }

        binding.btnSync.setOnClickListener {
            SyncRunner.push(this) { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }
        binding.btnPull.setOnClickListener {
            SyncRunner.pull(this) { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }
        binding.btnChangePwd.setOnClickListener { showChangePassword() }
        binding.btnSyncPwd.setOnClickListener { showSetSyncPassword() }
        binding.btnHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
        binding.btnCheckUpdate.setOnClickListener {
            updater.check(auto = false)
        }
        binding.btnViewLog.setOnClickListener {
            startActivity(Intent(this, CrashActivity::class.java))
        }
        if (CrashHandler.hasCrash(this)) {
            binding.btnViewLog.text = getString(R.string.view_crash_log) + "（有记录）"
        }

        binding.tvVersion.text = getString(R.string.version_label, BuildConfig.VERSION_NAME)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun saveGitConfig() {
        Prefs.setRepo(this, binding.etRepo.text.toString().trim())
        Prefs.setBranch(this, binding.etBranch.text.toString().trim().ifEmpty { "main" })
        Prefs.setToken(this, binding.etToken.text.toString().trim())
        Prefs.setEncryptBackup(this, binding.swEncrypt.isChecked)
    }

    // 下拉选项对应分钟数：-1 从不，0 立即，其余为分钟
    private val bgLockMinutes = intArrayOf(0, 1, 5, 15, -1)

    private fun setupBgLockSpinner() {
        // 用清晰的卡片点击选择，替代不明显的下拉框
        val options = resources.getStringArray(R.array.bg_lock_options)
        val current = Prefs.getBgLockTimeout(this)
        val pos = (0 until bgLockMinutes.size).firstOrNull { bgLockMinutes[it] == current } ?: 0
        binding.tvBgLockValue.text = options[pos]

        binding.cardBgLock.setOnClickListener {
            var checked = (0 until bgLockMinutes.size).firstOrNull { bgLockMinutes[it] == Prefs.getBgLockTimeout(this) } ?: 0
            AlertDialog.Builder(this)
                .setTitle(R.string.lock_after_title)
                .setSingleChoiceItems(options, checked) { _, which -> checked = which }
                .setPositiveButton(R.string.save) { _, _ ->
                    Prefs.setBgLockTimeout(this, bgLockMinutes[checked])
                    binding.tvBgLockValue.text = options[checked]
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun showChangePassword() {
        val passwordType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val etOld = TextInputEditText(this).apply { hint = "原密码"; inputType = passwordType }
        val etNew = TextInputEditText(this).apply { hint = "新密码（至少 4 位）"; inputType = passwordType }
        val etNew2 = TextInputEditText(this).apply { hint = "确认新密码"; inputType = passwordType }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 10)
            addView(etOld); addView(etNew); addView(etNew2)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.change_password)
            .setView(layout)
            .setPositiveButton(R.string.save) { _, _ ->
                val old = etOld.text.toString()
                val new = etNew.text.toString()
                val new2 = etNew2.text.toString()
                when {
                    !Prefs.verifyPassword(this, old) ->
                        Toast.makeText(this, "原密码错误", Toast.LENGTH_SHORT).show()
                    new.length < 4 ->
                        Toast.makeText(this, "新密码至少 4 位", Toast.LENGTH_SHORT).show()
                    new != new2 ->
                        Toast.makeText(this, R.string.password_mismatch, Toast.LENGTH_SHORT).show()
                    else -> {
                        Prefs.setPassword(this, new)
                        Toast.makeText(this, R.string.password_set, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSetSyncPassword() {
        val passwordType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val etNew = TextInputEditText(this).apply { hint = "同步密码（至少 4 位）"; inputType = passwordType }
        val etNew2 = TextInputEditText(this).apply { hint = "确认同步密码"; inputType = passwordType }
        val note = android.widget.TextView(this).apply {
            text = getString(R.string.sync_password_hint)
            setTextColor(getColor(R.color.text_secondary))
            textSize = 12f
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 10)
            addView(etNew); addView(etNew2); addView(note)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.set_sync_password)
            .setView(layout)
            .setPositiveButton(R.string.save) { _, _ ->
                val new = etNew.text.toString()
                val new2 = etNew2.text.toString()
                when {
                    new.length < 4 ->
                        Toast.makeText(this, "同步密码至少 4 位", Toast.LENGTH_SHORT).show()
                    new != new2 ->
                        Toast.makeText(this, R.string.password_mismatch, Toast.LENGTH_SHORT).show()
                    else -> {
                        Prefs.setSyncPassword(this, new)
                        Toast.makeText(this, R.string.sync_password_set, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
