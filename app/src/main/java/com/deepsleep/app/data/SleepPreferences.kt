package com.deepsleep.app.data

import android.content.Context
import android.content.SharedPreferences

object SleepPreferences {
    private const val PREF_NAME = "deepsleep_preferences"
    private const val KEY_SLEEP_LIST = "sleep_list_packages"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getSleepList(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_SLEEP_LIST, emptySet()) ?: emptySet()
    }

    fun saveSleepList(context: Context, packages: Set<String>) {
        getPrefs(context).edit().putStringSet(KEY_SLEEP_LIST, packages).apply()
    }

    fun isAppInSleepList(context: Context, packageName: String): Boolean {
        return getSleepList(context).contains(packageName)
    }

    fun addAppToSleepList(context: Context, packageName: String) {
        val current = getSleepList(context).toMutableSet()
        if (current.add(packageName)) {
            saveSleepList(context, current)
        }
    }

    fun removeAppFromSleepList(context: Context, packageName: String) {
        val current = getSleepList(context).toMutableSet()
        if (current.remove(packageName)) {
            saveSleepList(context, current)
        }
    }
}
