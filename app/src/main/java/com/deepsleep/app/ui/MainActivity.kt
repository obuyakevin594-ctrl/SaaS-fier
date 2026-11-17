package com.deepsleep.app.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepsleep.app.R
import com.deepsleep.app.data.SleepPreferences
import com.deepsleep.app.databinding.ActivityMainBinding
import com.deepsleep.app.databinding.DialogProminentDisclosureBinding
import com.deepsleep.app.model.AppInfo
import com.deepsleep.app.service.HibernationManager
import com.deepsleep.app.utils.AccessibilityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val allApps = mutableListOf<AppInfo>()
    private lateinit var appAdapter: AppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView(); setupListeners(); loadInstalledApps(); setupCallbacks()
    }

    override fun onResume() { super.onResume(); updateStatus() }

    private fun updateStatus() {
        val en = AccessibilityUtils.isAccessibilityServiceEnabled(this)
        binding.tvStatusTitle.text = getString(if (en) R.string.status_active_title else R.string.status_inactive_title)
        binding.tvStatusSubtitle.text = getString(if (en) R.string.status_active_subtitle else R.string.status_inactive_subtitle)
        binding.btnEnableService.text = getString(if (en) R.string.btn_service_running else R.string.btn_enable_service)
        binding.btnEnableService.isEnabled = !en
    }

    private fun setupListeners() {
        binding.btnEnableService.setOnClickListener { showDisclosure() }
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { filter(s?.toString() ?: "") }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.btnSelectAll.setOnClickListener {
            val list = SleepPreferences.getSleepList(this).toMutableSet()
            allApps.forEach { it.isSelected = true; list.add(it.packageName) }
            SleepPreferences.saveSleepList(this, list); appAdapter.updateList(allApps); updateCount()
        }
        binding.btnDeselectAll.setOnClickListener {
            SleepPreferences.saveSleepList(this, emptySet())
            allApps.forEach { it.isSelected = false }; appAdapter.updateList(allApps); updateCount()
        }
        binding.btnHibernate.setOnClickListener {
            val pkgs = SleepPreferences.getSleepList(this).toList()
            if (pkgs.isEmpty()) { Toast.makeText(this, R.string.no_apps_selected_warning, Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (!AccessibilityUtils.isAccessibilityServiceEnabled(this)) { showDisclosure(); return@setOnClickListener }
            Toast.makeText(this, R.string.hibernation_started, Toast.LENGTH_SHORT).show()
            HibernationManager.startHibernation(this, pkgs)
        }
    }

    private fun showDisclosure() {
        val db = DialogProminentDisclosureBinding.inflate(layoutInflater)
        val d = AlertDialog.Builder(this, R.style.Theme_DeepSleep).setView(db.root).create()
        db.btnCancelDisclosure.setOnClickListener { d.dismiss() }
        db.btnAgreeDisclosure.setOnClickListener { d.dismiss(); startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        d.window?.setBackgroundDrawableResource(android.R.color.transparent); d.show()
    }

    private fun setupRecyclerView() {
        appAdapter = AppAdapter(emptyList()) { app, sel ->
            if (sel) SleepPreferences.addAppToSleepList(this, app.packageName)
            else SleepPreferences.removeAppFromSleepList(this, app.packageName)
            updateCount()
        }
        binding.recyclerViewApps.apply { layoutManager = LinearLayoutManager(this@MainActivity); adapter = appAdapter }
    }

    private fun loadInstalledApps() {
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager; val sleepList = SleepPreferences.getSleepList(this@MainActivity)
            val loaded = mutableListOf<AppInfo>()
            for (pkg in pm.getInstalledPackages(PackageManager.GET_META_DATA)) {
                val info = pkg.applicationInfo
                val isSys = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpd = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                if ((!isSys || isUpd) && info.packageName != packageName) {
                    val label = pm.getApplicationLabel(info).toString()
                    val icon = try { pm.getApplicationIcon(info) } catch (e: Exception) { null }
                    loaded.add(AppInfo(label, info.packageName, icon, sleepList.contains(info.packageName)))
                }
            }
            loaded.sortBy { it.name.lowercase() }
            withContext(Dispatchers.Main) { allApps.clear(); allApps.addAll(loaded); appAdapter.updateList(allApps); updateCount() }
        }
    }

    private fun filter(q: String) {
        val t = q.trim().lowercase()
        if (t.isEmpty()) { appAdapter.updateList(allApps); return }
        appAdapter.updateList(allApps.filter { it.name.lowercase().contains(t) || it.packageName.lowercase().contains(t) })
    }

    private fun updateCount() {
        val count = SleepPreferences.getSleepList(this).size
        binding.btnHibernate.text = if (count > 0) getString(R.string.btn_hibernate_apps_count, count) else getString(R.string.btn_hibernate_apps)
    }

    private fun setupCallbacks() {
        HibernationManager.onProgressCallback = { cur, tot, pkg ->
            val name = allApps.find { it.packageName == pkg }?.name ?: pkg
            runOnUiThread { Toast.makeText(this, getString(R.string.hibernation_progress, name, cur, tot), Toast.LENGTH_SHORT).show() }
        }
        HibernationManager.onCompleteCallback = { count ->
            runOnUiThread {
                Toast.makeText(this, getString(R.string.hibernation_complete, count), Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP) })
            }
        }
    }
}

