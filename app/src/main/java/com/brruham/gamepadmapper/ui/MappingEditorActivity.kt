package com.brruham.gamepadmapper.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.brruham.gamepadmapper.R
import com.brruham.gamepadmapper.databinding.ActivityMappingEditorBinding
import com.brruham.gamepadmapper.model.*
import com.brruham.gamepadmapper.utils.GamepadKeyHelper
import com.brruham.gamepadmapper.utils.ProfileRepository

class MappingEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMappingEditorBinding
    private lateinit var profile: MappingProfile
    private lateinit var mappingAdapter: MappingAdapter

    private var pendingMapping: ButtonMapping? = null
    private var isWaitingForButton = false

    companion object {
        private const val PICK_IMAGE_REQUEST = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMappingEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getStringExtra("profile_id") ?: return finish()
        profile = ProfileRepository.loadAll(this).firstOrNull { it.id == id } ?: return finish()

        setupUI()
        loadScreenshot()
    }

    // ─── UI Setup ─────────────────────────────────────────────────────────────

    private fun setupUI() {
        title = profile.name

        // Profile name edit
        binding.etProfileName.setText(profile.name)
        binding.etProfileName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                profile = profile.copy(name = binding.etProfileName.text.toString())
                saveProfile()
            }
        }

        // Screenshot picker
        binding.btnPickScreenshot.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Mappings list
        mappingAdapter = MappingAdapter(
            profile.mappings,
            onEdit = { m -> showMappingDialog(m) },
            onDelete = { m ->
                profile.mappings.remove(m)
                saveProfile()
                mappingAdapter.notifyDataSetChanged()
            }
        )
        binding.rvMappings.layoutManager = LinearLayoutManager(this)
        binding.rvMappings.adapter = mappingAdapter

        // Add mapping button
        binding.fabAddMapping.setOnClickListener {
            showMappingDialog(null)
        }

        // Canvas point picking
        binding.canvas.onPointPicked = { pts ->
            pendingMapping?.let { m ->
                // Update pending mapping coords based on action type
                when (m.actionType) {
                    ActionType.TAP, ActionType.HOLD, ActionType.MULTI_TAP ->
                        if (pts.isNotEmpty()) pendingMapping = m.copy(point = pts[0])
                    ActionType.SWIPE ->
                        if (pts.size >= 2) pendingMapping = m.copy(swipeFrom = pts[0], swipeTo = pts[1])
                    ActionType.GESTURE_PATH ->
                        pendingMapping = m.copy(gesturePath = pts)
                    ActionType.JOYSTICK_SWIPE ->
                        if (pts.isNotEmpty()) pendingMapping = m.copy(
                            joystickCenterX = pts[0].x, joystickCenterY = pts[0].y
                        )
                }
                binding.tvPickHint.text = "Tap canvas to set coordinates. Points: ${pts.size}"
            }
        }
    }

    // ─── Mapping dialog ───────────────────────────────────────────────────────

    private fun showMappingDialog(existing: ButtonMapping?) {
        val isNew = existing == null
        var mapping = existing ?: ButtonMapping()
        pendingMapping = mapping

        val view = layoutInflater.inflate(R.layout.dialog_mapping, null)
        val btnDetect     = view.findViewById<Button>(R.id.btnDetectButton)
        val tvButtonLabel = view.findViewById<TextView>(R.id.tvButtonLabel)
        val spinnerAction = view.findViewById<Spinner>(R.id.spinnerAction)
        val etDuration    = view.findViewById<EditText>(R.id.etDuration)
        val etInterval    = view.findViewById<EditText>(R.id.etInterval)
        val etRadius      = view.findViewById<EditText>(R.id.etRadius)
        val btnFinishPath = view.findViewById<Button>(R.id.btnFinishPath)
        val btnClearPts   = view.findViewById<Button>(R.id.btnClearPoints)

        tvButtonLabel.text = mapping.buttonLabel

        // Action type spinner
        val actionTypes = ActionType.values()
        spinnerAction.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            actionTypes.map { it.name })
        spinnerAction.setSelection(actionTypes.indexOf(mapping.actionType))

        spinnerAction.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val at = actionTypes[pos]
                pendingMapping = pendingMapping?.copy(actionType = at)
                // Update canvas mode
                binding.canvas.mode = when (at) {
                    ActionType.TAP, ActionType.HOLD, ActionType.MULTI_TAP,
                    ActionType.JOYSTICK_SWIPE -> ScreenshotCanvasView.PickMode.SINGLE_POINT
                    ActionType.SWIPE -> ScreenshotCanvasView.PickMode.TWO_POINTS
                    ActionType.GESTURE_PATH -> ScreenshotCanvasView.PickMode.PATH
                }
                binding.canvas.clearPoints()
                // Show/hide extra fields
                etDuration.visibility  = if (at == ActionType.SWIPE) View.VISIBLE else View.GONE
                etInterval.visibility  = if (at == ActionType.MULTI_TAP) View.VISIBLE else View.GONE
                etRadius.visibility    = if (at == ActionType.JOYSTICK_SWIPE) View.VISIBLE else View.GONE
                btnFinishPath.visibility = if (at == ActionType.GESTURE_PATH) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        etDuration.setText(mapping.swipeDurationMs.toString())
        etInterval.setText(mapping.multiTapIntervalMs.toString())
        etRadius.setText(mapping.joystickRadius.toString())

        btnFinishPath.setOnClickListener { binding.canvas.finishPath() }
        btnClearPts.setOnClickListener { binding.canvas.clearPoints() }

        // Detect gamepad button
        btnDetect.setOnClickListener {
            isWaitingForButton = true
            tvButtonLabel.text = "Press a gamepad button..."
            btnDetect.isEnabled = false
        }

        AlertDialog.Builder(this)
            .setTitle(if (isNew) "Add Mapping" else "Edit Mapping")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val pm = pendingMapping ?: return@setPositiveButton
                val final = pm.copy(
                    swipeDurationMs    = etDuration.text.toString().toLongOrNull() ?: 200,
                    multiTapIntervalMs = etInterval.text.toString().toLongOrNull() ?: 100,
                    joystickRadius     = etRadius.text.toString().toFloatOrNull() ?: 100f
                )
                if (isNew) profile.mappings.add(final)
                else {
                    val idx = profile.mappings.indexOfFirst { it.id == final.id }
                    if (idx >= 0) profile.mappings[idx] = final
                }
                saveProfile()
                mappingAdapter.notifyDataSetChanged()
                pendingMapping = null
                isWaitingForButton = false
            }
            .setNegativeButton("Cancel") { _, _ ->
                pendingMapping = null
                isWaitingForButton = false
                binding.canvas.clearPoints()
            }
            .show()
    }

    // ─── Gamepad button detection ─────────────────────────────────────────────

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isWaitingForButton && event != null && GamepadKeyHelper.isGamepadKey(event)) {
            val label = GamepadKeyHelper.labelForKeycode(keyCode)
            pendingMapping = pendingMapping?.copy(buttonCode = keyCode, buttonLabel = label)
            isWaitingForButton = false
            Toast.makeText(this, "Button detected: $label", Toast.LENGTH_SHORT).show()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // ─── Screenshot ───────────────────────────────────────────────────────────

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                binding.canvas.bitmap = bmp
                profile = profile.copy(
                    screenshotWidth = bmp.width,
                    screenshotHeight = bmp.height,
                    screenshotPath = uri.toString()
                )
                saveProfile()
            }
        }
    }

    private fun loadScreenshot() {
        if (profile.screenshotPath.isNotEmpty()) {
            try {
                val uri = android.net.Uri.parse(profile.screenshotPath)
                contentResolver.openInputStream(uri)?.use { stream ->
                    binding.canvas.bitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) { /* screenshot unavailable */ }
        }
    }

    // ─── Save ─────────────────────────────────────────────────────────────────

    private fun saveProfile() {
        ProfileRepository.saveProfile(this, profile)
    }
}
