package com.example.gistapp

import java.text.SimpleDateFormat
import java.util.Locale

data class UsageRecord(
    val device: String,
    val app: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationSeconds: Long
)

object UsageParser {
    fun parseData(rawCsv: String): List<UsageRecord> {
        val records = mutableListOf<UsageRecord>()
        val lines = rawCsv.lines().filter { it.isNotBlank() }
        
        val format = SimpleDateFormat("dd MMM yy hh:mm:ss a", Locale.US)

        for (line in lines) {
            // Split by tabs or multiple spaces (handles both CSV and table structures)
            val parts = line.split("\\s{2,}|\t|,".toRegex()).map { it.trim() }
            if (parts.size >= 5) {
                try {
                    val startStr = "${parts[2]} ${parts[3]}"
                    val endStr = "${parts[2]} ${parts[4]}"
                    
                    val startDate = format.parse(startStr)
                    val endDate = format.parse(endStr)
                    
                    val duration = if (startDate != null && endDate != null) {
                        (endDate.time - startDate.time) / 1000 // duration in seconds
                    } else 0L

                    records.add(
                        UsageRecord(
                            device = parts[0],
                            app = getFriendlyAppName(parts[1]),
                            date = parts[2],
                            startTime = parts[3],
                            endTime = parts[4],
                            durationSeconds = maxOf(0L, duration) // ensure no negative time
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return records
    }

    private fun getFriendlyAppName(packageName: String): String {
        return when (packageName.lowercase()) {
            "com.coloros.filemanager" -> "File Manager"
            "com.oppo.launcher" -> "System Launcher"
            "org.mozilla.firefox" -> "Firefox"
            "com.example.gistapp" -> "Live Notify"
            "com.android.settings" -> "Settings"
            "com.whatsapp" -> "WhatsApp"
            "com.google.android.youtube" -> "YouTube"
            else -> {
                val parts = packageName.split(".")
                if (parts.isNotEmpty()) parts.last().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } else packageName
            }
        }
    }
}
