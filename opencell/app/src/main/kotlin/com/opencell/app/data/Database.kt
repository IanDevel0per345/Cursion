package com.opencell.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val goal: String,
    val startedAtMs: Long,
    val finishedAtMs: Long?,
    val outcome: String?,
    val summary: String?,
    val appPackage: String?,
    val provider: String?,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
)

@Entity(tableName = "task_events", indices = [Index("taskId")])
data class TaskEventEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val taskId: String,
    val atMs: Long,
    val kind: String,
    val title: String,
    val detail: String,
)

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(task: TaskEntity)
    @Insert suspend fun insertEvent(event: TaskEventEntity)
    @Query("SELECT * FROM tasks ORDER BY startedAtMs DESC LIMIT :limit") fun recent(limit: Int): Flow<List<TaskEntity>>
    @Query("SELECT * FROM task_events WHERE taskId = :taskId ORDER BY rowId") fun events(taskId: String): Flow<List<TaskEventEntity>>
    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun task(id: String): TaskEntity?
    @Query("SELECT * FROM tasks ORDER BY startedAtMs DESC") suspend fun all(): List<TaskEntity>
    @Query("SELECT * FROM task_events ORDER BY rowId") suspend fun allEvents(): List<TaskEventEntity>
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTask(id: String)
    @Query("DELETE FROM task_events WHERE taskId = :id") suspend fun deleteEvents(id: String)
    @Query("DELETE FROM tasks") suspend fun clearTasks()
    @Query("DELETE FROM task_events") suspend fun clearEvents()
    @Query("SELECT id FROM tasks WHERE startedAtMs < :before") suspend fun idsOlderThan(before: Long): List<String>
    @Query("SELECT COUNT(*) FROM tasks") fun count(): Flow<Int>
    @Query("UPDATE tasks SET inputTokens = inputTokens + :input, outputTokens = outputTokens + :output WHERE id = :id")
    suspend fun addTokens(id: String, input: Int, output: Int)
}

@Database(entities = [TaskEntity::class, TaskEventEntity::class], version = 1, exportSchema = true)
abstract class OpenCellDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao

    companion object {
        fun create(context: Context) = Room.databaseBuilder(context, OpenCellDatabase::class.java, "opencell.db").build()
    }
}
