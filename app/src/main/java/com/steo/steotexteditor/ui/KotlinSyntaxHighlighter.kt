package com.steo.steotexteditor.ui

import android.graphics.Color

class KotlinSyntaxHighlighter {

    fun buildSpans(text: String): List<SyntaxHighlightSpan> {
        val spans = mutableListOf<SyntaxHighlightSpan>()

        addColorSpans(spans, KEYWORDS_REGEX, text, COLOR_KEYWORD)
        addColorSpans(spans, NUMBERS_REGEX, text, COLOR_NUMBER)
        addColorSpans(spans, ANNOTATION_REGEX, text, COLOR_ANNOTATION)
        addColorSpans(spans, STRINGS_REGEX, text, COLOR_STRING)
        addColorSpans(spans, SINGLE_LINE_COMMENT_REGEX, text, COLOR_COMMENT)
        addColorSpans(spans, MULTI_LINE_COMMENT_REGEX, text, COLOR_COMMENT)

        return spans
    }

    private fun addColorSpans(
        destination: MutableList<SyntaxHighlightSpan>,
        regex: Regex,
        text: String,
        color: Int
    ) {
        regex.findAll(text).forEach { match ->
            destination.add(
                SyntaxHighlightSpan(
                    start = match.range.first,
                    end = match.range.last + 1,
                    foregroundColor = color
                )
            )
        }
    }

    companion object {
        private val COLOR_KEYWORD = Color.parseColor("#CC99CD")
        private val COLOR_STRING = Color.parseColor("#CE9178")
        private val COLOR_COMMENT = Color.parseColor("#6A9955")
        private val COLOR_NUMBER = Color.parseColor("#B5CEA8")
        private val COLOR_ANNOTATION = Color.parseColor("#DCDCAA")

        private val KEYWORDS = listOf(
            "val", "var", "fun", "class", "object", "interface", "if", "else", "when", "for",
            "while", "do", "return", "true", "false", "null", "this", "super", "import",
            "package", "override", "private", "public", "protected", "internal", "companion",
            "data", "sealed", "abstract", "open", "final", "lateinit", "lazy", "by", "in",
            "is", "as", "try", "catch", "finally", "throw", "constructor", "init", "get", "set",
            "suspend", "coroutine"
        )

        private val KEYWORDS_REGEX =
            Regex("\\b(${KEYWORDS.joinToString("|") { Regex.escape(it) }})\\b")
        private val STRINGS_REGEX = Regex("\"(?:\\\\.|[^\"\\\\])*\"")
        private val SINGLE_LINE_COMMENT_REGEX = Regex("//.*$", setOf(RegexOption.MULTILINE))
        private val MULTI_LINE_COMMENT_REGEX = Regex("/\\*[\\s\\S]*?\\*/")
        private val NUMBERS_REGEX = Regex("\\b\\d+(?:\\.\\d+)?\\b")
        private val ANNOTATION_REGEX = Regex("@[A-Za-z_][A-Za-z0-9_]*")
    }
}

