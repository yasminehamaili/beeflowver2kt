package com.pomodoro.app.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.pomodoro.app.data.models.Session
import com.pomodoro.app.databinding.FragmentStatsBinding
import com.pomodoro.app.ui.SharedViewModel
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SharedViewModel
    private var showWeekView = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        setupChart()
        setupToggle()
        setupObservers()
    }

    private fun setupToggle() {
        binding.btnDay.setOnClickListener {
            showWeekView = false
            updateToggleUI()
            viewModel.allSessions.value?.let { updateStats(it) }
        }
        binding.btnWeek.setOnClickListener {
            showWeekView = true
            updateToggleUI()
            viewModel.allSessions.value?.let { updateStats(it) }
        }
        updateToggleUI()
    }

    private fun updateToggleUI() {
        val colors = viewModel.themeColors.value
        val accent = colors?.accent ?: Color.parseColor("#483320")
        val surface = colors?.surface ?: Color.parseColor("#FEE49A")

        if (showWeekView) {
            binding.btnWeek.setBackgroundColor(Color.WHITE)
            binding.btnDay.setBackgroundColor(Color.TRANSPARENT)
        } else {
            binding.btnDay.setBackgroundColor(Color.WHITE)
            binding.btnWeek.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun setupChart() {
        binding.barChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            setTouchEnabled(false)
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#0D483320")
                setDrawAxisLine(false)
                textColor = Color.parseColor("#80483320")
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(v: Float) =
                        "${(v * 60).toInt()}m"
                }
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.parseColor("#80483320")
                granularity = 1f
            }
            animateY(500)
        }
    }

    private fun setupObservers() {
        viewModel.allSessions.observe(viewLifecycleOwner) { sessions ->
            updateStats(sessions)
        }

        viewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            binding.tvCompletedCount.text = tasks.count { it.completed }.toString()
        }

        viewModel.themeColors.observe(viewLifecycleOwner) { colors ->
            binding.tvTitle.setTextColor(colors.accent)
            binding.cardCompleted.setCardBackgroundColor(colors.surface)
            binding.cardDuration.setCardBackgroundColor(colors.surface)
            binding.cardStreak.setCardBackgroundColor(colors.surface)
            binding.tvCompletedCount.setTextColor(colors.accent)
            binding.tvDuration.setTextColor(colors.accent)
            binding.tvStreak.setTextColor(colors.accent)
            binding.tvCompletedLabel.setTextColor(colors.accent)
            binding.tvDurationLabel.setTextColor(colors.accent)
            binding.tvStreakLabel.setTextColor(colors.accent)
            binding.toggleContainer.backgroundTintList =
                android.content.res.ColorStateList.valueOf(colors.surface)
        }
    }

    private fun updateStats(sessions: List<Session>) {
        val focusSessions = sessions.filter { it.type == "focus" }
        val totalMins = focusSessions.sumOf { it.duration }
        val hours = totalMins / 60
        val mins = totalMins % 60
        binding.tvDuration.text = "${hours}h ${mins}m"
        binding.tvStreak.text = calcStreak(focusSessions).toString()

        updateChart(focusSessions)
    }

    private fun calcStreak(sessions: List<Session>): Int {
        if (sessions.isEmpty()) return 0
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        var streak = 0
        for (i in 0..29) {
            val dayStart = today - TimeUnit.DAYS.toMillis(i.toLong())
            val dayEnd = dayStart + TimeUnit.DAYS.toMillis(1)
            val hasSession = sessions.any { it.date in dayStart until dayEnd }
            if (hasSession) streak++ else if (i > 0) break
        }
        return streak
    }

    private fun updateChart(sessions: List<Session>) {
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val colors = viewModel.themeColors.value
        val barColor = colors?.primary ?: Color.parseColor("#E7992C")

        if (showWeekView) {
            val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val cal = Calendar.getInstance()
            val today = cal.get(Calendar.DAY_OF_WEEK)
            for (i in 6 downTo 0) {
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                val dayStart = c.apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val dayEnd = dayStart + TimeUnit.DAYS.toMillis(1)
                val mins = sessions.filter { it.date in dayStart until dayEnd }.sumOf { it.duration }
                entries.add(BarEntry((6 - i).toFloat(), mins / 60f))
                labels.add(days[c.get(Calendar.DAY_OF_WEEK) - 2 + 1])
            }
        } else {
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            for (i in 4 downTo 0) {
                val h = currentHour - i
                val displayH = if (h < 0) h + 24 else h
                val period = if (displayH >= 12) "PM" else "AM"
                val h12 = if (displayH % 12 == 0) 12 else displayH % 12
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, displayH)
                    set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                }
                val hourStart = cal.timeInMillis
                val hourEnd = hourStart + TimeUnit.HOURS.toMillis(1)
                val mins = sessions.filter { it.date in hourStart until hourEnd }.sumOf { it.duration }
                entries.add(BarEntry((4 - i).toFloat(), mins / 60f))
                labels.add("$h12$period")
            }
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = barColor
            setDrawValues(false)
            var barBorderRadius = 12f
        }

        binding.barChart.apply {
            data = BarData(dataSet).apply { barWidth = 0.5f }
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(v: Float) =
                    labels.getOrNull(v.toInt()) ?: ""
            }
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}