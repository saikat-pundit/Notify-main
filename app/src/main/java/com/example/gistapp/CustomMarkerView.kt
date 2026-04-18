package com.example.gistapp

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import android.os.Handler
import android.os.Looper

class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
    private val tvContent: TextView = findViewById(R.id.tvContent)
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { chartView?.highlightValue(null) }
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return

        var appName = ""
        var durationSecs = 0L

        when (e) {
            is PieEntry -> {
                // Donut Chart
                appName = e.label ?: ""
                durationSecs = e.value.toLong()
            }
            is BarEntry -> {
                if (e.yVals != null && highlight != null) {
                    // Stacked Bar (24-Hour Timeline)
                    val stackIndex = highlight.stackIndex
                    val appsList = e.data as? List<*>
                    appName = appsList?.getOrNull(stackIndex)?.toString() ?: ""
                    durationSecs = (e.yVals[stackIndex] * 60).toLong() // Mins back to Secs
                } else {
                    // Single Bar (App Specific Usage)
                    appName = e.data?.toString() ?: ""
                    durationSecs = (e.y * 60).toLong() // Mins back to Secs
                }
            }
        }

        // Format beautifully
        val hrs = durationSecs / 3600
        val mins = (durationSecs % 3600) / 60
        val timeStr = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
        tvContent.text = "$appName\n$timeStr"
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 3000)
        super.refreshContent(e, highlight)
    }

    // Centers the tooltip slightly above the user's finger
    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat() - 20f)
    }
}
