package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object Formatters {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val unit = 1024.0
        if (bytes < unit) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(unit)).toInt().coerceAtMost(4)
        val prefix = "KMGT"[exp - 1]
        val value = bytes / unit.pow(exp.toDouble())
        return if (value >= 100.0) {
            String.format(Locale.US, "%.0f %sB", value, prefix)
        } else if (value >= 10.0) {
            String.format(Locale.US, "%.1f %sB", value, prefix)
        } else {
            String.format(Locale.US, "%.2f %sB", value, prefix)
        }
    }

    fun formatDate(millis: Long): String {
        if (millis <= 0L) return "Unknown date"
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US)
        return formatter.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        if (millis <= 0L) return ""
        val formatter = SimpleDateFormat("MMM d • HH:mm", Locale.US)
        return formatter.format(Date(millis))
    }

    fun formatCount(count: Int): String {
        return String.format(Locale.US, "%,d", count)
    }
}
