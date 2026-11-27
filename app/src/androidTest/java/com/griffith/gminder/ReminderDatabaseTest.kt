package com.griffith.gminder

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDatabaseTest {
    @Test fun remindersAndCompletionSurviveClosingAndReopeningDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "reminder-test-${java.util.UUID.randomUUID()}.db"
        var database = Room.databaseBuilder(context, ReminderDatabase::class.java, name).build()
        try {
            val reminder = Reminder("saved", "Buy groceries", "Milk and bread", "53.29000, -6.20000", createdAt = 1L)
            database.reminders().insert(reminder)
            assertEquals(1, database.reminders().setCompleted(reminder.id, true))
            database.close()
            database = Room.databaseBuilder(context, ReminderDatabase::class.java, name).build()
            assertEquals(reminder.copy(completed = true), database.reminders().find(reminder.id))
            assertEquals(listOf(reminder.copy(completed = true)), database.reminders().observeAll().first())
            assertEquals(1, database.reminders().setCompleted(reminder.id, false))
            database.close()
            database = Room.databaseBuilder(context, ReminderDatabase::class.java, name).build()
            assertEquals(reminder, database.reminders().find(reminder.id))
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test fun insertConflictCannotOverwriteAnExistingReminder() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, ReminderDatabase::class.java).build()
        try {
            val reminder = Reminder("same-id", "Original", "Keep this note", "53.3, -6.2")
            database.reminders().insert(reminder)
            var rejected = false
            try { database.reminders().insert(reminder.copy(title = "Replacement")) }
            catch (_: android.database.sqlite.SQLiteConstraintException) { rejected = true }
            assertTrue(rejected)
            assertEquals(reminder, database.reminders().find(reminder.id))
            assertEquals(0, database.reminders().setCompleted("missing", true))
        } finally { database.close() }
    }

    @Test fun flowStartsEmptyAndKeepsCreationOrderAcrossStatusChanges() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, ReminderDatabase::class.java).build()
        try {
            assertTrue(database.reminders().observeAll().first().isEmpty())
            val older = Reminder("older", "Coffee beans", "", "53.3, -6.2", createdAt = 1L)
            val newer = Reminder("newer", "Groceries", "", "53.4, -6.3", createdAt = 2L)
            database.reminders().insert(newer)
            database.reminders().insert(older)
            database.reminders().setCompleted(older.id, true)
            assertEquals(listOf(older.copy(completed = true), newer), database.reminders().observeAll().first())
        } finally { database.close() }
    }
}
