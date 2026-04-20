package com.pomodoro.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.*
import androidx.palette.graphics.Palette
import com.pomodoro.app.data.models.*
import com.pomodoro.app.data.repository.AppRepository
import kotlinx.coroutines.launch

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppRepository.getInstance(application)

    val allTasks: LiveData<List<Task>> = repo.allTasks
    val allSessions: LiveData<List<Session>> = repo.allSessions

    private val _settings = MutableLiveData(repo.getSettings())
    val settings: LiveData<AppSettings> = _settings

    private val _timerState = MutableLiveData(repo.getTimerState())
    val timerState: LiveData<TimerState> = _timerState

    private val _activeTask = MutableLiveData<Task?>(null)
    val activeTask: LiveData<Task?> = _activeTask

    private val _themeColors = MutableLiveData<ThemeColors>()
    val themeColors: LiveData<ThemeColors> = _themeColors

    init {
        val s = repo.getSettings()
        _themeColors.value = ThemeColors(s.primaryColor, s.accentColor, s.surfaceColor)
    }

    fun addTask(name: String) = viewModelScope.launch {
        repo.insertTask(Task(name = name))
    }

    fun completeTask(task: Task) = viewModelScope.launch {
        repo.updateTask(task.copy(completed = !task.completed))
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repo.deleteTask(task)
        if (_activeTask.value?.id == task.id) _activeTask.value = null
    }

    fun updateTaskSessions(task: Task, sessions: Int) = viewModelScope.launch {
        repo.updateTask(task.copy(sessions = sessions))
    }

    fun setActiveTask(task: Task?) { _activeTask.value = task }

    fun addSession(taskName: String, duration: Int, type: String = "focus") = viewModelScope.launch {
        repo.insertSession(Session(taskName = taskName, duration = duration, type = type))
    }

    fun updateSettings(update: AppSettings.() -> AppSettings) {
        val new = _settings.value!!.update()
        _settings.value = new
        repo.saveSettings(new)
    }

    fun updateTimerState(update: TimerState.() -> TimerState) {
        val new = _timerState.value!!.update()
        _timerState.value = new
        repo.saveTimerState(new)
    }

    fun extractColorsFromBitmap(bitmap: Bitmap, imageUri: Uri) {
        Palette.from(bitmap).generate { palette ->
            palette ?: return@generate
            val primary = palette.getVibrantColor(
                palette.getMutedColor(0xFFE7992C.toInt())
            )
            val accent = palette.getDarkVibrantColor(
                palette.getDarkMutedColor(0xFF483320.toInt())
            )
            val surface = palette.getLightVibrantColor(
                palette.getLightMutedColor(0xFFFEE49A.toInt())
            )
            val colors = ThemeColors(primary, accent, surface)
            _themeColors.value = colors
            updateSettings {
                copy(
                    backgroundImageUri = imageUri.toString(),
                    primaryColor = primary,
                    accentColor = accent,
                    surfaceColor = surface
                )
            }
        }
    }

    fun clearBackgroundImage() {
        updateSettings {
            copy(
                backgroundImageUri = null,
                primaryColor = 0xFFE7992C.toInt(),
                accentColor = 0xFF483320.toInt(),
                surfaceColor = 0xFFFEE49A.toInt()
            )
        }
        _themeColors.value = ThemeColors(0xFFE7992C.toInt(), 0xFF483320.toInt(), 0xFFFEE49A.toInt())
    }

    fun getDuration(mode: TimerMode): Int {
        val s = _settings.value ?: AppSettings()
        return when (mode) {
            TimerMode.FOCUS -> s.focusDuration * 60
            TimerMode.SHORT_BREAK -> s.shortBreakDuration * 60
            TimerMode.LONG_BREAK -> s.longBreakDuration * 60
        }
    }
}

data class ThemeColors(val primary: Int, val accent: Int, val surface: Int)