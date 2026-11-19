package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiTtsManager
import com.example.ai.TtsState
import com.example.data.NoteRepository
import com.example.data.SettingsRepository
import com.example.data.ThemeMode
import com.example.pattern.StoryBlock
import com.example.pattern.StoryDocumentAnalysis
import com.example.pattern.StoryPatternAnalyzer
import com.example.pattern.StorySpeechFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SaveStatus(val label: String) {
    SAVED("Auto-saved"),
    SAVING("Saving…"),
    OFFLINE_SAVED("Saved offline")
}

data class FocusCommand(
    val blockIndex: Int,
    val cursorOffset: Int,
    val token: Long = System.nanoTime()
)

data class EditorUiState(
    val noteId: Long = 0L,
    val title: String = "",
    val content: String = "",
    val isPinned: Boolean = false,
    val analysis: StoryDocumentAnalysis = StoryPatternAnalyzer.analyzeDocument(""),
    val activeBlockIndex: Int = 0,
    val saveStatus: SaveStatus = SaveStatus.SAVED,
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isOfflineMode: Boolean = false,
    val ttsVoice: String = "Kore",
    val ttsState: TtsState = TtsState.Idle,
    val focusCommand: FocusCommand? = null
)

class EditorViewModel(
    private val initialNoteId: Long,
    private val noteRepository: NoteRepository,
    private val settingsRepository: SettingsRepository,
    private val ttsManager: GeminiTtsManager
) : ViewModel() {

    private val _noteId = MutableStateFlow(initialNoteId)
    private val _title = MutableStateFlow("")
    private val _content = MutableStateFlow("")
    private val _isPinned = MutableStateFlow(false)
    private val _analysis = MutableStateFlow(StoryPatternAnalyzer.analyzeDocument(""))
    private val _activeBlockIndex = MutableStateFlow(0)
    private val _saveStatus = MutableStateFlow(SaveStatus.SAVED)
    private val _isLoading = MutableStateFlow(true)
    private val _focusCommand = MutableStateFlow<FocusCommand?>(null)

    private var createdAt: Long = System.currentTimeMillis()
    private var autoSaveJob: Job? = null

    val uiState: StateFlow<EditorUiState> = combine(
        combine(_noteId, _title, _content, _isPinned, _analysis) { id, title, content, pinned, analysis ->
            Tuple5(id, title, content, pinned, analysis)
        },
        combine(_activeBlockIndex, _saveStatus, _isLoading, _focusCommand) { activeIdx, save, loading, focus ->
            Tuple4(activeIdx, save, loading, focus)
        },
        combine(
            settingsRepository.themeMode,
            settingsRepository.isOfflineMode,
            settingsRepository.ttsVoice,
            ttsManager.ttsState
        ) { theme, offline, voice, tts ->
            Tuple4(theme, offline, voice, tts)
        }
    ) { notePart, editorPart, settingsPart ->
        val saveLabel = if (settingsPart.second && editorPart.second == SaveStatus.SAVED) {
            SaveStatus.OFFLINE_SAVED
        } else {
            editorPart.second
        }
        EditorUiState(
            noteId = notePart.first,
            title = notePart.second,
            content = notePart.third,
            isPinned = notePart.fourth,
            analysis = notePart.fifth,
            activeBlockIndex = editorPart.first.coerceIn(0, (notePart.fifth.blocks.size - 1).coerceAtLeast(0)),
            saveStatus = saveLabel,
            isLoading = editorPart.third,
            themeMode = settingsPart.first,
            isOfflineMode = settingsPart.second,
            ttsVoice = settingsPart.third,
            ttsState = settingsPart.fourth,
            focusCommand = editorPart.fourth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = EditorUiState(noteId = initialNoteId)
    )

    init {
        loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            if (initialNoteId > 0L) {
                val existing = noteRepository.getNoteById(initialNoteId)
                if (existing != null) {
                    _noteId.value = existing.id
                    _title.value = existing.title
                    _content.value = existing.content
                    _isPinned.value = existing.isPinned
                    createdAt = existing.createdAt
                    _analysis.value = StoryPatternAnalyzer.analyzeDocument(existing.content)
                }
            }
            _isLoading.value = false
        }
    }

    fun onTitleChange(newTitle: String) {
        _title.value = newTitle
        scheduleAutoSave()
    }

    fun onActiveBlockFocused(blockIndex: Int) {
        _activeBlockIndex.value = blockIndex
    }

    fun onBlockTextChange(blockIndex: Int, newBlockText: String, cursorPos: Int) {
        val currentBlocks = _analysis.value.blocks
        if (blockIndex !in currentBlocks.indices) return

        val previousCount = currentBlocks.size
        val updatedContent = buildString {
            currentBlocks.forEachIndexed { idx, block ->
                val text = if (idx == blockIndex) newBlockText else block.rawText
                append(text)
                if (idx < currentBlocks.lastIndex) {
                    append(block.separatorAfter.ifEmpty { "\n\n" })
                } else if (block.separatorAfter.isNotEmpty()) {
                    append(block.separatorAfter)
                }
            }
        }

        val newAnalysis = StoryPatternAnalyzer.analyzeDocument(updatedContent)
        _content.value = updatedContent
        _analysis.value = newAnalysis

        // Carry keyboard focus seamlessly when a new story splits off into a separate block
        if (newAnalysis.blocks.size > previousCount && blockIndex + 1 < newAnalysis.blocks.size) {
            val prevBlockLen = newAnalysis.blocks[blockIndex].rawText.length
            if (cursorPos > prevBlockLen) {
                val nextBlockText = newAnalysis.blocks[blockIndex + 1].rawText
                val offsetInNext = (cursorPos - prevBlockLen - 1).coerceIn(0, nextBlockText.length)
                _activeBlockIndex.value = blockIndex + 1
                _focusCommand.value = FocusCommand(blockIndex + 1, offsetInNext)
            }
        } else if (newAnalysis.blocks.size < previousCount) {
            val targetIdx = blockIndex.coerceIn(0, newAnalysis.blocks.lastIndex)
            _activeBlockIndex.value = targetIdx
            _focusCommand.value = FocusCommand(targetIdx, newAnalysis.blocks[targetIdx].rawText.length)
        }

        scheduleAutoSave()
    }

    fun addNewStoryBlock() {
        val currentContent = _content.value.trimEnd()
        val nextText = if (currentContent.isEmpty()) "" else "$currentContent\n\n"
        _content.value = nextText
        val newAnalysis = StoryPatternAnalyzer.analyzeDocument(nextText)
        _analysis.value = newAnalysis
        val targetIndex = newAnalysis.blocks.lastIndex.coerceAtLeast(0)
        _activeBlockIndex.value = targetIndex
        _focusCommand.value = FocusCommand(targetIndex, newAnalysis.blocks[targetIndex].rawText.length)
        scheduleAutoSave()
    }

    fun removeBlock(blockIndex: Int) {
        val blocks = _analysis.value.blocks
        if (blocks.size <= 1) {
            _content.value = ""
            _analysis.value = StoryPatternAnalyzer.analyzeDocument("")
            _activeBlockIndex.value = 0
            scheduleAutoSave()
            return
        }
        val remaining = blocks.filterIndexed { index, _ -> index != blockIndex }
        val rebuilt = remaining.joinToString("\n\n") { it.rawText }
        _content.value = rebuilt
        val newAnalysis = StoryPatternAnalyzer.analyzeDocument(rebuilt)
        _analysis.value = newAnalysis
        val nextIdx = (blockIndex - 1).coerceIn(0, newAnalysis.blocks.lastIndex)
        _activeBlockIndex.value = nextIdx
        scheduleAutoSave()
    }

    fun loadStoryPreset(title: String, content: String) {
        if (_title.value.isBlank()) {
            _title.value = title
        }
        _content.value = content
        _analysis.value = StoryPatternAnalyzer.analyzeDocument(content)
        _activeBlockIndex.value = 0
        scheduleAutoSave()
    }

    fun clearFocusCommand() {
        _focusCommand.value = null
    }

    fun togglePin() {
        _isPinned.value = !_isPinned.value
        flushSave()
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

    fun selectTtsVoice(voiceName: String) {
        viewModelScope.launch {
            settingsRepository.setTtsVoice(voiceName)
        }
    }

    fun speakEntireNote() {
        val state = uiState.value
        val spoken = StorySpeechFormatter.formatNoteForSpeech(state.title, state.analysis.blocks)
        ttsManager.speak(
            scope = viewModelScope,
            label = state.title.ifBlank { "Story Note" },
            spokenText = spoken,
            voiceName = state.ttsVoice,
            isOfflineMode = state.isOfflineMode
        )
    }

    fun speakBlock(block: StoryBlock) {
        val state = uiState.value
        val spoken = StorySpeechFormatter.formatBlockForSpeech(block)
        ttsManager.speak(
            scope = viewModelScope,
            label = "Story Block ${block.blockIndex + 1}",
            spokenText = spoken,
            voiceName = state.ttsVoice,
            isOfflineMode = state.isOfflineMode
        )
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun dismissTtsError() {
        ttsManager.dismissError()
    }

    private fun scheduleAutoSave() {
        _saveStatus.value = SaveStatus.SAVING
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(350L)
            persistNow()
        }
    }

    fun flushSave() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            persistNow()
        }
    }

    private suspend fun persistNow() {
        val titleVal = _title.value
        val contentVal = _content.value
        if (_noteId.value == 0L && titleVal.isBlank() && contentVal.isBlank()) {
            _saveStatus.value = SaveStatus.SAVED
            return
        }
        val savedId = noteRepository.saveNote(
            id = _noteId.value,
            title = titleVal.ifBlank { deriveDefaultTitle() },
            content = contentVal,
            isPinned = _isPinned.value,
            createdAt = createdAt
        )
        _noteId.value = savedId
        _saveStatus.value = SaveStatus.SAVED
    }

    private fun deriveDefaultTitle(): String {
        val firstLine = _content.value.lines().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        return if (firstLine.isBlank()) {
            "Untitled Story"
        } else {
            firstLine.take(36)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }

    private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    private data class Tuple5<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

    class Factory(
        private val noteId: Long,
        private val noteRepository: NoteRepository,
        private val settingsRepository: SettingsRepository,
        private val ttsManager: GeminiTtsManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(noteId, noteRepository, settingsRepository, ttsManager) as T
        }
    }
}
