package com.pomodoro.app.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.pomodoro.app.data.models.AmbientSound
import com.pomodoro.app.databinding.FragmentSettingsBinding
import com.pomodoro.app.ui.SharedViewModel
import java.io.InputStream

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SharedViewModel

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { handleImageSelected(it) } }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pickImageLauncher.launch("image/*")
        else Toast.makeText(requireContext(), "Permission needed to pick images", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.settings.observe(viewLifecycleOwner) { settings ->

            binding.switchAutoResume.isChecked = settings.autoStartNext
            binding.switchNotifications.isChecked = settings.notifications
            binding.tvFocusDuration.text = "${settings.focusDuration}"
            binding.tvShortBreak.text = "${settings.shortBreakDuration}"
            binding.tvLongBreak.text = "${settings.longBreakDuration}"
            updateSoundSelection(settings.ambientSound)

            if (settings.backgroundImageUri != null) {
                Glide.with(this).load(settings.backgroundImageUri)
                    .centerCrop().into(binding.ivBgPreview)
                binding.ivBgPreview.visibility = View.VISIBLE
                binding.btnClearBg.visibility = View.VISIBLE
                binding.tvBgHint.text = "Tap to change background"
            } else {
                binding.ivBgPreview.visibility = View.GONE
                binding.btnClearBg.visibility = View.GONE
                binding.tvBgHint.text = "Choose a photo to set as background"
            }
        }

        viewModel.themeColors.observe(viewLifecycleOwner) { colors ->
            applyColors(colors.primary, colors.accent, colors.surface)
        }
    }

    private fun setupListeners() {
        binding.switchAutoResume.setOnCheckedChangeListener { _, checked ->
            viewModel.updateSettings { copy(autoStartNext = checked) }
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, checked ->
            viewModel.updateSettings { copy(notifications = checked) }
        }

        // Focus duration
        binding.btnFocusPlus.setOnClickListener {
            val v = (viewModel.settings.value?.focusDuration ?: 25) + 1
            viewModel.updateSettings { copy(focusDuration = v) }
        }
        binding.btnFocusMinus.setOnClickListener {
            val v = maxOf(1, (viewModel.settings.value?.focusDuration ?: 25) - 1)
            viewModel.updateSettings { copy(focusDuration = v) }
        }

        // Short break
        binding.btnShortPlus.setOnClickListener {
            val v = (viewModel.settings.value?.shortBreakDuration ?: 5) + 1
            viewModel.updateSettings { copy(shortBreakDuration = v) }
        }
        binding.btnShortMinus.setOnClickListener {
            val v = maxOf(1, (viewModel.settings.value?.shortBreakDuration ?: 5) - 1)
            viewModel.updateSettings { copy(shortBreakDuration = v) }
        }

        // Long break
        binding.btnLongPlus.setOnClickListener {
            val v = (viewModel.settings.value?.longBreakDuration ?: 15) + 1
            viewModel.updateSettings { copy(longBreakDuration = v) }
        }
        binding.btnLongMinus.setOnClickListener {
            val v = maxOf(1, (viewModel.settings.value?.longBreakDuration ?: 15) - 1)
            viewModel.updateSettings { copy(longBreakDuration = v) }
        }

        // Ambient sound buttons
        val soundMap = mapOf(
            binding.btnSoundNone to AmbientSound.NONE,
            binding.btnSoundForest to AmbientSound.FOREST,
            binding.btnSoundCafe to AmbientSound.CAFE,
            binding.btnSoundRain to AmbientSound.RAIN,
            binding.btnSoundOcean to AmbientSound.OCEAN,
            binding.btnSoundFire to AmbientSound.FIREPLACE
        )
        soundMap.forEach { (btn, sound) ->
            btn.setOnClickListener { viewModel.updateSettings { copy(ambientSound = sound) } }
        }

        // Background image picker
        binding.bgPickerCard.setOnClickListener { requestImagePermissionAndPick() }
        binding.btnClearBg.setOnClickListener { viewModel.clearBackgroundImage() }
    }

    private fun requestImagePermissionAndPick() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun handleImageSelected(uri: Uri) {
        try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                viewModel.extractColorsFromBitmap(bitmap, uri)
                Toast.makeText(requireContext(), "Colors extracted from image!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSoundSelection(current: AmbientSound) {
        val colors = viewModel.themeColors.value
        val primary = colors?.primary ?: 0xFFE7992C.toInt()
        val surface = colors?.surface ?: 0xFFFEE49A.toInt()
        val accent = colors?.accent ?: 0xFF483320.toInt()

        val soundMap = mapOf(
            binding.btnSoundNone to AmbientSound.NONE,
            binding.btnSoundForest to AmbientSound.FOREST,
            binding.btnSoundCafe to AmbientSound.CAFE,
            binding.btnSoundRain to AmbientSound.RAIN,
            binding.btnSoundOcean to AmbientSound.OCEAN,
            binding.btnSoundFire to AmbientSound.FIREPLACE
        )

        soundMap.forEach { (btn, sound) ->
            if (sound == current) {
                btn.setBackgroundColor(primary)
                btn.setTextColor(android.graphics.Color.WHITE)
            } else {
                btn.setBackgroundColor(surface)
                btn.setTextColor(accent)
            }
        }
    }

    private fun applyColors(primary: Int, accent: Int, surface: Int) {
        val accentList = android.content.res.ColorStateList.valueOf(accent)
        val surfaceList = android.content.res.ColorStateList.valueOf(surface)
        binding.apply {
            tvTitle.setTextColor(accent)

            switchAutoResume.thumbTintList = accentList
            switchNotifications.thumbTintList = accentList
            btnFocusPlus.imageTintList = accentList
            btnFocusMinus.imageTintList = accentList
            btnShortPlus.imageTintList = accentList
            btnShortMinus.imageTintList = accentList
            btnLongPlus.imageTintList = accentList
            btnLongMinus.imageTintList = accentList
            tvFocusDuration.setTextColor(accent)
            tvShortBreak.setTextColor(accent)
            tvLongBreak.setTextColor(accent)
            durationFocusCard.setCardBackgroundColor(surface)
            durationShortCard.setCardBackgroundColor(surface)
            durationLongCard.setCardBackgroundColor(surface)
            bgPickerCard.setCardBackgroundColor(surface)
        }
        updateSoundSelection(viewModel.settings.value?.ambientSound ?: AmbientSound.NONE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}