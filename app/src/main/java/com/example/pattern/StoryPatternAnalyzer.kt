package com.example.pattern

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object StoryPatternAnalyzer {

    private val englishStopwords = setOf(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
        "by", "from", "up", "about", "into", "onto", "upon", "over", "after", "before",
        "during", "since", "until", "through", "along", "around", "beside", "near",
        "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
        "do", "does", "did", "will", "would", "could", "should", "may", "might", "must",
        "shall", "can", "it", "its", "he", "she", "they", "we", "you", "i", "me", "him",
        "her", "them", "us", "my", "his", "their", "our", "your", "this", "that", "these",
        "those", "there", "here", "then", "than", "when", "where", "while", "as", "so",
        "if", "not", "no", "yes", "very", "just", "one", "two", "all", "some", "also",
        "who", "what", "which", "whom", "whose", "every", "each", "other", "another",
        "many", "much", "more", "most", "such", "only", "own", "same", "too", "still",
        "yet", "even", "suddenly", "everything"
    )

    private val romanUrduStopwords = setOf(
        "aur", "hai", "hain", "tha", "thi", "the", "ke", "ki", "ka", "ko", "se", "mein",
        "me", "main", "ek", "aik", "woh", "wo", "yeh", "ye", "par", "pe", "ne", "kya",
        "kyun", "jab", "tab", "ab", "phir", "bhi", "nahi", "na", "to", "hi", "kar", "karke",
        "raha", "rahi", "rahe", "gaya", "gayi", "gaye", "hua", "hui", "huye", "us", "is",
        "apne", "apni", "apna", "sab", "kuch", "bahut", "bohat", "liya", "diya", "kaha",
        "har", "taraf", "saath", "baad", "pehle", "tak", "liye", "doosri", "pehli"
    )

    private val urduScriptStopwords = setOf(
        "اور", "ہے", "ہیں", "تھا", "تھی", "تھے", "کے", "کی", "کا", "کو", "سے", "میں",
        "ایک", "وہ", "یہ", "پر", "نے", "کیا", "جب", "تب", "اب", "پھر", "بھی", "نہیں",
        "تو", "ہی", "کر", "رہا", "رہی", "رہے", "گیا", "گئی", "گئے", "ہوا", "ہوئی", "ہوئے",
        "اس", "اپنے", "اپنی", "اپنا", "سب", "کچھ", "بہت", "لیا", "دیا", "کہا", "کہ",
        "ہر", "طرف", "ساتھ", "بعد", "پہلے", "تک", "لیے", "دوسری", "پہلی", "والا", "والی",
        "والے", "کسی", "کون", "کیوں", "کہاں", "کب", "کیسے", "ابھی", "جبکہ", "اگر", "مگر",
        "لیکن", "یا", "نہ", "سی", "سا", "آ", "جا", "ہوتا", "ہوتی", "ہوتے", "پاس"
    )

    private val allStopwords = englishStopwords + romanUrduStopwords + urduScriptStopwords

    private val newStoryCues = listOf(
        "once upon a time", "meanwhile", "on the other hand", "chapter ", "story ", "part ",
        "in another", "far away", "years later", "ek din", "aik din", "ek zamane", "aik dafa",
        "doosri kahani", "nayi kahani", "doosri taraf", "دوسری طرف", "دوسری جانب",
        "ایک دن", "نئی کہانی", "دوسری کہانی", "ایک زمانے", "اس کے بعد", "---", "***"
    )

    private enum class ScriptKind {
        LATIN,
        ARABIC_URDU,
        OTHER
    }

    private data class UnitFeatures(
        val text: String,
        val hadBlankBefore: Boolean,
        val isDivider: Boolean,
        val allWords: List<String>,
        val contentWords: List<String>,
        val wordFreq: Map<String, Float>,
        val stemFreq: Map<String, Float>,
        val actorCandidates: Set<String>,
        val script: ScriptKind,
        val hasStoryCue: Boolean,
        val avgWordLength: Float,
        val hasDialogue: Boolean,
        val sentenceCount: Int
    )

    fun analyzeDocument(content: String): StoryDocumentAnalysis {
        if (content.isBlank()) {
            val emptyBlock = StoryBlock(
                id = "story_0",
                blockIndex = 0,
                rawText = content,
                separatorAfter = "",
                style = StoryPatternStyle.SIMPLE_TALE,
                metrics = StoryMetrics(
                    wordCount = 0,
                    uniqueWordCount = 0,
                    sentenceCount = 0,
                    guiraudTtr = 0f,
                    shannonEntropy = 0f,
                    avgWordLength = 0f,
                    coherenceScore = 1f,
                    driftPenalty = 0f,
                    wallColorIndex = 0f
                ),
                alignment = StoryAlignment.ON_PATTERN,
                alignmentExplanation = "Start writing a story in English, Urdu, or any language — the wall behind your words shifts automatically with pattern & drift.",
                anchorKeywords = emptyList(),
                driftedKeywords = emptyList(),
                segments = emptyList(),
                shiftedFromPrevious = false,
                previousStoryLabel = null
            )
            return StoryDocumentAnalysis(
                blocks = listOf(emptyBlock),
                overallComplexity = 0f,
                dominantStyle = StoryPatternStyle.SIMPLE_TALE,
                onPatternCount = 1,
                driftingCount = 0,
                storySplitCount = 0
            )
        }

        val rawLines = content.split("\n")
        val trailingEmptyBlock = rawLines.size >= 3 &&
            rawLines.last().isBlank() &&
            rawLines[rawLines.size - 2].isBlank()

        // Group lines into logical units (sentences/lines) preserving blank separators
        val units = buildUnits(rawLines)
        if (units.isEmpty()) {
            return analyzeDocument("")
        }

        // Segment into independent StoryBlocks using mathematical TF-IDF/Cosine + 3-gram Jaccard + Actor continuity
        data class MutableBlock(
            val units: MutableList<UnitFeatures> = mutableListOf(),
            val trailingBlanks: Int = 0,
            var shiftedFromLabel: String? = null
        )

        val rawBlocks = mutableListOf<MutableList<UnitFeatures>>()
        val blockShiftLabels = mutableListOf<String?>()
        val blockSeparators = mutableListOf<String>()

        var currentUnits = mutableListOf<UnitFeatures>()
        var currentShiftLabel: String? = null
        var consecutiveOffTopic = 0

        for (unit in units) {
            if (unit.isDivider) {
                if (currentUnits.isNotEmpty()) {
                    rawBlocks.add(currentUnits)
                    blockShiftLabels.add(currentShiftLabel)
                    blockSeparators.add("\n---\n")
                    currentUnits = mutableListOf()
                    currentShiftLabel = "Divider"
                    consecutiveOffTopic = 0
                }
                continue
            }

            if (currentUnits.isEmpty()) {
                currentUnits.add(unit)
                consecutiveOffTopic = 0
                continue
            }

            val anchorSummary = summarizeUnits(currentUnits)
            val coherence = computeCoherence(anchorSummary, unit)

            // Only split into a completely separate story block if the current block has established context
            // AND the new unit has enough content tokens to represent a real new story opening
            val establishedEnough = anchorSummary.totalContentWords >= 5
            val candidateLongEnough = (unit.contentWords.size >= 4) ||
                (unit.hadBlankBefore && unit.contentWords.size >= 3) ||
                (unit.hasStoryCue && unit.contentWords.size >= 2)

            val scriptChanged = anchorSummary.dominantScript != unit.script &&
                anchorSummary.dominantScript != ScriptKind.OTHER &&
                unit.script != ScriptKind.OTHER

            if (!unit.hadBlankBefore && coherence < 0.08f && unit.contentWords.size >= 4) {
                consecutiveOffTopic++
            } else {
                consecutiveOffTopic = 0
            }

            val isNewStory = establishedEnough && candidateLongEnough && (
                (unit.hadBlankBefore && coherence < 0.14f) ||
                    (unit.hasStoryCue && coherence < 0.24f) ||
                    (scriptChanged && coherence < 0.18f) ||
                    (consecutiveOffTopic >= 2)
                )

            if (isNewStory) {
                rawBlocks.add(currentUnits)
                blockShiftLabels.add(currentShiftLabel)
                blockSeparators.add(if (unit.hadBlankBefore) "\n\n" else "\n")

                val prevKeywords = anchorSummary.topKeywords.take(2).joinToString(", ")
                currentShiftLabel = if (prevKeywords.isNotBlank()) prevKeywords else "Story ${rawBlocks.size}"
                currentUnits = mutableListOf(unit)
                consecutiveOffTopic = 0
            } else {
                currentUnits.add(unit)
            }
        }

        if (currentUnits.isNotEmpty()) {
            rawBlocks.add(currentUnits)
            blockShiftLabels.add(currentShiftLabel)
            blockSeparators.add(if (trailingEmptyBlock) "\n\n" else "")
        }

        val analyzedBlocks = rawBlocks.mapIndexed { idx, blockUnits ->
            buildStoryBlock(
                index = idx,
                units = blockUnits,
                separatorAfter = blockSeparators.getOrElse(idx) { "" },
                shiftedFromPreviousLabel = blockShiftLabels.getOrNull(idx)
            )
        }.toMutableList()

        if (trailingEmptyBlock) {
            val nextIdx = analyzedBlocks.size
            analyzedBlocks.add(
                StoryBlock(
                    id = "story_$nextIdx",
                    blockIndex = nextIdx,
                    rawText = "",
                    separatorAfter = "",
                    style = StoryPatternStyle.SIMPLE_TALE,
                    metrics = StoryMetrics(0, 0, 0, 0f, 0f, 0f, 1f, 0f, 0f),
                    alignment = StoryAlignment.ON_PATTERN,
                    alignmentExplanation = "New isolated story block ready",
                    anchorKeywords = emptyList(),
                    driftedKeywords = emptyList(),
                    segments = emptyList(),
                    shiftedFromPrevious = false,
                    previousStoryLabel = null
                )
            )
        }

        val nonEmpty = analyzedBlocks.filter { it.rawText.isNotBlank() }
        val overallComplexity = if (nonEmpty.isEmpty()) {
            0f
        } else {
            (nonEmpty.sumOf { it.metrics.wallColorIndex.toDouble() }.toFloat() / nonEmpty.size)
                .coerceIn(0f, 1f)
        }

        val dominantStyle = nonEmpty
            .maxByOrNull { it.metrics.wordCount }
            ?.style ?: StoryPatternStyle.SIMPLE_TALE

        return StoryDocumentAnalysis(
            blocks = analyzedBlocks,
            overallComplexity = overallComplexity,
            dominantStyle = dominantStyle,
            onPatternCount = nonEmpty.count { it.alignment == StoryAlignment.ON_PATTERN },
            driftingCount = nonEmpty.count { it.alignment == StoryAlignment.SLIGHT_DRIFT },
            storySplitCount = analyzedBlocks.count { it.shiftedFromPrevious }
        )
    }

    private fun buildUnits(rawLines: List<String>): List<UnitFeatures> {
        val units = mutableListOf<UnitFeatures>()
        var blankAccumulator = 0

        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                blankAccumulator++
                continue
            }
            if (trimmed == "---" || trimmed == "***" || trimmed == "===") {
                units.add(
                    UnitFeatures(
                        text = trimmed,
                        hadBlankBefore = blankAccumulator > 0,
                        isDivider = true,
                        allWords = emptyList(),
                        contentWords = emptyList(),
                        wordFreq = emptyMap(),
                        stemFreq = emptyMap(),
                        actorCandidates = emptySet(),
                        script = ScriptKind.OTHER,
                        hasStoryCue = true,
                        avgWordLength = 0f,
                        hasDialogue = false,
                        sentenceCount = 0
                    )
                )
                blankAccumulator = 0
                continue
            }

            // Split long single lines into individual sentences so mid-line story shifts also split cleanly
            val subSentences = splitSentencesPreservingText(line)
            subSentences.forEachIndexed { subIdx, seg ->
                if (seg.isNotBlank()) {
                    val hadBlank = subIdx == 0 && blankAccumulator > 0
                    units.add(extractFeatures(seg, hadBlankBefore = hadBlank))
                }
            }
            blankAccumulator = 0
        }
        return units
    }

    private fun splitSentencesPreservingText(line: String): List<String> {
        // Split only at clear sentence terminators followed by space and another clause
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            sb.append(ch)
            val isTerminator = ch == '.' || ch == '!' || ch == '?' || ch == '۔' || ch == '؟'
            if (isTerminator && i + 1 < line.length && line[i + 1] == ' ') {
                // Skip decimal numbers like 3.5
                val prevDigit = i > 0 && line[i - 1].isDigit()
                val nextDigit = i + 2 < line.length && line[i + 2].isDigit()
                if (!prevDigit || !nextDigit) {
                    result.add(sb.toString().trim())
                    sb.clear()
                    i++ // consume separating space
                }
            }
            i++
        }
        if (sb.isNotBlank()) {
            result.add(sb.toString().trim())
        }
        return if (result.isEmpty()) listOf(line) else result
    }

    private fun extractFeatures(text: String, hadBlankBefore: Boolean): UnitFeatures {
        val rawTokens = tokenizeUnicodeWords(text)
        val normalizedWords = rawTokens.map { it.lowercase() }
        val contentWords = normalizedWords.filter { it.length >= 2 && it !in allStopwords }

        val wordFreq = mutableMapOf<String, Float>()
        contentWords.forEach { w ->
            wordFreq[w] = (wordFreq[w] ?: 0f) + 1f
        }
        // Sublinear TF weighting: 1 + ln(tf)
        wordFreq.replaceAll { _, tf -> 1f + ln(tf) }

        val stemFreq = mutableMapOf<String, Float>()
        val wordsForStems = if (contentWords.isNotEmpty()) contentWords else normalizedWords
        for (w in wordsForStems) {
            val stem = stemToken(w)
            stemFreq[stem] = (stemFreq[stem] ?: 0f) + 1f
        }
        stemFreq.replaceAll { _, tf -> 1f + ln(tf) }

        // Actor/proper-noun heuristic: capitalized words mid-sentence OR longer nouns
        val actors = mutableSetOf<String>()
        rawTokens.forEachIndexed { idx, tok ->
            val lower = tok.lowercase()
            if (lower !in allStopwords && tok.length >= 3) {
                if (idx > 0 && tok[0].isUpperCase()) {
                    actors.add(lower)
                } else if (tok.length >= 5) {
                    actors.add(lower)
                }
            }
        }

        val lowerText = text.lowercase()
        val hasCue = newStoryCues.any { lowerText.contains(it) }
        val script = detectScript(text)
        val avgLen = if (normalizedWords.isEmpty()) 0f
        else normalizedWords.sumOf { it.length }.toFloat() / normalizedWords.size
        val hasDialogue = text.contains('"') || text.contains('“') || text.contains('”') || text.contains(':')

        return UnitFeatures(
            text = text,
            hadBlankBefore = hadBlankBefore,
            isDivider = false,
            allWords = normalizedWords,
            contentWords = contentWords,
            wordFreq = wordFreq,
            stemFreq = stemFreq,
            actorCandidates = actors,
            script = script,
            hasStoryCue = hasCue,
            avgWordLength = avgLen,
            hasDialogue = hasDialogue,
            sentenceCount = 1
        )
    }

    private fun stemToken(word: String): String {
        if (word.length <= 3) return word
        // Urdu / Arabic script plural & suffix normalization
        val isArabicScript = word.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC }
        if (isArabicScript) {
            val urduSuffixes = listOf("یاں", "وں", "یں", "ے", "ی")
            for (suffix in urduSuffixes) {
                if (word.endsWith(suffix) && word.length - suffix.length >= 2) {
                    return word.dropLast(suffix.length)
                }
            }
            return word
        }
        // Latin / English / Roman Urdu inflection trimming
        return when {
            word.endsWith("ies") && word.length > 5 -> word.dropLast(3) + "y"
            word.endsWith("ing") && word.length > 5 -> word.dropLast(3)
            word.endsWith("ed") && word.length > 4 -> word.dropLast(2)
            word.endsWith("ly") && word.length > 4 -> word.dropLast(2)
            word.endsWith("es") && word.length > 4 -> word.dropLast(2)
            word.endsWith("s") && word.length > 4 -> word.dropLast(1)
            else -> word
        }
    }

    private data class AnchorSummary(
        val wordFreq: Map<String, Float>,
        val stemFreq: Map<String, Float>,
        val actors: Set<String>,
        val totalContentWords: Int,
        val dominantScript: ScriptKind,
        val topKeywords: List<String>
    )

    private fun summarizeUnits(units: List<UnitFeatures>): AnchorSummary {
        val mergedWords = mutableMapOf<String, Float>()
        val mergedStems = mutableMapOf<String, Float>()
        val actorCounts = mutableMapOf<String, Int>()
        var totalContent = 0
        var latin = 0
        var arabic = 0

        for (u in units) {
            totalContent += u.contentWords.size
            u.wordFreq.forEach { (k, v) -> mergedWords[k] = (mergedWords[k] ?: 0f) + v }
            u.stemFreq.forEach { (k, v) -> mergedStems[k] = (mergedStems[k] ?: 0f) + v }
            u.actorCandidates.forEach { a -> actorCounts[a] = (actorCounts[a] ?: 0) + 1 }
            when (u.script) {
                ScriptKind.LATIN -> latin++
                ScriptKind.ARABIC_URDU -> arabic++
                ScriptKind.OTHER -> Unit
            }
        }

        val dominantScript = when {
            arabic > latin -> ScriptKind.ARABIC_URDU
            latin > 0 -> ScriptKind.LATIN
            else -> ScriptKind.OTHER
        }

        val topKeywords = mergedWords.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        return AnchorSummary(
            wordFreq = mergedWords,
            stemFreq = mergedStems,
            actors = actorCounts.keys,
            totalContentWords = totalContent,
            dominantScript = dominantScript,
            topKeywords = topKeywords
        )
    }

    private fun computeCoherence(anchor: AnchorSummary, candidate: UnitFeatures): Float {
        if (anchor.wordFreq.isEmpty() || candidate.wordFreq.isEmpty()) return 1f

        val wordCos = cosineSimilarity(anchor.wordFreq, candidate.wordFreq)
        val stemCos = cosineSimilarity(anchor.stemFreq, candidate.stemFreq)
        val actorUnion = anchor.actors.union(candidate.actorCandidates)
        val actorJaccard = if (actorUnion.isEmpty()) 0f
        else anchor.actors.intersect(candidate.actorCandidates).size.toFloat() / actorUnion.size

        val hasExactOverlap = anchor.wordFreq.keys.any { it in candidate.wordFreq }
        val hasStemOverlap = !hasExactOverlap && anchor.stemFreq.keys.any { it in candidate.stemFreq }
        val overlapBonus = when {
            hasExactOverlap -> 0.14f
            hasStemOverlap -> 0.08f
            else -> 0f
        }

        // Blend stem TF-IDF cosine, word cosine, actor overlap, and lexical continuity bonus
        return (0.45f * wordCos + 0.35f * stemCos + 0.20f * actorJaccard + overlapBonus)
            .coerceIn(0f, 1f)
    }

    private fun buildStoryBlock(
        index: Int,
        units: List<UnitFeatures>,
        separatorAfter: String,
        shiftedFromPreviousLabel: String?
    ): StoryBlock {
        // Reconstruct block text preserving blank lines or sentence spacing cleanly
        val rebuiltText = buildString {
            units.forEachIndexed { idx, u ->
                if (idx > 0) {
                    append(if (u.hadBlankBefore) "\n\n" else " ")
                }
                append(u.text)
            }
        }

        val allWords = units.flatMap { it.allWords }
        val contentWords = units.flatMap { it.contentWords }
        val wordCount = allWords.size
        val uniqueWords = allWords.toSet().size

        // 1. Guiraud's Root Type-Token Ratio: V / sqrt(N)
        val guiraud = if (wordCount > 0) uniqueWords.toFloat() / sqrt(wordCount.toFloat()) else 0f
        val normalizedGuiraud = ((guiraud - 1.2f) / 4.2f).coerceIn(0f, 1f)

        // 2. Normalized Shannon Lexical Entropy: H = -sum(p * log2(p)) / log2(N)
        val shannon = computeNormalizedShannonEntropy(allWords)

        // 3. Syntactic & Punctuation Complexity
        val avgWordLen = if (allWords.isEmpty()) 0f else allWords.sumOf { it.length }.toFloat() / allWords.size
        val lenScore = ((avgWordLen - 3.2f) / 3.8f).coerceIn(0f, 1f)
        val clausePunctCount = rebuiltText.count { it in setOf(',', ';', ':', '—', '(', ')', '?', '!') }
        val punctDensity = if (wordCount > 0) (clausePunctCount.toFloat() / wordCount).coerceIn(0f, 0.5f) * 2f else 0f

        // 4. Within-block Narrative Drift Analysis (anchor = opening unit(s) of this story block)
        val segments = mutableListOf<SegmentAnalysis>()
        val anchorBuilder = mutableListOf<UnitFeatures>()
        var minCoherence = 1f
        val driftedTokens = mutableSetOf<String>()
        var driftDetected = false

        units.forEachIndexed { uIdx, unit ->
            if (uIdx == 0) {
                anchorBuilder.add(unit)
                segments.add(
                    SegmentAnalysis(
                        segmentIndex = 0,
                        rawText = unit.text,
                        coherenceWithAnchor = 1f,
                        alignment = StoryAlignment.ON_PATTERN,
                        explanation = "Story anchor established",
                        topKeywords = unit.wordFreq.entries.sortedByDescending { it.value }.take(3).map { it.key }
                    )
                )
            } else {
                val anchor = summarizeUnits(anchorBuilder)
                val coh = computeCoherence(anchor, unit)
                minCoherence = min(minCoherence, coh)

                val isDrifting = unit.contentWords.size >= 3 && coh < 0.18f
                if (isDrifting) {
                    driftDetected = true
                    val anchorWords = anchor.wordFreq.keys
                    driftedTokens.addAll(unit.contentWords.filter { it !in anchorWords }.take(4))
                } else {
                    // Gently reinforce anchor only from on-pattern sentences
                    anchorBuilder.add(unit)
                }

                val segAlignment = if (isDrifting) StoryAlignment.SLIGHT_DRIFT else StoryAlignment.ON_PATTERN
                val segNote = if (isDrifting) {
                    "Coherence dropped to ${(coh * 100).toInt()}% — drifting away from (${anchor.topKeywords.take(3).joinToString(", ")})"
                } else {
                    "Follows story pattern (${(coh * 100).toInt()}% coherence)"
                }

                segments.add(
                    SegmentAnalysis(
                        segmentIndex = uIdx,
                        rawText = unit.text,
                        coherenceWithAnchor = coh,
                        alignment = segAlignment,
                        explanation = segNote,
                        topKeywords = unit.wordFreq.entries.sortedByDescending { it.value }.take(3).map { it.key }
                    )
                )
            }
        }

        val avgCoherence = if (segments.isEmpty()) 1f
        else segments.sumOf { it.coherenceWithAnchor.toDouble() }.toFloat() / segments.size

        // Drift penalty pushes wall color darker/warmer whenever the writer wanders from the story!
        val driftPenalty = if (driftDetected) {
            ((0.35f - minCoherence).coerceIn(0.10f, 0.35f) * 1.35f).coerceIn(0.20f, 0.48f)
        } else if (units.size > 1 && avgCoherence < 0.32f) {
            ((0.32f - avgCoherence) * 0.5f).coerceIn(0f, 0.12f)
        } else {
            0f
        }

        // Base story complexity + drift shift -> drives wall color behind the words (Light Beige -> Deep Charcoal)
        val structuralComplexity = (
            normalizedGuiraud * 0.34f +
                shannon * 0.28f +
                lenScore * 0.22f +
                punctDensity * 0.16f
            ).coerceIn(0f, 1f)

        val wallColorIndex = (structuralComplexity * 0.72f + driftPenalty).coerceIn(0.04f, 1.0f)

        val anchorSummary = summarizeUnits(anchorBuilder.ifEmpty { units })
        val style = classifyStoryStyle(units, structuralComplexity, anchorSummary.dominantScript)
        val alignment = if (driftDetected) StoryAlignment.SLIGHT_DRIFT else StoryAlignment.ON_PATTERN

        val explanation = buildBlockExplanation(
            style = style,
            alignment = alignment,
            anchorKeywords = anchorSummary.topKeywords,
            driftedKeywords = driftedTokens.toList(),
            coherence = avgCoherence,
            shiftedFromLabel = shiftedFromPreviousLabel
        )

        val metrics = StoryMetrics(
            wordCount = wordCount,
            uniqueWordCount = uniqueWords,
            sentenceCount = units.size,
            guiraudTtr = guiraud,
            shannonEntropy = shannon,
            avgWordLength = avgWordLen,
            coherenceScore = avgCoherence,
            driftPenalty = driftPenalty,
            wallColorIndex = wallColorIndex
        )

        return StoryBlock(
            id = "story_$index",
            blockIndex = index,
            rawText = rebuiltText,
            separatorAfter = separatorAfter,
            style = style,
            metrics = metrics,
            alignment = alignment,
            alignmentExplanation = explanation,
            anchorKeywords = anchorSummary.topKeywords,
            driftedKeywords = driftedTokens.toList(),
            segments = segments,
            shiftedFromPrevious = shiftedFromPreviousLabel != null,
            previousStoryLabel = shiftedFromPreviousLabel
        )
    }

    private fun classifyStoryStyle(
        units: List<UnitFeatures>,
        complexity: Float,
        script: ScriptKind
    ): StoryPatternStyle {
        val dialogueCount = units.count { it.hasDialogue }
        val hasActors = units.sumOf { it.actorCandidates.size } >= 2
        return when {
            script == ScriptKind.ARABIC_URDU -> StoryPatternStyle.MULTILINGUAL_VERSE
            dialogueCount >= 1 && dialogueCount >= units.size / 2 -> StoryPatternStyle.DIALOGUE_SCENE
            complexity >= 0.62f -> StoryPatternStyle.DRAMATIC_INTENSE
            hasActors && complexity < 0.45f -> StoryPatternStyle.CHARACTER_STORY
            complexity >= 0.36f -> StoryPatternStyle.DESCRIPTIVE_PROSE
            else -> StoryPatternStyle.SIMPLE_TALE
        }
    }

    private fun buildBlockExplanation(
        style: StoryPatternStyle,
        alignment: StoryAlignment,
        anchorKeywords: List<String>,
        driftedKeywords: List<String>,
        coherence: Float,
        shiftedFromLabel: String?
    ): String {
        val themeStr = anchorKeywords.take(3).joinToString(", ").ifEmpty { "opening tone" }
        val shiftPrefix = if (shiftedFromLabel != null) {
            "New story separated from [$shiftedFromLabel]. "
        } else {
            ""
        }

        return when (alignment) {
            StoryAlignment.SLIGHT_DRIFT -> {
                val driftStr = driftedKeywords.take(3).joinToString(", ").ifEmpty { "off-theme words" }
                "${shiftPrefix}Drifting from story theme ($themeStr) toward [$driftStr] — wall color shifted."
            }
            StoryAlignment.ON_PATTERN -> {
                "${shiftPrefix}Inside ${style.displayName} pattern ($themeStr) · ${(coherence * 100).toInt()}% coherence."
            }
            StoryAlignment.MAJOR_SHIFT -> {
                "${shiftPrefix}New story boundary detected."
            }
        }
    }

    private fun computeNormalizedShannonEntropy(tokens: List<String>): Float {
        if (tokens.size <= 1) return 0f
        val counts = mutableMapOf<String, Int>()
        tokens.forEach { t -> counts[t] = (counts[t] ?: 0) + 1 }
        val n = tokens.size.toDouble()
        var entropy = 0.0
        for (c in counts.values) {
            val p = c / n
            entropy -= p * (ln(p) / ln(2.0))
        }
        val maxEntropy = ln(n) / ln(2.0)
        return if (maxEntropy > 0.0) (entropy / maxEntropy).toFloat().coerceIn(0f, 1f) else 0f
    }

    private fun cosineSimilarity(a: Map<String, Float>, b: Map<String, Float>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for ((k, v) in a) {
            normA += v * v
            val bv = b[k]
            if (bv != null) dot += v * bv
        }
        for (v in b.values) {
            normB += v * v
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 1e-6f) (dot / denom).coerceIn(0f, 1f) else 0f
    }

    private fun tokenizeUnicodeWords(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val charCount = Character.charCount(cp)
            if (Character.isLetterOrDigit(cp) || cp == '\''.code || cp == '’'.code) {
                sb.appendCodePoint(cp)
            } else if (sb.isNotEmpty()) {
                tokens.add(sb.toString())
                sb.clear()
            }
            i += charCount
        }
        if (sb.isNotEmpty()) tokens.add(sb.toString())
        return tokens
    }

    private fun detectScript(text: String): ScriptKind {
        var latin = 0
        var arabic = 0
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            if (Character.isLetter(cp)) {
                val block = Character.UnicodeBlock.of(cp)
                when (block) {
                    Character.UnicodeBlock.BASIC_LATIN,
                    Character.UnicodeBlock.LATIN_1_SUPPLEMENT,
                    Character.UnicodeBlock.LATIN_EXTENDED_A -> latin++
                    Character.UnicodeBlock.ARABIC,
                    Character.UnicodeBlock.ARABIC_SUPPLEMENT,
                    Character.UnicodeBlock.ARABIC_EXTENDED_A -> arabic++
                    null -> Unit
                    else -> Unit
                }
            }
            i += Character.charCount(cp)
        }
        return when {
            arabic > latin -> ScriptKind.ARABIC_URDU
            latin > 0 -> ScriptKind.LATIN
            else -> ScriptKind.OTHER
        }
    }
}
