package com.steo.steotexteditor.ui

import android.graphics.Color
import android.graphics.Typeface

class MarkdownSyntaxHighlighter {

    fun buildSpans(text: String): List<SyntaxHighlightSpan> {
        val spans = mutableListOf<SyntaxHighlightSpan>()

        addColorSpans(
            destination = spans,
            regex = HEADERS_REGEX,
            text = text,
            color = COLOR_HEADER
        )
        addColorSpans(
            destination = spans,
            regex = BOLD_REGEX,
            text = text,
            color = COLOR_BOLD
        )
        addStyledColorSpans(
            destination = spans,
            regex = ITALIC_REGEX,
            text = text,
            color = COLOR_ITALIC,
            style = Typeface.ITALIC
        )
        addSpans(
            destination = spans,
            regex = INLINE_CODE_REGEX,
            text = text,
            foregroundColor = COLOR_INLINE_CODE,
            useMonospace = true
        )
        addColorSpans(spans, LINKS_REGEX, text, COLOR_LINK)
        addColorSpans(spans, UNORDERED_LIST_REGEX, text, COLOR_LIST_ITEM)
        addSpans(
            destination = spans,
            regex = CODE_BLOCK_REGEX,
            text = text,
            backgroundColor = COLOR_CODE_BLOCK_BG,
            useMonospace = true
        )

        return spans
    }

    private fun addColorSpans(
        destination: MutableList<SyntaxHighlightSpan>,
        regex: Regex,
        text: String,
        color: Int
    ) {
        addSpans(destination, regex, text, foregroundColor = color)
    }

    private fun addStyledColorSpans(
        destination: MutableList<SyntaxHighlightSpan>,
        regex: Regex,
        text: String,
        color: Int,
        style: Int
    ) {
        addSpans(destination, regex, text, foregroundColor = color, style = style)
    }

    private fun addSpans(
        destination: MutableList<SyntaxHighlightSpan>,
        regex: Regex,
        text: String,
        foregroundColor: Int? = null,
        style: Int? = null,
        backgroundColor: Int? = null,
        useMonospace: Boolean = false
    ) {
        regex.findAll(text).forEach { match ->
            destination.add(
                SyntaxHighlightSpan(
                    start = match.range.first,
                    end = match.range.last + 1,
                    foregroundColor = foregroundColor,
                    style = style,
                    backgroundColor = backgroundColor,
                    monospace = useMonospace
                )
            )
        }
    }

    companion object {
        private val COLOR_HEADER = Color.parseColor("#569CD6")
        private val COLOR_BOLD = Color.parseColor("#DCDCAA")
        private val COLOR_ITALIC = Color.parseColor("#CE9178")
        private val COLOR_INLINE_CODE = Color.parseColor("#CE9178")
        private val COLOR_CODE_BLOCK_BG = Color.parseColor("#1E1E1E")
        private val COLOR_LINK = Color.parseColor("#4EC9B0")
        private val COLOR_LIST_ITEM = Color.parseColor("#DCDCF0")

        private val HEADERS_REGEX = Regex("(?m)^#{1,3}\\s+.+$")
        private val BOLD_REGEX = Regex("(\\*\\*[^*\\n]+\\*\\*)|(__[^_\\n]+__)")
        private val ITALIC_REGEX = Regex("(?<!\\*)\\*[^*\\n]+\\*(?!\\*)|(?<!_)_[^_\\n]+_(?!_)")
        private val INLINE_CODE_REGEX = Regex("`[^`\\n]+`")
        private val CODE_BLOCK_REGEX = Regex("```[\\s\\S]*?```")
        private val LINKS_REGEX = Regex("\\[[^\\]]+\\]\\([^\\)]+\\)")
        private val UNORDERED_LIST_REGEX = Regex("(?m)^\\s*[-*]\\s+.+$")
    }
}

