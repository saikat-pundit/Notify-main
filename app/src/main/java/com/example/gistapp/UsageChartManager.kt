package com.example.gistapp

import android.graphics.Color
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter

class UsageChartManager(private val donutChart: PieChart, private val barChart: HorizontalBarChart) {

    // Aesthetic color palette for Dark Mode
    private val colors = listOf(
        Color.parseColor("#64FFDA"), // Mint
        Color.parseColor("#FF8A65"), // Coral
        Color.parseColor("#BA68C8"), // Purple
        Color.parseColor("#4FC3F7"), // Light Blue
        Color.parseColor("#FFF176"), // Yellow
        Color.parseColor("#F06292"), // Pink
        Color.parseColor("#4DB6AC")  // Teal
    )

    init {
        setupDonutChart()
        setupBarChart()
    }

    private fun setupDonutChart() {
        donutChart.setUsePercentValues(false)
        donutChart.description.isEnabled = false
        donutChart.isDrawHoleEnabled = true
        donutChart.setHoleColor(Color.parseColor("#121212")) // Match background
        donutChart.setTransparentCircleAlpha(0)
        donutChart.holeRadius = 58f
        donutChart.setDrawEntryLabels(false) // Hide text on slices, use legend

        val legend = donutChart.legend
        legend.textColor = Color.WHITE
        legend.isWordWrapEnabled = true
    }

    private fun setupBarChart() {
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawValueAboveBar(true)
        barChart.setFitBars(true)
        
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.textColor = Color.WHITE
        barChart.xAxis.setDrawGridLines(false)

        barChart.axisLeft.textColor = Color.WHITE
        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = false // Colors are tied to X-axis, don't need legend here
    }

    fun updateCharts(records: List<UsageRecord>) {
        // Aggregate durations by App
        val usageMap = mutableMapOf<String, Long>()
        for (record in records) {
            usageMap[record.app] = usageMap.getOrDefault(record.app, 0L) + record.durationSeconds
        }

        // Sort by longest duration
        val sortedUsage = usageMap.entries.sortedByDescending { it.value }

        // 1. UPDATE DONUT CHART
        val pieEntries = sortedUsage.map { PieEntry(it.value.toFloat(), it.key) }
        val pieDataSet = PieDataSet(pieEntries, "")
        pieDataSet.colors = colors
        pieDataSet.valueTextColor = Color.parseColor("#121212")
        pieDataSet.valueTextSize = 12f
        pieDataSet.valueFormatter = TimeFormatter()

        donutChart.data = PieData(pieDataSet)
        donutChart.invalidate()

        // 2. UPDATE HORIZONTAL BAR CHART
        val barEntries = ArrayList<BarEntry>()
        val appNames = ArrayList<String>()
        
        // Reverse so the largest is at the top of the horizontal chart
        sortedUsage.reversed().forEachIndexed { index, entry ->
            barEntries.add(BarEntry(index.toFloat(), entry.value.toFloat()))
            appNames.add(entry.key)
        }

        val barDataSet = BarDataSet(barEntries, "Usage Time")
        barDataSet.colors = colors.reversed() // Match Donut colors roughly
        barDataSet.valueTextColor = Color.WHITE
        barDataSet.valueTextSize = 10f
        barDataSet.valueFormatter = TimeFormatter()

        barChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < appNames.size) appNames[index] else ""
            }
        }
        barChart.xAxis.labelCount = appNames.size

        barChart.data = BarData(barDataSet)
        barChart.invalidate()
    }

    // Converts seconds to readable "1h 5m" format
    private class TimeFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val totalSeconds = value.toLong()
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }
    }
}
