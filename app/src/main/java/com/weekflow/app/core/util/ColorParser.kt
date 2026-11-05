package com.weekflow.app.core.util

import androidx.compose.ui.graphics.Color
import com.weekflow.app.core.designsystem.IosBlue

object ColorParser {

    fun parseHexColor(hex: String?, fallback: Color = IosBlue): Color {
        if (hex.isNullOrBlank()) return fallback
        var clean = hex.trim()
        if (clean.startsWith("#")) {
            clean = clean.substring(1)
        }
        return try {
            when (clean.length) {
                6 -> {
                    val r = clean.substring(0, 2).toInt(16)
                    val g = clean.substring(2, 4).toInt(16)
                    val b = clean.substring(4, 6).toInt(16)
                    Color(r, g, b)
                }
                8 -> {
                    val a = clean.substring(0, 2).toInt(16)
                    val r = clean.substring(2, 4).toInt(16)
                    val g = clean.substring(4, 6).toInt(16)
                    val b = clean.substring(6, 8).toInt(16)
                    Color(r, g, b, a)
                }
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }
}
