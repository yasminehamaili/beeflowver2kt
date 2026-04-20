package com.pomodoro.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.pomodoro.app.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val taskDao = db.taskDao()
    private val sessionDao = db.sessionDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    val allTasks: LiveData<List<Task>> = taskDao.getAllTasks()
    val allSessions: LiveData<List<Session>> = sessionDao.getAllSessions()

    suspend fun insertTask(task: Task) = withContext(Dispatchers.IO) { taskDao.insertTask(task) }
    suspend fun updateTask(task: Task) = withContext(Dispatchers.IO) { taskDao.updateTask(task) }
    suspend fun deleteTask(task: Task) = withContext(Dispatchers.IO) { taskDao.deleteTask(task) }
    suspend fun insertSession(session: Session) = withContext(Dispatchers.IO) { sessionDao.insertSession(session) }
    suspend fun getSessionsSince(startTime: Long) = withContext(Dispatchers.IO) { sessionDao.getSessionsSince(startTime) }

    fun getSettings(): AppSettings {
        val json = prefs.getString("settings", null) ?: return AppSettings()
        return try { gson.fromJson(json, AppSettings::class.java) } catch (e: Exception) { AppSettings() }
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit().putString("settings", gson.toJson(settings)).apply()
    }

    fun getTimerState(): TimerState {
        val json = prefs.getString("timer_state", null) ?: return TimerState()
        return try { gson.fromJson(json, TimerState::class.java) } catch (e: Exception) { TimerState() }
    }

    fun saveTimerState(state: TimerState) {
        prefs.edit().putString("timer_state", gson.toJson(state)).apply()
    }

    companion object {
        @Volatile private var INSTANCE: AppRepository? = null
        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            AppRepository(context).also { INSTANCE = it }
        }
    }
}