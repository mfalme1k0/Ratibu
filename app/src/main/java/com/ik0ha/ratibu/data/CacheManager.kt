package com.ik0ha.ratibu.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CacheManager(context: Context) {
    private val prefs = context.getSharedPreferences("ratibu_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveBookings(key: String, sessions: List<Session>) {
        val json = gson.toJson(sessions)
        prefs.edit().putString(key, json).apply()
    }

    fun getBookings(key: String): List<Session> {
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<Session>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveProfile(profile: ServiceProvider) {
        val json = gson.toJson(profile)
        prefs.edit().putString("cached_profile_${profile.uid}", json).apply()
    }

    fun getProfile(uid: String): ServiceProvider? {
        val json = prefs.getString("cached_profile_$uid", null) ?: return null
        return try {
            gson.fromJson(json, ServiceProvider::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveUserRole(uid: String, role: String) {
        prefs.edit().putString("user_role_$uid", role).apply()
    }

    fun getUserRole(uid: String): String? {
        return prefs.getString("user_role_$uid", null)
    }

    fun setThemePreference(isDarkMode: Boolean?) {
        if (isDarkMode == null) {
            prefs.edit().remove("theme_preference").apply()
        } else {
            prefs.edit().putBoolean("theme_preference", isDarkMode).apply()
        }
    }

    fun getThemePreference(): Boolean? {
        if (!prefs.contains("theme_preference")) return null
        return prefs.getBoolean("theme_preference", false)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
