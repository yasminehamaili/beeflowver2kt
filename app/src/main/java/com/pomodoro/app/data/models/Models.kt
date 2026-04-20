package com.pomodoro.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

enum class TimerMode { FOCUS, SHORT_BREAK, LONG_BREAK }
enum class AmbientSound { NONE, FOREST, CAFE, RAIN, OCEAN, FIREPLACE }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val sessions: Int = 0,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val id: String = System.currentTimeMillis().toString(),
    val taskName: String,
    val duration: Int,
    val date: Long = System.currentTimeMillis(),
    val type: String = "focus"
)

data class AppSettings(
    val focusDuration: Int = 50,
    val shortBreakDuration: Int = 5,
    val longBreakDuration: Int = 15,
    val autoStartNext: Boolean = false,
    val darkMode: Boolean = false,
    val notifications: Boolean = true,
    val ambientSound: AmbientSound = AmbientSound.NONE,
    val backgroundImageUri: String? = null,
    val primaryColor: Int = 0xFFE7992C.toInt(),
    val accentColor: Int = 0xFF483320.toInt(),
    val surfaceColor: Int = 0xFFFEE49A.toInt()
)

data class TimerState(
    val mode: TimerMode = TimerMode.FOCUS,
    val timeLeft: Int = 50 * 60,
    val isRunning: Boolean = false,
    val cycleCount: Int = 0
)