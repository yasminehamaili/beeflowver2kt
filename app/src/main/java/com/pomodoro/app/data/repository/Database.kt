package com.pomodoro.app.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import com.pomodoro.app.data.models.Session
import com.pomodoro.app.data.models.Task

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): LiveData<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY date DESC")
    fun getAllSessions(): LiveData<List<Session>>

    @Insert
    suspend fun insertSession(session: Session)

    @Query("SELECT * FROM sessions WHERE date >= :startTime")
    suspend fun getSessionsSince(startTime: Long): List<Session>
}

@Database(entities = [Task::class, Session::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pomodoro_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}