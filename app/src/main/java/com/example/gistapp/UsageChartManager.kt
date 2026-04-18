package com.example.gistapp

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

class UsageChartManager(
    private val context: Context,
    private val donutChart: PieChart, 
    private val appUsageBarChart: HorizontalBarChart,
    private val timelineBarChart: HorizontalBarChart
) {
    private val colors = listOf(
        Color.parseColor("#64FFDA"), Color.parseColor("#FF8A65"), Color.parseColor("#BA68C8"),
        Color.parseColor("#4FC3F7"), Color.parseColor("#FFF176"), Color.parseColor("#F06292"),
        Color.parseColor("#4DB6AC"), Color.parseColor("#FFD54F"), Color.parseColor("#A1887F"),
        Color.parseColor("#90CAF9"), Color.parseColor("#F48FB1"), Color.parseColor("#81C784")
    )

    init {
        // Attach the interactive tooltips to all charts
        val marker = CustomMarkerView(context, R.layout.custom_marker_view)
        donutChart.marker = marker
        appUsageBarChart.marker = marker
        timelineBarChart.marker = marker

        setupDonutChart()
        setupAppUsageBarChart()
        setupTimelineBarChart()
    }

    private fun setupDonutChart() {
        donutChart.setUsePercentValues(true)
        donutChart.description.isEnabled = false
        donutChart.isDrawHoleEnabled = true
        donutChart.setHoleColor(Color.parseColor("#121212"))
        donutChart.setTransparentCircleAlpha(0)
        donutChart.holeRadius = 55f
        donutChart.setDrawEntryLabels(false)

        donutChart.setCenterTextColor(Color.WHITE)
        donutChart.setCenterTextSize(20f) // Bigger center text
        donutChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD)

        val legend = donutChart.legend
        legend.textColor = Color.WHITE
        legend.isWordWrapEnabled = true
    }

    private fun setupAppUsageBarChart() {
        appUsageBarChart.description.isEnabled = false
        appUsageBarChart.setDrawGridBackground(false)
        appUsageBarChart.setDrawValueAboveBar(true) // Show totals at end of bar
        appUsageBarChart.setScaleEnabled(false)

        appUsageBarChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        appUsageBarChart.xAxis.textColor = Color.WHITE
        appUsageBarChart.xAxis.setDrawGridLines(false)

        appUsageBarChart.axisLeft.textColor = Color.WHITE
        appUsageBarChart.axisLeft.axisMinimum = 0f
        appUsageBarChart.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "${value.toInt()}m"
        }
        appUsageBarChart.axisRight.isEnabled = false
        appUsageBarChart.legend.isEnabled = false
    }

    private fun setupTimelineBarChart() {
        timelineBarChart.description.isEnabled = false
        timelineBarChart.setDrawGridBackground(false)
        timelineBarChart.setDrawValueAboveBar(false) 
        timelineBarChart.setScaleEnabled(false)
        
        timelineBarChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        timelineBarChart.xAxis.textColor = Color.WHITE
        timelineBarChart.xAxis.setDrawGridLines(false)
        timelineBarChart.xAxis.textSize = 12f

        timelineBarChart.axisLeft.textColor = Color.WHITE
        timelineBarChart.axisLeft.axisMinimum = 0f
        timelineBarChart.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "${value.toInt()}m"
        }
        timelineBarChart.axisRight.isEnabled = false
        timelineBarChart.legend.isEnabled = false 
    }

    fun updateCharts(records: List<UsageRecord>) {
        if (records.isEmpty()) return

        val appTotals = mutableMapOf<String, Long>()
        records.forEach { appTotals[it.app] = appTotals.getOrDefault(it.app, 0L) + it.durationSeconds }
        val sortedTotals = appTotals.entries.sortedByDescending { it.value }

        val totalSecs = records.sumOf { it.durationSeconds }
        val totalHr = totalSecs / 3600
        val totalMin = (totalSecs % 3600) / 60
        
        // 1. UPDATE DONUT CHART (Clean center text)
        donutChart.centerText = "${totalHr} hr. ${totalMin} min."

        val pieEntries = sortedTotals.map { PieEntry(it.value.toFloat(), it.key) }
        val dynamicColors = sortedTotals.indices.map { colors[it % colors.size] }
        
        val pieDataSet = PieDataSet(pieEntries, "")
        pieDataSet.colors = dynamicColors
        pieDataSet.yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
        pieDataSet.valueTextColor = Color.parseColor("#121212")
        pieDataSet.valueTextSize = 14f
        pieDataSet.valueTypeface = Typeface.DEFAULT_BOLD
        pieDataSet.valueFormatter = object : ValueFormatter() {
            override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                return if (value > 6f) "${value.toInt()}%" else ""
            }
        }
        donutChart.data = PieData(pieDataSet)
        donutChart.invalidate()

        // 2. UPDATE APP SPECIFIC BAR CHART (Restored)
        val appBarEntries = ArrayList<BarEntry>()
        val appNames = ArrayList<String>()
        
        sortedTotals.reversed().forEachIndexed { index, entry ->
            // Pass app name as 'data' for the Tooltip
            appBarEntries.add(BarEntry(index.toFloat(), entry.value / 60f, entry.key))
            appNames.add(entry.key)
        }

        val appBarDataSet = BarDataSet(appBarEntries, "")
        appBarDataSet.colors = dynamicColors.reversed()
        appBarDataSet.valueTextColor = Color.WHITE
        appBarDataSet.valueTextSize = 10f
        appBarDataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val mins = value.toInt()
                val hrs = mins / 60
                val remMins = mins % 60
                return if (hrs > 0) "${hrs}h ${remMins}m" else "${remMins}m"
            }
        }

        appUsageBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(appNames)
        appUsageBarChart.xAxis.labelCount = appNames.size
        val appUsageData = BarData(appBarDataSet)
        appUsageData.barWidth = 0.95f
        appUsageBarChart.data = appUsageData
        appUsageBarChart.invalidate()


        // 3. UPDATE 24-HOUR TIMELINE (Only Active Hours)
        val timelineEntries = ArrayList<BarEntry>()
        val appsList = sortedTotals.map { it.key }
        
        // Find only hours that have usage
        val activeHours = (0..23).filter { hour -> records.any { it.startHour == hour } }
        
        // Create labels like "12am", "3pm"
        val activeHourLabels = activeHours.map { i -> 
            val hr = if (i % 12 == 0) 12 else i % 12
            val ampm = if (i < 12) "am" else "pm"
            "$hr$ampm"
        }.reversed()

        // Build the stacked bars
        activeHours.reversed().forEachIndexed { index, hour ->
            val recordsInHour = records.filter { it.startHour == hour }
            val floatVals = FloatArray(appsList.size)
            
            for (i in appsList.indices) {
                val sumSecs = recordsInHour.filter { it.app == appsList[i] }.sumOf { it.durationSeconds }
                floatVals[i] = sumSecs / 60f 
            }
            
            // Pass appsList as 'data' so the Tooltip knows which stack index belongs to which app
            timelineEntries.add(BarEntry(index.toFloat(), floatVals, appsList))
        }

        val timelineDataSet = BarDataSet(timelineEntries, "")
        timelineDataSet.colors = dynamicColors 
        timelineDataSet.setDrawValues(false) 

        timelineBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(activeHourLabels)
        timelineBarChart.xAxis.labelCount = activeHours.size
        
        // Adjust height dynamically based on active hours so bars aren't too stretched
        val params = timelineBarChart.layoutParams
        params.height = (activeHours.size * 100) + 100 // 100 pixels per row + padding
        timelineBarChart.layoutParams = params

        val timelineData = BarData(timelineDataSet)
        timelineData.barWidth = 0.4f
        timelineBarChart.data = timelineData
        timelineBarChart.invalidate()
    }
}
