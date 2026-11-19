package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ai.GeminiTtsManager
import com.example.ai.configureAppCheck
import com.example.data.NoteDatabase
import com.example.data.NoteRepository
import com.example.data.SettingsRepository
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.theme.MathPadTheme
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.LibraryViewModel

private const val ROUTE_LIBRARY = "library"
private const val ROUTE_EDITOR = "editor/{noteId}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before rendering UI or creating ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val database = NoteDatabase.getInstance(applicationContext)
        val noteRepository = NoteRepository(database.noteDao())
        val settingsRepository = SettingsRepository(applicationContext)
        val ttsManager = GeminiTtsManager()

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = com.example.data.ThemeMode.SYSTEM
            )

            MathPadTheme(themeMode = themeMode) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_LIBRARY
                ) {
                    composable(ROUTE_LIBRARY) {
                        val libraryViewModel: LibraryViewModel = viewModel(
                            factory = remember {
                                LibraryViewModel.Factory(noteRepository, settingsRepository)
                            }
                        )
                        val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()

                        LibraryScreen(
                            state = libraryState,
                            onSearchQueryChange = libraryViewModel::onSearchQueryChange,
                            onFilterSelected = libraryViewModel::onFilterSelected,
                            onCycleTheme = libraryViewModel::cycleThemeMode,
                            onToggleOffline = libraryViewModel::toggleOfflineMode,
                            onNoteClick = { noteId ->
                                navController.navigate("editor/$noteId")
                            },
                            onCreateBlankNote = {
                                navController.navigate("editor/0")
                            },
                            onCreateDemoNote = {
                                libraryViewModel.createNote(
                                    title = "Two Stories & Drift Demo",
                                    content = "Every morning, old Tariq baked warm bread in his quiet clay oven beside the river. Children smelled the sweet loaves from the village lane and ran to greet him.\n\nMeanwhile, Commander Elena calibrated the plasma thrusters aboard Orion Seven while asteroids rattled the titanium hull near Jupiter. Later, quarterly tax audits and corporate spreadsheets interrupted the space mission."
                                ) { newId ->
                                    navController.navigate("editor/$newId")
                                }
                            },
                            onTogglePin = libraryViewModel::togglePin,
                            onDeleteNote = libraryViewModel::deleteNote
                        )
                    }

                    composable(
                        route = ROUTE_EDITOR,
                        arguments = listOf(navArgument("noteId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                        val editorViewModel: EditorViewModel = viewModel(
                            key = "editor_$noteId",
                            factory = remember(noteId) {
                                EditorViewModel.Factory(
                                    noteId = noteId,
                                    noteRepository = noteRepository,
                                    settingsRepository = settingsRepository,
                                    ttsManager = ttsManager
                                )
                            }
                        )
                        val editorState by editorViewModel.uiState.collectAsStateWithLifecycle()

                        EditorScreen(
                            state = editorState,
                            onNavigateBack = { navController.popBackStack() },
                            onTitleChange = editorViewModel::onTitleChange,
                            onBlockFocused = editorViewModel::onActiveBlockFocused,
                            onBlockTextChange = editorViewModel::onBlockTextChange,
                            onAddStoryBlock = editorViewModel::addNewStoryBlock,
                            onRemoveBlock = editorViewModel::removeBlock,
                            onLoadPreset = editorViewModel::loadStoryPreset,
                            onClearFocusCommand = editorViewModel::clearFocusCommand,
                            onTogglePin = editorViewModel::togglePin,
                            onCycleTheme = editorViewModel::cycleThemeMode,
                            onToggleOffline = editorViewModel::toggleOfflineMode,
                            onSelectVoice = editorViewModel::selectTtsVoice,
                            onSpeakNote = editorViewModel::speakEntireNote,
                            onSpeakBlock = editorViewModel::speakBlock,
                            onStopSpeaking = editorViewModel::stopSpeaking,
                            onDismissTtsError = editorViewModel::dismissTtsError,
                            onFlushSave = editorViewModel::flushSave
                        )
                    }
                }
            }
        }
    }
}
