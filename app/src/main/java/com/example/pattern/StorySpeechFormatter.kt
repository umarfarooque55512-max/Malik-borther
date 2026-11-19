package com.example.pattern

object StorySpeechFormatter {

    fun formatNoteForSpeech(title: String, blocks: List<StoryBlock>): String {
        val nonBlank = blocks.filter { it.rawText.isNotBlank() }
        if (nonBlank.isEmpty()) return title.ifBlank { "Empty note." }

        val sb = StringBuilder()
        if (title.isNotBlank()) {
            sb.append(title).append(". ")
        }
        nonBlank.forEachIndexed { idx, block ->
            if (nonBlank.size > 1) {
                sb.append("Story block ").append(idx + 1).append(": ")
            }
            sb.append(block.rawText.trim()).append(". ")
        }
        return sb.toString().trim()
    }

    fun formatBlockForSpeech(block: StoryBlock): String {
        return block.rawText.trim()
    }
}
