package com.example.pattern

enum class StoryPatternStyle(
    val displayName: String,
    val shortTag: String
) {
    SIMPLE_TALE("Simple Narrative", "Simple"),
    CHARACTER_STORY("Character Driven", "Character"),
    DIALOGUE_SCENE("Dialogue & Scene", "Dialogue"),
    DESCRIPTIVE_PROSE("Descriptive Prose", "Descriptive"),
    DRAMATIC_INTENSE("Dramatic / Dense", "Dramatic"),
    MULTILINGUAL_VERSE("Multilingual / Poetic", "Multilingual")
}

enum class StoryAlignment(val badgeLabel: String) {
    ON_PATTERN("On Story Pattern"),
    SLIGHT_DRIFT("Drifting from Story"),
    MAJOR_SHIFT("New Story Split")
}

data class StoryMetrics(
    val wordCount: Int,
    val uniqueWordCount: Int,
    val sentenceCount: Int,
    val guiraudTtr: Float,
    val shannonEntropy: Float,
    val avgWordLength: Float,
    val coherenceScore: Float,
    val driftPenalty: Float,
    val wallColorIndex: Float
) {
    val wallToneLabel: String
        get() = when {
            wallColorIndex < 0.20f -> "Light Beige · Steady"
            wallColorIndex < 0.40f -> "Warm Sand · Evolving"
            wallColorIndex < 0.62f -> "Taupe · Shifting"
            wallColorIndex < 0.80f -> "Slate · Complex / Drifting"
            else -> "Deep Charcoal · Dense / Off-Pattern"
        }
}

data class SegmentAnalysis(
    val segmentIndex: Int,
    val rawText: String,
    val coherenceWithAnchor: Float,
    val alignment: StoryAlignment,
    val explanation: String,
    val topKeywords: List<String>
)

data class StoryBlock(
    val id: String,
    val blockIndex: Int,
    val rawText: String,
    val separatorAfter: String,
    val style: StoryPatternStyle,
    val metrics: StoryMetrics,
    val alignment: StoryAlignment,
    val alignmentExplanation: String,
    val anchorKeywords: List<String>,
    val driftedKeywords: List<String>,
    val segments: List<SegmentAnalysis>,
    val shiftedFromPrevious: Boolean,
    val previousStoryLabel: String?
)

data class StoryDocumentAnalysis(
    val blocks: List<StoryBlock>,
    val overallComplexity: Float,
    val dominantStyle: StoryPatternStyle,
    val onPatternCount: Int,
    val driftingCount: Int,
    val storySplitCount: Int
)
