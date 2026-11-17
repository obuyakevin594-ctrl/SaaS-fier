package com.deepsleep.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.deepsleep.app.databinding.ItemAppSleepBinding
import com.deepsleep.app.model.AppInfo

class AppAdapter(
    private var apps: List<AppInfo>,
    private val onAppToggled: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    inner class AppViewHolder(val binding: ItemAppSleepBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppSleepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.binding.tvAppName.text = app.name
        holder.binding.tvPackageName.text = app.packageName
        if (app.icon != null) {
            holder.binding.ivAppIcon.setImageDrawable(app.icon)
        } else {
            holder.binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        holder.binding.checkboxApp.isChecked = app.isSelected

        holder.itemView.setOnClickListener {
            app.isSelected = !app.isSelected
            holder.binding.checkboxApp.isChecked = app.isSelected
            onAppToggled(app, app.isSelected)
        }
    }

    override fun getItemCount(): Int = apps.size

    fun updateList(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
