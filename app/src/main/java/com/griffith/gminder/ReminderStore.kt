package com.griffith.gminder

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReminderStore(application: Application) : AndroidViewModel(application) {
    private val dao = ReminderDatabase.get(application).reminders()
    private var observation: Job? = null

    var reminders by mutableStateOf<List<Reminder>?>(null)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var writeError by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    init { load() }

    fun load() {
        observation?.cancel()
        loadError = null
        observation = viewModelScope.launch {
            try { dao.observeAll().collect { reminders = it } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { loadError = "Saved reminders could not be loaded. Please try again." }
        }
    }

    fun add(title: String, note: String, place: String, onSaved: () -> Unit) {
        if (saving) return
        if (title.isBlank() || LocationFix.fromMapQuery(place) == null) {
            writeError = "Enter a title and confirm a location before saving."
            return
        }
        write {
            dao.insert(Reminder(java.util.UUID.randomUUID().toString(), title.trim(), note.trim(), place))
            onSaved()
        }
    }

    fun setCompleted(id: String, completed: Boolean, onSaved: () -> Unit = {}) {
        if (saving) return
        write {
            check(dao.setCompleted(id, completed) == 1)
            onSaved()
        }
    }

    fun dismissError() { writeError = null }

    private fun write(action: suspend () -> Unit) {
        saving = true
        writeError = null
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { writeError = "The change could not be saved. Please try again." }
            finally { saving = false }
        }
    }
}
