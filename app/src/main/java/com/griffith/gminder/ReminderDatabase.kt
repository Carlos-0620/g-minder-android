package com.griffith.gminder

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val id: String,
    val title: String,
    val note: String,
    val place: String,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY createdAt, id")
    fun observeAll(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reminder: Reminder)

    @Query("UPDATE reminders SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean): Int

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun find(id: String): Reminder?
}

@Database(entities = [Reminder::class], version = 1, exportSchema = true)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminders(): ReminderDao

    companion object {
        @Volatile private var instance: ReminderDatabase? = null

        fun get(context: Context): ReminderDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ReminderDatabase::class.java, "reminders.db")
                .build().also { instance = it }
        }
    }
}
