package com.example.gistapp

import java.text.SimpleDateFormat
import java.util.Date
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
        val lines = data.lines().filter { it.isNotBlank() }
        
        // Added the ISO format (yyyy-MM-dd) seen in your screenshot!
        val formatIso = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.US)
        val format2Digit = SimpleDateFormat("dd MMM yy hh:mm:ss a", Locale.US)
        val format4Digit = SimpleDateFormat("dd MMM yyyy hh:mm:ss a", Locale.US)

        for (line in lines) {
            // 1. Bulletproof Splitting
            var parts = line.split("\t").map { it.trim().replace("\"", "") }
            if (parts.size < 5) parts = line.split(",").map { it.trim().replace("\"", "") }
            if (parts.size < 5) parts = line.split("\\s{2,}".toRegex()).map { it.trim().replace("\"", "") }
            
            // 2. Safely check for at least 5 columns
            if (parts.size >= 5 && !parts[0].contains("Device", ignoreCase = true)) {
                val device = parts[0]
                val app = getFriendlyAppName(parts[1])
                val date = parts[2]
                val startTime = parts[3]
                val endTime = parts[4]

                val startStr = "$date $startTime"
                val endStr = "$date $endTime"
                
                var startDate: Date? = null
                var endDate: Date? = null
                
                // 3. Safe Date Parsing: Try the new format first, fallback to the old ones
                try { startDate = formatIso.parse(startStr) } catch (e: Exception) {}
                if (startDate == null) try { startDate = format2Digit.parse(startStr) } catch (e: Exception) {}
                if (startDate == null) try { startDate = format4Digit.parse(startStr) } catch (e: Exception) {}
                
                try { endDate = formatIso.parse(endStr) } catch (e: Exception) {}
                if (endDate == null) try { endDate = format2Digit.parse(endStr) } catch (e: Exception) {}
                if (endDate == null) try { endDate = format4Digit.parse(endStr) } catch (e: Exception) {}
                
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
            "com.instagram.android" -> "Instagram"
            "com.facebook.katana" -> "Facebook"
            "com.facebook.orca" -> "Messenger"
            else -> {
                val parts = packageName.split(".")
                if (parts.isNotEmpty()) parts.last().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } else packageName
            }
        }
    }
}
