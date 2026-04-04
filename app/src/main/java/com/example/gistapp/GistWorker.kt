package com.example.gistapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request

class GistWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val client = OkHttpClient()
        
        val url = "https://gist.githubusercontent.com/saikat-pundit/b529558252be113e01993f24429e8556/raw/notifications.csv?t=${System.currentTimeMillis()}"
        val request = Request.Builder().url(url).build()
        
        return try {
            val response = client.newCall(request).execute()
            val rawBody = response.body?.string() ?: return Result.failure()
            
            // --- DECRYPTION INTERCEPT ---
            var finalData = EncryptionHelper.decrypt(rawBody)
            if (finalData.isEmpty() && rawBody.contains(",")) {
                finalData = rawBody
            }
            
            val rows = finalData.lines().filter { it.isNotBlank() }
            if (rows.isEmpty()) return Result.failure()
            
            val prefs = applicationContext.getSharedPreferences("GistPrefs", Context.MODE_PRIVATE)
            val lastCount = prefs.getInt("count", 0)
            
            if (lastCount > 0 && rows.size > lastCount) {
                val latestRowParts = rows.last().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                val notificationText = if (latestRowParts.size >= 3) latestRowParts[2].replace("\"", "") else "New Secure Data"
                
                sendNotification(notificationText)
            }
            prefs.edit().putInt("count", rows.size).apply()
            Result.success()
        } catch (e: Exception) { 
            Result.retry() 
        }
    }

    private fun sendNotification(text: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel("ch1", "Alerts", NotificationManager.IMPORTANCE_HIGH))
        }
        val notif = NotificationCompat.Builder(applicationContext, "ch1")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Live Notify")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        manager.notify(1, notif)
    }
}
