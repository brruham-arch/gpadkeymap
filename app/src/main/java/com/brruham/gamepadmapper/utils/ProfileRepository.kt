package com.brruham.gamepadmapper.utils

import android.content.Context
import com.brruham.gamepadmapper.model.MappingProfile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object ProfileRepository {

    private const val PROFILES_FILE = "profiles.json"
    private val gson = Gson()

    private fun getFile(context: Context): File =
        File(context.filesDir, PROFILES_FILE)

    fun loadAll(context: Context): MutableList<MappingProfile> {
        val file = getFile(context)
        if (!file.exists()) return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<MappingProfile>>() {}.type
            gson.fromJson(file.readText(), type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun saveAll(context: Context, profiles: List<MappingProfile>) {
        getFile(context).writeText(gson.toJson(profiles))
    }

    fun saveProfile(context: Context, profile: MappingProfile) {
        val all = loadAll(context)
        val idx = all.indexOfFirst { it.id == profile.id }
        if (idx >= 0) all[idx] = profile else all.add(profile)
        saveAll(context, all)
    }

    fun deleteProfile(context: Context, profileId: String) {
        val all = loadAll(context).filter { it.id != profileId }
        saveAll(context, all)
    }

    fun getActiveProfileId(context: Context): String? {
        val prefs = context.getSharedPreferences("gm_prefs", Context.MODE_PRIVATE)
        return prefs.getString("active_profile_id", null)
    }

    fun setActiveProfileId(context: Context, id: String?) {
        context.getSharedPreferences("gm_prefs", Context.MODE_PRIVATE)
            .edit().putString("active_profile_id", id).apply()
    }

    fun getActiveProfile(context: Context): MappingProfile? {
        val id = getActiveProfileId(context) ?: return null
        return loadAll(context).firstOrNull { it.id == id }
    }
}
