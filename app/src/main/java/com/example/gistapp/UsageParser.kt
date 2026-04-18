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
    fun parseData(data: String): List<UsageRecord> {
        val records = mutableListOf<UsageRecord>()
        
        // Ensure we only process lines with actual content
        val lines = data.lines().filter { it.isNotBlank() }
        
        // Prepare for both 2-digit and 4-digit years just in case
        val format2Digit = SimpleDateFormat("dd MMM yy hh:mm:ss a", Locale.US)
        val format4Digit = SimpleDateFormat("dd MMM yyyy hh:mm:ss a", Locale.US)

        for (line in lines) {
            // Smart Split: Handles proper CSV commas, but falls back to tabs if the file is Tab-Separated
            val parts = if (line.contains(",")) {
                line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.replace("\"", "").trim() }
            } else {
                line.split("\\s{2,}|\t".toRegex()).map { it.trim() }
            }
            
            // Ensure we have at least 5 columns and skip the Header row
            if (parts.size >= 5 && !parts[0].equals("Device name", ignoreCase = true) && !parts[0].equals("Device", ignoreCase = true)) {
                try {
                    val device = parts[0]
                    val app = getFriendlyAppName(parts[1])
                    val date = parts[2]
                    val startTime = parts[3]
                    val endTime = parts[4]

                    val startStr = "$date $startTime"
                    val endStr = "$date $endTime"
                    
                    var startDate = format2Digit.parse(startStr)
                    var endDate = format2Digit.parse(endStr)
                    
                    // Fallback to 4-digit year format if parsing failed
                    if (startDate == null) startDate = format4Digit.parse(startStr)
                    if (endDate == null) endDate = format4Digit.parse(endStr)
                    
                    val duration = if (startDate != null && endDate != null) {
                        (endDate.time - startDate.time) / 1000 // Convert milliseconds to seconds
                    } else 0L

                    records.add(
                        UsageRecord(
                            device = device,
                            app = app,
                            date = date,
                            startTime = startTime,
                            endTime = endTime,
                            durationSeconds = maxOf(0L, duration) // Ensure we never log negative time
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
            "com.google.android.apps.messaging" -> "SMS"
            "com.google.android.dialer" -> "Phone"
            "com.android.systemui" -> "System UI"
            else -> {
                val parts = packageName.split(".")
                if (parts.isNotEmpty()) parts.last().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } else packageName
            }
        }
    }
}
