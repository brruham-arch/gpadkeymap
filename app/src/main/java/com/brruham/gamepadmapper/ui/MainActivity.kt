package com.brruham.gamepadmapper.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.brruham.gamepadmapper.accessibility.FloatingOverlayService
import com.brruham.gamepadmapper.accessibility.GamepadAccessibilityService
import com.brruham.gamepadmapper.databinding.ActivityMainBinding
import com.brruham.gamepadmapper.model.MappingProfile
import com.brruham.gamepadmapper.utils.ProfileRepository

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ProfileAdapter
    private val profiles = mutableListOf<MappingProfile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        loadProfiles()
        updateServiceStatus()
    }

    // ─── RecyclerView ─────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = ProfileAdapter(
            profiles,
            onEdit = { profile -> openEditor(profile) },
            onDelete = { profile -> confirmDelete(profile) },
            onActivate = { profile ->
                ProfileRepository.setActiveProfileId(this, profile.id)
                sendBroadcast(Intent(GamepadAccessibilityService.ACTION_RELOAD_PROFILE))
                Toast.makeText(this, "Active: ${profile.name}", Toast.LENGTH_SHORT).show()
                loadProfiles()
            }
        )
        binding.rvProfiles.layoutManager = LinearLayoutManager(this)
        binding.rvProfiles.adapter = adapter
    }

    private fun loadProfiles() {
        profiles.clear()
        profiles.addAll(ProfileRepository.loadAll(this))
        adapter.notifyDataSetChanged()

        val activeId = ProfileRepository.getActiveProfileId(this)
        adapter.setActiveId(activeId)
    }

    // ─── Buttons ──────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.fabAddProfile.setOnClickListener {
            val profile = MappingProfile(name = "Profile ${profiles.size + 1}")
            ProfileRepository.saveProfile(this, profile)
            openEditor(profile)
        }

        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } else {
                startService(Intent(this, FloatingOverlayService::class.java))
                Toast.makeText(this, "Overlay started", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── Editor ───────────────────────────────────────────────────────────────

    private fun openEditor(profile: MappingProfile) {
        val intent = Intent(this, MappingEditorActivity::class.java)
            .putExtra("profile_id", profile.id)
        startActivity(intent)
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    private fun confirmDelete(profile: MappingProfile) {
        AlertDialog.Builder(this)
            .setTitle("Delete Profile")
            .setMessage("Delete \"${profile.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                ProfileRepository.deleteProfile(this, profile.id)
                loadProfiles()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─── Permissions / status ─────────────────────────────────────────────────

    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            binding.tvOverlayStatus.text = "Overlay permission: NOT granted (tap to grant)"
        } else {
            binding.tvOverlayStatus.text = "Overlay permission: Granted ✓"
        }
    }

    private fun updateServiceStatus() {
        val active = GamepadAccessibilityService.instance != null
        binding.tvServiceStatus.text = if (active)
            "Accessibility Service: Active ✓"
        else
            "Accessibility Service: NOT active (tap button below)"
    }
}
