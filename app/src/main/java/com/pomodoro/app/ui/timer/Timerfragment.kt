package com.pomodoro.app.ui.timer

import android.content.*
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.pomodoro.app.R
import com.pomodoro.app.data.models.AmbientSound
import com.pomodoro.app.data.models.TimerMode
import com.pomodoro.app.databinding.FragmentTimerBinding
import com.pomodoro.app.service.TimerService
import com.pomodoro.app.ui.SharedViewModel

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SharedViewModel
    private var timerService: TimerService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            timerService = (binder as TimerService.TimerBinder).getService()
            serviceBound = true
            timerService?.setCallbacks(
                onTick = { timeLeft ->
                    activity?.runOnUiThread {
                        viewModel.updateTimerState { copy(timeLeft = timeLeft) }
                        updateTimerDisplay(timeLeft)
                    }
                },
                onComplete = {
                    activity?.runOnUiThread { onTimerComplete() }
                }
            )
            syncServiceState()
        }
        override fun onServiceDisconnected(name: ComponentName?) { serviceBound = false }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        bindTimerService()
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.timerState.observe(viewLifecycleOwner) { state ->
            updateTimerDisplay(state.timeLeft)
            updateModeChip(state.mode)
            updatePlayPauseButton(state.isRunning)
        }

        viewModel.activeTask.observe(viewLifecycleOwner) { task ->
            binding.tvActiveTask.text = task?.name ?: ""
            binding.tvActiveTask.visibility = if (task != null) View.VISIBLE else View.GONE
        }

        viewModel.settings.observe(viewLifecycleOwner) { settings ->
            loadBackground(settings.backgroundImageUri)
        }

        viewModel.themeColors.observe(viewLifecycleOwner) { colors ->
            applyColors(colors.primary, colors.accent, colors.surface)
        }
    }

    private fun setupClickListeners() {
        binding.btnPlayPause.setOnClickListener { togglePlayPause() }
        binding.btnSkip.setOnClickListener { skipToNext() }
        binding.btnReset.setOnClickListener { resetTimer() }
    }

    private fun togglePlayPause() {
        val state = viewModel.timerState.value ?: return
        val settings = viewModel.settings.value ?: return

        if (state.isRunning) {
            timerService?.pauseTimer()
            viewModel.updateTimerState { copy(isRunning = false) }
        } else {
            if (!serviceBound) bindTimerService()
            timerService?.startTimer(state.timeLeft, settings.ambientSound)
            viewModel.updateTimerState { copy(isRunning = true) }
        }
    }

    private fun skipToNext() {
        timerService?.stopTimer()
        viewModel.updateTimerState { copy(isRunning = false) }
        val state = viewModel.timerState.value ?: return
        val newMode: TimerMode
        val newCycle: Int

        if (state.mode == TimerMode.FOCUS) {
            val cycle = state.cycleCount + 1
            newCycle = cycle
            newMode = if (cycle % 4 == 0) TimerMode.LONG_BREAK else TimerMode.SHORT_BREAK
        } else {
            newMode = TimerMode.FOCUS
            newCycle = state.cycleCount
        }

        val newTime = viewModel.getDuration(newMode)
        viewModel.updateTimerState { copy(mode = newMode, timeLeft = newTime, cycleCount = newCycle, isRunning = false) }

        val settings = viewModel.settings.value ?: return
        if (settings.autoStartNext) {
            timerService?.startTimer(newTime, settings.ambientSound)
            viewModel.updateTimerState { copy(isRunning = true) }
        }
    }

    private fun resetTimer() {
        timerService?.stopTimer()
        val mode = viewModel.timerState.value?.mode ?: TimerMode.FOCUS
        val time = viewModel.getDuration(mode)
        viewModel.updateTimerState { copy(timeLeft = time, isRunning = false) }
    }

    private fun onTimerComplete() {
        val state = viewModel.timerState.value ?: return
        val settings = viewModel.settings.value ?: return

        if (state.mode == TimerMode.FOCUS) {
            val taskName = viewModel.activeTask.value?.name ?: "No Task"
            viewModel.addSession(taskName, settings.focusDuration)
            viewModel.activeTask.value?.let { task ->
                viewModel.updateTaskSessions(task, task.sessions + 1)
            }
        }
        skipToNext()
    }

    private fun updateTimerDisplay(seconds: Int) {
        val mins = seconds / 60
        val secs = seconds % 60
        binding.tvMinutes.text = String.format("%02d", mins)
        binding.tvSeconds.text = String.format("%02d", secs)

        val total = viewModel.getDuration(viewModel.timerState.value?.mode ?: TimerMode.FOCUS)
        val progress = if (total > 0) ((total - seconds).toFloat() / total * 100).toInt() else 0
        binding.progressTimer.progress = progress
    }

    private fun updateModeChip(mode: TimerMode) {
        val (text, icon) = when (mode) {
            TimerMode.FOCUS -> "Focus" to R.drawable.ic_brain
            TimerMode.SHORT_BREAK -> "Short Break" to R.drawable.ic_coffee
            TimerMode.LONG_BREAK -> "Long Break" to R.drawable.ic_coffee
        }
        binding.tvMode.text = text
        binding.ivModeIcon.setImageResource(icon)
    }

    private fun updatePlayPauseButton(isRunning: Boolean) {
        binding.btnPlayPause.icon = ContextCompat.getDrawable(
            requireContext(),
            if (isRunning) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun loadBackground(uri: String?) {
        if (uri != null) {
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(binding.ivBackground)
            binding.ivBackground.visibility = View.VISIBLE
            binding.ivBackground.alpha = 0.35f
        } else {
            binding.ivBackground.visibility = View.GONE
        }
    }

    private fun applyColors(primary: Int, accent: Int, surface: Int) {
        val accentList = ColorStateList.valueOf(accent)
        val surfaceList = ColorStateList.valueOf(surface)
        val primaryList = ColorStateList.valueOf(primary)

        binding.apply {
            root.setBackgroundColor(Color.WHITE)
            chipMode.backgroundTintList = surfaceList
            tvMode.setTextColor(accent)
            ivModeIcon.imageTintList = accentList
            tvMinutes.setTextColor(accent)
            tvSeconds.setTextColor(accent)
            tvColon.setTextColor(accent)

            btnPlayPause.backgroundTintList = surfaceList
            btnPlayPause.iconTint = accentList 

            btnSkip.backgroundTintList = surfaceList
            btnSkip.iconTint = accentList

            btnReset.backgroundTintList = surfaceList
            btnReset.iconTint = accentList

            progressTimer.progressTintList = primaryList
            progressTimer.progressBackgroundTintList = surfaceList
            chipMode.setStrokeColor(accentList)
        }
    }

    private fun syncServiceState() {
        val state = viewModel.timerState.value ?: return
        if (serviceBound && state.isRunning) {
            val settings = viewModel.settings.value ?: return
            timerService?.startTimer(state.timeLeft, settings.ambientSound)
        }
    }

    private fun bindTimerService() {
        Intent(requireContext(), TimerService::class.java).also { intent ->
            requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (serviceBound) {
            requireContext().unbindService(serviceConnection)
            serviceBound = false
        }
        _binding = null
    }
}
