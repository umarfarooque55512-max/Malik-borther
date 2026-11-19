package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiTtsManager
import com.example.ai.TtsState
import com.example.data.ThemeMode
import com.example.pattern.StoryAlignment
import com.example.pattern.StoryBlock
import com.example.ui.theme.AmberDrift
import com.example.ui.theme.ComplexityPalette
import com.example.ui.theme.DarkAmber
import com.example.ui.theme.DarkSage
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.SagePattern
import com.example.ui.viewmodel.EditorUiState
import com.example.ui.viewmodel.FocusCommand
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    state: EditorUiState,
    onNavigateBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBlockFocused: (Int) -> Unit,
    onBlockTextChange: (Int, String, Int) -> Unit,
    onAddStoryBlock: () -> Unit,
    onRemoveBlock: (Int) -> Unit,
    onLoadPreset: (String, String) -> Unit,
    onClearFocusCommand: () -> Unit,
    onTogglePin: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleOffline: () -> Unit,
    onSelectVoice: (String) -> Unit,
    onSpeakNote: () -> Unit,
    onSpeakBlock: (StoryBlock) -> Unit,
    onStopSpeaking: () -> Unit,
    onDismissTtsError: () -> Unit,
    onFlushSave: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    var inspectedBlock by remember { mutableStateOf<StoryBlock?>(null) }
    var showVoicePicker by remember { mutableStateOf(false) }

    val focusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }

    BackHandler {
        onFlushSave()
        onStopSpeaking()
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            onFlushSave()
        }
    }

    // IMPORTANT: Outer notepad page stays steady neutral paper/dark studio!
    // Color changes appear ONLY on the wall directly behind the text in each story block.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = state.ttsState !is TtsState.Idle || showVoicePicker,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                ) {
                    TtsControlBar(
                        ttsState = state.ttsState,
                        selectedVoice = state.ttsVoice,
                        showVoicePicker = showVoicePicker,
                        onToggleVoicePicker = { showVoicePicker = !showVoicePicker },
                        onSelectVoice = {
                            onSelectVoice(it)
                            showVoicePicker = false
                        },
                        onStop = onStopSpeaking,
                        onDismissError = onDismissTtsError
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(innerPadding)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        onFlushSave()
                        onStopSpeaking()
                        onNavigateBack()
                    },
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to library",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = state.saveStatus.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("autosave_status_pill")
                    )
                }

                Spacer(Modifier.weight(1f))

                // TTS Voice Picker Toggle
                IconButton(
                    onClick = { showVoicePicker = !showVoicePicker },
                    modifier = Modifier.testTag("voice_picker_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = "Choose TTS voice (${state.ttsVoice})",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Read Whole Note Aloud (Gemini 3.8 Flash TTS)
                IconButton(
                    onClick = {
                        if (state.ttsState is TtsState.Playing || state.ttsState is TtsState.Generating) {
                            onStopSpeaking()
                        } else {
                            onSpeakNote()
                        }
                    },
                    modifier = Modifier.testTag("speak_note_button")
                ) {
                    Icon(
                        imageVector = if (state.ttsState is TtsState.Playing) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Read story aloud with Gemini TTS",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = onToggleOffline,
                    modifier = Modifier.testTag("editor_offline_toggle")
                ) {
                    Icon(
                        imageVector = if (state.isOfflineMode) Icons.Rounded.CloudOff else Icons.Rounded.CloudDone,
                        contentDescription = if (state.isOfflineMode) "Offline mode enabled" else "Online TTS enabled",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.testTag("editor_pin_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = if (state.isPinned) "Unpin note" else "Pin note",
                        tint = if (state.isPinned) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                IconButton(
                    onClick = onCycleTheme,
                    modifier = Modifier.testTag("editor_theme_button")
                ) {
                    val icon = when (state.themeMode) {
                        ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                        ThemeMode.LIGHT -> Icons.Rounded.LightMode
                        ThemeMode.DARK -> Icons.Rounded.DarkMode
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Theme: ${state.themeMode.label}",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Main Adaptive-Width Centered Sheet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)
                        .imePadding(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Title & Notebook Pattern Summary Header
                    item(key = "header") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            BasicTextField(
                                value = state.title,
                                onValueChange = onTitleChange,
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                textStyle = MaterialTheme.typography.headlineSmall.copy(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 24.sp,
                                    textDirection = TextDirection.Content
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("note_title_input"),
                                decorationBox = { innerTextField ->
                                    if (state.title.isEmpty()) {
                                        Text(
                                            text = "Story Title…",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                fontSize = 24.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            Spacer(Modifier.height(8.dp))

                            StoryWallLegendBar(
                                storyCount = state.analysis.blocks.size,
                                onPatternCount = state.analysis.onPatternCount,
                                driftingCount = state.analysis.driftingCount,
                                splitCount = state.analysis.storySplitCount,
                                isDark = isDark
                            )
                        }
                    }

                    // Quick Multilingual Story Presets when Note is Empty
                    if (state.content.isBlank()) {
                        item(key = "presets") {
                            StoryPresetStrip(onSelectPreset = onLoadPreset)
                        }
                    }

                    // Separated Story Blocks (ONLY the wall behind each block's words changes color!)
                    itemsIndexed(
                        items = state.analysis.blocks,
                        key = { index, _ -> "story_block_$index" }
                    ) { index, block ->
                        val requester = focusRequesters.getOrPut(index) { FocusRequester() }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            AnimatedVisibility(
                                visible = block.shiftedFromPrevious,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                NewStoryBridge(
                                    fromLabel = block.previousStoryLabel ?: "Story ${index}",
                                    newIndex = index + 1
                                )
                            }

                            StoryBlockCard(
                                block = block,
                                totalBlocks = state.analysis.blocks.size,
                                isActive = index == state.activeBlockIndex,
                                isDark = isDark,
                                focusRequester = requester,
                                focusCommand = state.focusCommand?.takeIf { it.blockIndex == index },
                                onConsumeFocusCommand = onClearFocusCommand,
                                onFocused = { onBlockFocused(index) },
                                onTextChanged = { newText, cursorPos ->
                                    onBlockTextChange(index, newText, cursorPos)
                                },
                                onSpeakBlock = { onSpeakBlock(block) },
                                onInspectBlock = { inspectedBlock = block },
                                onRemoveBlock = { onRemoveBlock(index) }
                            )
                        }
                    }

                    item(key = "add_block_footer") {
                        OutlinedButton(
                            onClick = onAddStoryBlock,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("add_block_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Start Separate Story Block")
                        }
                    }
                }
            }
        }
    }

    inspectedBlock?.let { block ->
        StoryInspectorDialog(
            block = block,
            onDismiss = { inspectedBlock = null }
        )
    }
}

@Composable
private fun StoryWallLegendBar(
    storyCount: Int,
    onPatternCount: Int,
    driftingCount: Int,
    splitCount: Int,
    isDark: Boolean
) {
    val gradientColors = remember(isDark) {
        listOf(
            ComplexityPalette.colorForComplexity(0.0f, isDark),
            ComplexityPalette.colorForComplexity(0.25f, isDark),
            ComplexityPalette.colorForComplexity(0.50f, isDark),
            ComplexityPalette.colorForComplexity(0.75f, isDark),
            ComplexityPalette.colorForComplexity(1.0f, isDark)
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$storyCount Story ${if (storyCount == 1) "Block" else "Blocks"} ($splitCount auto-split)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$onPatternCount on-pattern · $driftingCount drifting",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(gradientColors))
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Text Wall: Light Beige (Steady)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Deep Charcoal (Drift / Dense)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NewStoryBridge(
    fromLabel: String,
    newIndex: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .testTag("story_split_bridge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = "New story detected",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "New Story #$newIndex Separated (shifted from $fromLabel)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun StoryBlockCard(
    block: StoryBlock,
    totalBlocks: Int,
    isActive: Boolean,
    isDark: Boolean,
    focusRequester: FocusRequester,
    focusCommand: FocusCommand?,
    onConsumeFocusCommand: () -> Unit,
    onFocused: () -> Unit,
    onTextChanged: (String, Int) -> Unit,
    onSpeakBlock: () -> Unit,
    onInspectBlock: () -> Unit,
    onRemoveBlock: () -> Unit
) {
    // ONLY the wall directly behind the text animates color from Light Beige -> Deep Charcoal!
    val animatedWallColor by animateColorAsState(
        targetValue = ComplexityPalette.colorForComplexity(block.metrics.wallColorIndex, isDark),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "storyWallColor_${block.blockIndex}"
    )

    val wallPrimaryInk = ComplexityPalette.primaryInkOn(animatedWallColor)
    val wallSecondaryInk = ComplexityPalette.secondaryInkOn(animatedWallColor)

    var textFieldValue by remember(block.blockIndex) {
        mutableStateOf(TextFieldValue(text = block.rawText, selection = TextRange(block.rawText.length)))
    }

    LaunchedEffect(block.rawText) {
        if (textFieldValue.text != block.rawText) {
            val safePos = textFieldValue.selection.end.coerceIn(0, block.rawText.length)
            textFieldValue = TextFieldValue(text = block.rawText, selection = TextRange(safePos))
        }
    }

    LaunchedEffect(focusCommand?.token) {
        if (focusCommand != null) {
            val safeCursor = focusCommand.cursorOffset.coerceIn(0, textFieldValue.text.length)
            textFieldValue = textFieldValue.copy(selection = TextRange(safeCursor))
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
            onConsumeFocusCommand()
        }
    }

    // Outer block frame stays calm neutral surface; the Text Wall inside carries the animated color!
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .testTag("story_block_card_${block.blockIndex}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Neutral Block Metadata Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Story ${block.blockIndex + 1} · ${block.style.shortTag}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.width(6.dp))

                StoryAlignmentBadge(alignment = block.alignment, isDark = isDark)

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = onSpeakBlock,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("speak_block_${block.blockIndex}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Read story block aloud",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onInspectBlock,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("inspect_block_${block.blockIndex}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Inspect mathematical story metrics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (totalBlocks > 1) {
                    IconButton(
                        onClick = onRemoveBlock,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("remove_block_${block.blockIndex}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove story block",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // THE COLOR-SHIFTING TEXT WALL RIGHT BEHIND THE WORDS!
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = animatedWallColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = ComplexityPalette.borderOn(animatedWallColor),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .testTag("story_text_wall_${block.blockIndex}")
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { updated ->
                        textFieldValue = updated
                        onTextChanged(updated.text, updated.selection.end)
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    textStyle = TextStyle(
                        fontFamily = LoraFontFamily,
                        fontSize = 16.5.sp,
                        lineHeight = 25.sp,
                        color = wallPrimaryInk,
                        textDirection = TextDirection.Content
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onFocused()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .testTag("block_editor_${block.blockIndex}"),
                    decorationBox = { innerTextField ->
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = "Write your story here (English, اردو, Roman Urdu)…\nThe wall behind these words changes color with story complexity & drift.",
                                fontFamily = LoraFontFamily,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = wallSecondaryInk.copy(alpha = 0.7f)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            // Neutral Pattern Diagnostic Footer
            Text(
                text = block.alignmentExplanation,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${block.metrics.wallToneLabel} (${(block.metrics.wallColorIndex * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${block.metrics.wordCount} words · Coherence ${(block.metrics.coherenceScore * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StoryAlignmentBadge(
    alignment: StoryAlignment,
    isDark: Boolean
) {
    val (bg, fg, icon) = when (alignment) {
        StoryAlignment.ON_PATTERN -> Triple(
            if (isDark) DarkSage.copy(alpha = 0.18f) else SagePattern.copy(alpha = 0.14f),
            if (isDark) DarkSage else SagePattern,
            Icons.Rounded.CheckCircleOutline
        )
        StoryAlignment.SLIGHT_DRIFT -> Triple(
            if (isDark) DarkAmber.copy(alpha = 0.20f) else AmberDrift.copy(alpha = 0.15f),
            if (isDark) DarkAmber else AmberDrift,
            Icons.AutoMirrored.Rounded.TrendingUp
        )
        StoryAlignment.MAJOR_SHIFT -> Triple(
            if (isDark) DarkAmber.copy(alpha = 0.20f) else AmberDrift.copy(alpha = 0.15f),
            if (isDark) DarkAmber else AmberDrift,
            Icons.Rounded.Bolt
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        modifier = Modifier.testTag("alignment_badge_${alignment.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = alignment.badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
    }
}

@Composable
private fun StoryPresetStrip(
    onSelectPreset: (String, String) -> Unit
) {
    val presets = listOf(
        Triple(
            "Two Stories Split",
            "The Village Baker & The Starship",
            "Every dawn, old Tariq baked warm bread in his quiet clay oven beside the river. Children smelled the sweet loaves from the village lane and ran to greet him.\n\nMeanwhile, Commander Elena calibrated the plasma thrusters aboard Orion Seven while asteroids rattled the titanium hull near Jupiter."
        ),
        Triple(
            "Story Drift Alert",
            "Forest Walk Drifting Off-Pattern",
            "Sara walked slowly along the misty pine forest trail listening to silver birds sing. The morning dew sparkled on green ferns beside the wooden forest bridge. Suddenly quarterly tax audits, corporate spreadsheets, and cryptocurrency brokerage fees interrupted everything."
        ),
        Triple(
            "Urdu Dual-Story",
            "دو مختلف کہانیاں",
            "ایک دن ایک چھوٹی سی چڑیا باغ میں درخت پر بیٹھ کر میٹھے گیت گا رہی تھی۔ پھولوں کی خوشبو ہر طرف پھیلی ہوئی تھی۔\n\nدوسری طرف سمندر کی گہرائی میں ایک آبدوز تاریک پانیوں کو چیرتی ہوئی آگے بڑھ رہی تھی اور ریڈار پر عجیب سگنل آ رہے تھے۔"
        )
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Try a story preset (English or Urdu):",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (chipLabel, title, body) ->
                Surface(
                    onClick = { onSelectPreset(title, body) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = chipLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TtsControlBar(
    ttsState: TtsState,
    selectedVoice: String,
    showVoicePicker: Boolean,
    onToggleVoicePicker: () -> Unit,
    onSelectVoice: (String) -> Unit,
    onStop: () -> Unit,
    onDismissError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (showVoicePicker) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gemini 3.8 Flash TTS Voice:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GeminiTtsManager.availableVoices.forEach { voice ->
                    val selected = voice == selectedVoice
                    Surface(
                        onClick = { onSelectVoice(voice) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = voice,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            if (ttsState !is TtsState.Idle) {
                Spacer(Modifier.height(6.dp))
            }
        }

        when (ttsState) {
            is TtsState.Generating -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Synthesizing \"${ttsState.label}\" via gemini-3.8-flash-tts…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Stop, contentDescription = "Cancel TTS")
                    }
                }
            }
            is TtsState.Playing -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Reading \"${ttsState.label}\" ($selectedVoice)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = onStop) {
                        Text("Stop")
                    }
                }
            }
            is TtsState.Error -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = ttsState.message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissError, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Dismiss error",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            TtsState.Idle -> {
                if (showVoicePicker) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onToggleVoicePicker() }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryInspectorDialog(
    block: StoryBlock,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Story Block #${block.blockIndex + 1} · ${block.style.displayName}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Mathematical NLP Analysis (Offline · Zero AI)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Wall Index: ${(block.metrics.wallColorIndex * 100).roundToInt()}% (${block.metrics.wallToneLabel})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Guiraud TTR: ${String.format("%.2f", block.metrics.guiraudTtr)} · Shannon Entropy: ${String.format("%.2f", block.metrics.shannonEntropy)} · Drift Penalty: +${(block.metrics.driftPenalty * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (block.anchorKeywords.isNotEmpty()) {
                    Text(
                        text = "Story Anchor Keywords: ${block.anchorKeywords.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (block.driftedKeywords.isNotEmpty()) {
                    Text(
                        text = "Drifting Words: ${block.driftedKeywords.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (block.segments.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Sentence-by-Sentence Coherence:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    block.segments.forEach { seg ->
                        Text(
                            text = "• [${seg.alignment.badgeLabel}] ${seg.explanation}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
