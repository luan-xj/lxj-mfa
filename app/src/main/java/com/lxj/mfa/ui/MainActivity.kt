package com.lxj.mfa.ui

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.integration.android.IntentIntegrator
import com.lxj.mfa.AppState
import com.lxj.mfa.Crypto
import com.lxj.mfa.Prefs
import com.lxj.mfa.R
import com.lxj.mfa.data.Account
import com.lxj.mfa.data.AppDatabase
import com.lxj.mfa.databinding.ActivityMainBinding
import com.lxj.mfa.databinding.ItemAccountBinding
import com.lxj.mfa.totp.Otp
import com.lxj.mfa.totp.OtpAuth
import com.lxj.mfa.totp.OtpInfo
import com.lxj.mfa.Updater
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AccountAdapter
    private lateinit var updater: Updater
    private val handler = Handler(Looper.getMainLooper())
    private companion object { var sCheckedUpdate = false }
    private val ticker = object : Runnable {
        override fun run() {
            adapter.tick()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
        updater = Updater(this)

        adapter = AccountAdapter(
            onCopy = { copyCode(it) },
            onDelete = { confirmDelete(it) },
            onEdit = { showAddDialog(edit = it) }
        )
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        binding.fab.setOnClickListener { showAddDialog() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.setQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        AppDatabase.get(this).dao().observe().observe(this) { list ->
            adapter.submit(list)
            binding.empty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (!AppState.unlocked) {
            startActivity(Intent(this, LockActivity::class.java))
            return
        }
        handler.post(ticker)
        if (!sCheckedUpdate) {
            sCheckedUpdate = true
            updater.check(auto = true)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
        R.id.action_sync -> { SyncRunner.push(this) { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }; true }
        R.id.action_lock -> { lockNow(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun lockNow() {
        AppState.unlocked = false
        startActivity(Intent(this, LockActivity::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val info = OtpAuth.parse(result.contents)
            showAddDialog(scan = info)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun copyCode(a: Account) {
        val secret = runCatching { Crypto.decrypt(a.secretEnc) }.getOrElse { return }
        val code = runCatching { Otp.code(a, secret) }.getOrElse { return }
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("mfa", code))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        // HOTP：每次使用（复制）后计数器自增
        if (a.type.uppercase() == "HOTP") {
            lifecycleScope.launch {
                AppDatabase.get(this@MainActivity).dao().upsert(a.copy(counter = a.counter + 1))
            }
        }
    }

    private fun confirmDelete(a: Account) {
        AlertDialog.Builder(this)
            .setMessage(R.string.delete_account)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch { AppDatabase.get(this@MainActivity).dao().delete(a) }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddDialog(edit: Account? = null, scan: OtpInfo? = null) {
        val dialog = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.dialog_add_account, null)
        dialog.setView(view)

        val etIssuer = view.findViewById<TextInputEditText>(R.id.et_issuer)
        val etLabel = view.findViewById<TextInputEditText>(R.id.et_label)
        val etTag = view.findViewById<TextInputEditText>(R.id.et_tag)
        val etSecret = view.findViewById<TextInputEditText>(R.id.et_secret)
        val spType = view.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.sp_type)
        val spAlgo = view.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.sp_algorithm)
        val layoutCounter = view.findViewById<android.view.View>(R.id.layout_counter)
        val etCounter = view.findViewById<TextInputEditText>(R.id.et_counter)
        val tvMotpHint = view.findViewById<android.widget.TextView>(R.id.tv_motp_hint)
        val layoutPreview = view.findViewById<android.view.View>(R.id.layout_preview)
        val tvPreviewCur = view.findViewById<android.widget.TextView>(R.id.tv_preview_cur)
        val tvPreviewNext = view.findViewById<android.widget.TextView>(R.id.tv_preview_next)
        val tvPreviewRem = view.findViewById<android.widget.TextView>(R.id.tv_preview_rem)
        view.findViewById<android.widget.TextView>(R.id.tv_dialog_title).text =
            if (edit != null) getString(R.string.edit_account) else getString(R.string.add_account)

        val type = edit?.type ?: scan?.let { "TOTP" } ?: "TOTP"
        val algorithm = edit?.algorithm ?: scan?.algorithm ?: "SHA1"
        val tag = edit?.tag ?: ""
        val secret = edit?.let { runCatching { Crypto.decrypt(it.secretEnc) }.getOrDefault("") } ?: scan?.secret ?: ""
        val counter = edit?.counter ?: 0

        etIssuer.setText(edit?.issuer ?: scan?.issuer ?: "")
        etLabel.setText(edit?.label ?: scan?.label ?: "")
        etTag.setText(tag)
        etSecret.setText(secret)
        etCounter.setText(counter.toString())

        spType.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, resources.getStringArray(R.array.account_types)))
        spAlgo.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, resources.getStringArray(R.array.algorithms)))
        spType.setText(type, false)
        spAlgo.setText(algorithm, false)

        fun refreshTypeUi() {
            val sel = spType.text.toString()
            layoutCounter.visibility = if (sel == "HOTP") View.VISIBLE else View.GONE
            tvMotpHint.visibility = if (sel == "MOTP") View.VISIBLE else View.GONE
        }

        // 实时预览当前/下一验证码
        fun updatePreview() {
            val s = etSecret.text.toString().trim().replace(" ", "")
            if (s.isEmpty()) {
                layoutPreview.visibility = View.GONE
                return
            }
            val t = spType.text.toString().ifEmpty { "TOTP" }
            val algo = spAlgo.text.toString().ifEmpty { "SHA1" }
            val tmp = Account(
                type = t,
                algorithm = if (t == "STEAM" || t == "MOTP") "SHA1" else algo,
                period = 30,
                digits = if (t == "STEAM") 5 else 6
            )
            val isTime = Otp.isTimeBased(tmp)
            layoutPreview.visibility = View.VISIBLE
            try {
                val cur = Otp.codeAt(tmp, s)
                tvPreviewCur.text = formatCode(cur)
                if (isTime) {
                    val period = Otp.effectivePeriod(tmp)
                    val next = Otp.codeAt(tmp, s, System.currentTimeMillis() + period * 1000L)
                    tvPreviewNext.text = getString(R.string.preview_next, formatCode(next))
                    tvPreviewRem.text = "剩余 ${Otp.remaining(period)}s"
                } else {
                    tvPreviewNext.text = "计数器型（无下一码）"
                    tvPreviewRem.text = ""
                }
            } catch (e: Exception) {
                tvPreviewCur.text = "密钥格式有误"
                tvPreviewNext.text = ""
                tvPreviewRem.text = ""
            }
        }

        refreshTypeUi()
        spType.setOnItemClickListener { _, _, _, _ -> refreshTypeUi(); updatePreview() }
        spAlgo.setOnItemClickListener { _, _, _, _ -> updatePreview() }
        etSecret.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = updatePreview()
        })

        val previewHandler = Handler(Looper.getMainLooper())
        val previewTicker = object : Runnable {
            override fun run() {
                updatePreview()
                previewHandler.postDelayed(this, 1000)
            }
        }
        dialog.setOnDismissListener { previewHandler.removeCallbacks(previewTicker) }

        view.findViewById<View>(R.id.btn_scan).setOnClickListener {
            IntentIntegrator(this)
                .setCaptureActivity(com.journeyapps.barcodescanner.CaptureActivity::class.java)
                .setOrientationLocked(true)
                .setPrompt("扫描 MFA 二维码")
                .initiateScan()
        }
        view.findViewById<View>(R.id.btn_cancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            val s = etSecret.text.toString().trim().replace(" ", "")
            if (s.isEmpty()) {
                etSecret.error = "请输入或扫码密钥"
                return@setOnClickListener
            }
            val selectedType = spType.text.toString().ifEmpty { "TOTP" }
            val selectedAlgo = spAlgo.text.toString().ifEmpty { "SHA1" }
            val account = Account(
                id = edit?.id ?: 0,
                issuer = etIssuer.text.toString().trim(),
                label = etLabel.text.toString().trim(),
                secretEnc = Crypto.encrypt(s),
                type = selectedType,
                algorithm = if (selectedType == "STEAM" || selectedType == "MOTP") "SHA1" else selectedAlgo,
                digits = if (selectedType == "STEAM") 5 else 6,
                period = 30,
                tag = etTag.text.toString().trim(),
                counter = if (selectedType == "HOTP") etCounter.text.toString().toIntOrNull() ?: 0 else 0
            )
            lifecycleScope.launch { AppDatabase.get(this@MainActivity).dao().upsert(account) }
            dialog.dismiss()
        }
        dialog.show()
        previewHandler.post(previewTicker)
    }

    // ---------------- Adapter ----------------
    private fun formatCode(code: String): String {
        if (code.length < 6) return code
        val mid = code.length / 2
        return code.substring(0, mid) + " " + code.substring(mid)
    }

    private fun metaText(a: Account): String = when (a.type.uppercase()) {
        "HOTP" -> "HOTP · 计数器 ${a.counter}"
        "STEAM" -> "STEAM"
        "MOTP" -> "MOTP"
        else -> "TOTP · ${a.algorithm}"
    }

    inner class AccountAdapter(
        private val onCopy: (Account) -> Unit,
        private val onDelete: (Account) -> Unit,
        private val onEdit: (Account) -> Unit
    ) : RecyclerView.Adapter<AccountAdapter.VH>() {

        private var all: List<Account> = emptyList()
        private var query: String = ""
        private var items: List<Account> = emptyList()

        fun submit(list: List<Account>) {
            all = list
            applyFilter()
        }

        fun setQuery(q: String) {
            query = q.trim().lowercase()
            applyFilter()
        }

        private fun applyFilter() {
            val filtered = if (query.isEmpty()) {
                all
            } else {
                all.filter { a ->
                    a.issuer.lowercase().contains(query) ||
                    a.label.lowercase().contains(query) ||
                    a.tag.lowercase().contains(query) ||
                    a.type.lowercase().contains(query) ||
                    a.algorithm.lowercase().contains(query)
                }
            }
            items = filtered
            notifyDataSetChanged()
        }

        fun current() = items

        inner class VH(val b: ItemAccountBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val a = items[pos]
            h.b.tvIssuer.text = a.issuer.ifEmpty { a.label.ifEmpty { "账号" } }
            h.b.tvLabel.text = a.label
            h.b.tvLabel.visibility = if (a.label.isEmpty()) View.GONE else View.VISIBLE

            h.b.tvTag.text = a.tag
            h.b.tvTag.visibility = if (a.tag.isEmpty()) View.GONE else View.VISIBLE

            val secret = runCatching { Crypto.decrypt(a.secretEnc) }.getOrElse { "" }
            h.b.tvCode.text = formatCode(
                runCatching { Otp.code(a, secret) }.getOrElse { "------" }
            )

            h.b.tvMeta.text = metaText(a)

            val period = Otp.effectivePeriod(a)
            if (period > 0) {
                h.b.pbProgress.visibility = View.VISIBLE
                val rem = Otp.remaining(period)
                h.b.pbProgress.max = period
                h.b.pbProgress.progress = period - rem
                val next = runCatching {
                    Otp.codeAt(a, secret, System.currentTimeMillis() + period * 1000L)
                }.getOrElse { "" }
                h.b.tvNext.text = "next " + formatCode(next)
                h.b.tvNext.visibility = View.VISIBLE
            } else {
                h.b.pbProgress.visibility = View.GONE
                h.b.tvNext.visibility = View.GONE
            }

            h.b.root.setOnClickListener { onCopy(a) }
            h.b.btnEdit.setOnClickListener { onEdit(a) }
            h.b.btnDelete.setOnClickListener { onDelete(a) }
        }

        // 每秒只更新时间型验证码文本与进度条，避免整卡片重绘导致的闪烁
        fun tick() {
            val list = this@MainActivity.binding.list
            for (i in items.indices) {
                val a = items[i]
                if (!Otp.isTimeBased(a)) continue
                val h = list.findViewHolderForAdapterPosition(i) as? VH ?: continue
                val secret = runCatching { Crypto.decrypt(a.secretEnc) }.getOrElse { "" }
                h.b.tvCode.text = formatCode(
                    runCatching { Otp.code(a, secret) }.getOrElse { "------" }
                )
                val period = Otp.effectivePeriod(a)
                val rem = Otp.remaining(period)
                h.b.pbProgress.progress = period - rem
                val next = runCatching {
                    Otp.codeAt(a, secret, System.currentTimeMillis() + period * 1000L)
                }.getOrElse { "" }
                h.b.tvNext.text = "next " + formatCode(next)
            }
        }
    }
}
