package com.example.gistapp

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UsageRecord(
    val device: String,
    val app: String,
    val date: String,
    val startHour: Int,
    val durationSeconds: Long,
    val startTimeMs: Long
)

object UsageParser {
    fun parseData(data: String): List<UsageRecord> {
        val records = mutableListOf<UsageRecord>()
        val lines = data.lines().filter { it.isNotBlank() }
        
        val formatIso = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.US)
        val format2Digit = SimpleDateFormat("dd MMM yy hh:mm:ss a", Locale.US)
        val format4Digit = SimpleDateFormat("dd MMM yyyy hh:mm:ss a", Locale.US)
        
        // NEW: Output formats
        val niceDateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val hourExtractor = SimpleDateFormat("HH", Locale.US) // 24-hour clock

        for (line in lines) {
            var parts = line.split("\t").map { it.trim().replace("\"", "") }
            if (parts.size < 5) parts = line.split(",").map { it.trim().replace("\"", "") }
            if (parts.size < 5) parts = line.split("\\s{2,}".toRegex()).map { it.trim().replace("\"", "") }
            
            if (parts.size >= 5 && !parts[0].contains("Device", ignoreCase = true)) {
                val app = getFriendlyAppName(parts[1])
                
                // EXCLUSION RULE: Skip Mycalculator entirely
                if (app.equals("Mycalculator", ignoreCase = true)) continue

                val startStr = "${parts[2]} ${parts[3]}"
                val endStr = "${parts[2]} ${parts[4]}"
                
                var startDate: Date? = null
                var endDate: Date? = null
                
                try { startDate = formatIso.parse(startStr) } catch (e: Exception) {}
                if (startDate == null) try { startDate = format2Digit.parse(startStr) } catch (e: Exception) {}
                if (startDate == null) try { startDate = format4Digit.parse(startStr) } catch (e: Exception) {}
                
                try { endDate = formatIso.parse(endStr) } catch (e: Exception) {}
                if (endDate == null) try { endDate = format2Digit.parse(endStr) } catch (e: Exception) {}
                if (endDate == null) try { endDate = format4Digit.parse(endStr) } catch (e: Exception) {}
                
                if (startDate != null && endDate != null) {
                    val duration = maxOf(0L, (endDate.time - startDate.time) / 1000)
                    
                    records.add(
                        UsageRecord(
                            device = parts[0],
                            app = app,
                            date = niceDateFormatter.format(startDate), // Beautiful Date String
                            startHour = hourExtractor.format(startDate).toInt(), // Extract 0-23 Hour
                            durationSeconds = duration,
                            startTimeMs = startDate.time
                        )
                    )
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
