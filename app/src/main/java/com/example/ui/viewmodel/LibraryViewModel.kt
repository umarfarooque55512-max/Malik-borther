package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.NoteEntity
import com.example.data.NoteRepository
import com.example.data.SettingsRepository
import com.example.data.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NoteFilter(val label: String) {
    ALL("All Stories"),
    PINNED("Pinned"),
    MULTI_STORY("Multi-Block"),
    DRIFTING("Drifting"),
    DENSE("Deep Charcoal")
}

data class LibraryUiState(
    val notes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: NoteFilter = NoteFilter.ALL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isOfflineMode: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val noteRepository: NoteRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(NoteFilter.ALL)

    private val searchedNotes = _searchQuery.flatMapLatest { query ->
        noteRepository.observeNotes(query)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        searchedNotes,
        _searchQuery,
        _activeFilter,
        settingsRepository.themeMode,
        settingsRepository.isOfflineMode
    ) { notes, query, filter, themeMode, isOffline ->
        val filtered = when (filter) {
            NoteFilter.ALL -> notes
            NoteFilter.PINNED -> notes.filter { it.isPinned }
            NoteFilter.MULTI_STORY -> notes.filter { it.blockCount > 1 }
            NoteFilter.DRIFTING -> notes.filter { it.hasDrift }
            NoteFilter.DENSE -> notes.filter { it.complexityScore >= 0.55f }
        }
        LibraryUiState(
            notes = filtered,
            searchQuery = query,
            activeFilter = filter,
            themeMode = themeMode,
            isOfflineMode = isOffline
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LibraryUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: NoteFilter) {
        _activeFilter.value = filter
    }

    fun cycleThemeMode() {
        viewModelScope.launch {
            val next = when (uiState.value.themeMode) {
                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.SYSTEM
            }
            settingsRepository.setThemeMode(next)
        }
    }

    fun toggleOfflineMode() {
        viewModelScope.launch {
            settingsRepository.setOfflineMode(!uiState.value.isOfflineMode)
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.setPinned(note.id, !note.isPinned)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }

    fun createNote(title: String = "", content: String = "", onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = noteRepository.createBlankNote(title = title, content = content)
            onCreated(newId)
        }
    }

    class Factory(
        private val noteRepository: NoteRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(noteRepository, settingsRepository) as T
        }
    }
}
