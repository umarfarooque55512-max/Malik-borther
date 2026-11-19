package com.example

import com.example.pattern.StoryAlignment
import com.example.pattern.StoryPatternAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryPatternAnalyzerTest {

    @Test
    fun coherentSimpleStoryStaysOnPatternWithLightBeigeWall() {
        val story = "The little cat sat on a warm mat. The cat liked the warm sun on the mat."
        val analysis = StoryPatternAnalyzer.analyzeDocument(story)

        assertEquals(1, analysis.blocks.size)
        val block = analysis.blocks.first()
        assertEquals(StoryAlignment.ON_PATTERN, block.alignment)
        assertTrue(block.metrics.wallColorIndex < 0.35f)
    }

    @Test
    fun slightNarrativeDriftShiftsWallColorAndFlagsDrift() {
        val coherentStory = "Sara walked along the quiet pine forest trail listening to birds sing. The morning dew sparkled on green ferns beside the wooden forest bridge."
        val driftingStory = "$coherentStory Suddenly quarterly tax audits, corporate spreadsheets, and brokerage fees interrupted."

        val cleanWall = StoryPatternAnalyzer.analyzeDocument(coherentStory).blocks.first().metrics.wallColorIndex
        val driftedAnalysis = StoryPatternAnalyzer.analyzeDocument(driftingStory)

        assertEquals(1, driftedAnalysis.blocks.size)
        val block = driftedAnalysis.blocks.first()
        assertEquals(StoryAlignment.SLIGHT_DRIFT, block.alignment)
        assertTrue(block.metrics.wallColorIndex > cleanWall + 0.12f)
    }

    @Test
    fun completelyNewStorySplitsIntoSeparateBlock() {
        val twoStories = """
            Every morning, old Tariq baked warm bread in his quiet clay oven beside the river. Children smelled the sweet loaves from the village lane and ran to greet him.

            Meanwhile, Commander Elena calibrated the plasma thrusters aboard Orion Seven while asteroids rattled the titanium hull near Jupiter.
        """.trimIndent()

        val analysis = StoryPatternAnalyzer.analyzeDocument(twoStories)
        assertEquals(2, analysis.blocks.size)
        assertTrue(analysis.blocks[1].shiftedFromPrevious)
        assertEquals(1, analysis.storySplitCount)
    }

    @Test
    fun urduMultilingualStoriesSeparateCleanly() {
        val urduDualStory = """
            ایک دن ایک چھوٹی سی چڑیا باغ میں درخت پر بیٹھ کر میٹھے گیت گا رہی تھی۔ پھولوں کی خوشبو ہر طرف پھیلی ہوئی تھی۔

            دوسری طرف سمندر کی گہرائی میں ایک آبدوز تاریک پانیوں کو چیرتی ہوئی آگے بڑھ رہی تھی اور ریڈار پر عجیب سگنل آ رہے تھے۔
        """.trimIndent()

        val analysis = StoryPatternAnalyzer.analyzeDocument(urduDualStory)
        assertEquals(2, analysis.blocks.size)
        assertTrue(analysis.blocks[1].shiftedFromPrevious)
    }
}
