package com.company.chamberly.utils

import android.text.InputFilter
import android.text.Spanned

class MaxLinesInputFilter(private val maxLines: Int) : InputFilter {
    override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence? {
        if (source == null || dest == null) return null

        // Combine the existing text with the new input
        val newText = StringBuilder(dest).insert(dstart, source, start, end).toString()
        val newLineCount = newText.count { it == '\n' }

        // Check if the new text exceeds the max lines
        if (newLineCount >= maxLines) {
            // Determine how many lines are currently present
            val currentLineCount = dest.count { it == '\n' }

            // If the current lines are already at max, reject the input
            if (currentLineCount >= maxLines - 1) {
                return ""
            }

            // Otherwise, accept part of the input until the max lines are reached
            val excessLines = newLineCount - (maxLines - 1)
            var linesAdded = 0

            for (i in start until end) {
                if (source[i] == '\n') {
                    linesAdded++
                    if (linesAdded >= excessLines) {
                        return source.subSequence(start, i)
                    }
                }
            }
        }
        return null
    }
}

