package com.example.gistapp

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

class UsageChartManager(
    private val donutChart: PieChart, 
    private val barChart: HorizontalBarChart,
    private val detailBoxCard: View,
    private val detailBoxText: TextView
) {

    // Extended color palette to handle many apps
    private val colors = listOf(
        Color.parseColor("#64FFDA"), Color.parseColor("#FF8A65"), Color.parseColor("#BA68C8"),
        Color.parseColor("#4FC3F7"), Color.parseColor("#FFF176"), Color.parseColor("#F06292"),
        Color.parseColor("#4DB6AC"), Color.parseColor("#FFD54F"), Color.parseColor("#A1887F"),
        Color.parseColor("#90CAF9"), Color.parseColor("#F48FB1"), Color.parseColor("#81C784")
    )

    init {
        setupDonutChart()
        setupBarChart()
    }

    private fun setupDonutChart() {
        donutChart.setUsePercentValues(true) // Force percentages
        donutChart.description.isEnabled = false
        donutChart.isDrawHoleEnabled = true
        donutChart.setHoleColor(Color.parseColor("#121212")) 
        donutChart.setTransparentCircleAlpha(0)
        donutChart.holeRadius = 65f // Slightly larger hole for text
        donutChart.setDrawEntryLabels(false)

        donutChart.setCenterTextColor(Color.WHITE)
        donutChart.setCenterTextSize(16f)
        donutChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD)

        val legend = donutChart.legend
        legend.textColor = Color.WHITE
        legend.isWordWrapEnabled = true
        
        // CLICK LISTENER FOR THE DETAILS BOX
        donutChart.setOnChartValueSelectedListener(object: OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val pieEntry = e as? PieEntry ?: return
                val secs = pieEntry.value.toLong()
                val hrs = secs / 3600
                val mins = (secs % 3600) / 60
                detailBoxText.text = "${pieEntry.label}\n${hrs} hr. ${mins} min."
                detailBoxCard.visibility = View.VISIBLE
            }
            override fun onNothingSelected() {
                detailBoxCard.visibility = View.GONE
            }
        })
    }

    private fun setupBarChart() {
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawValueAboveBar(false) // Put totals inside the stacked bars
        barChart.setFitBars(true)
        barChart.setScaleEnabled(false) // Prevent ugly zooming
        
        // Y-Axis (Draws on the Left for HorizontalBarChart - Represents Hours)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.textColor = Color.WHITE
        barChart.xAxis.setDrawGridLines(false)
        barChart.xAxis.textSize = 12f

        // X-Axis (Draws on the Bottom - Represents Minutes)
        barChart.axisLeft.textColor = Color.WHITE
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "${value.toInt()}m"
        }
        
        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = false // Hide legend to save space, donut already has it
    }

    fun updateCharts(records: List<UsageRecord>) {
        if (records.isEmpty()) return
        
        detailBoxCard.visibility = View.GONE // Reset UI

        // --- 1. DONUT CHART (TOTALS) ---
        val appTotals = mutableMapOf<String, Long>()
        records.forEach { appTotals[it.app] = appTotals.getOrDefault(it.app, 0L) + it.durationSeconds }
        val sortedTotals = appTotals.entries.sortedByDescending { it.value }

        val totalSecs = records.sumOf { it.durationSeconds }
        val totalHr = totalSecs / 3600
        val totalMin = (totalSecs % 3600) / 60
        donutChart.centerText = "Total Usage Time\n${totalHr} hr. ${totalMin} min."

        val pieEntries = sortedTotals.map { PieEntry(it.value.toFloat(), it.key) }
        val pieDataSet = PieDataSet(pieEntries, "")
        
        // Map dynamic colors
        val dynamicColors = sortedTotals.indices.map { colors[it % colors.size] }
        pieDataSet.colors = dynamicColors
        
        // Slice Percentage Formatting
        pieDataSet.yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
        pieDataSet.valueTextColor = Color.parseColor("#121212")
        pieDataSet.valueTextSize = 14f
        pieDataSet.valueTypeface = Typeface.DEFAULT_BOLD
        pieDataSet.valueFormatter = object : ValueFormatter() {
            override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                return if (value > 6f) "${value.toInt()}%" else "" // Hide if under 6% to fit slice
            }
        }

        donutChart.data = PieData(pieDataSet)
        donutChart.invalidate()

        // --- 2. BAR CHART (24 HOUR TIMELINE) ---
        val barEntries = ArrayList<BarEntry>()
        val appsList = sortedTotals.map { it.key } // Use same order as donut
        
        // Build 24-hour labels (12am, 1am... 11pm)
        val hourLabels = Array(24) { i -> 
            val hr = if (i % 12 == 0) 12 else i % 12
            val ampm = if (i < 12) "am" else "pm"
            "$hr$ampm"
        }

        // Aggregate minutes per hour per app
        for (hour in 0..23) {
            val recordsInHour = records.filter { it.startHour == hour }
            val floatVals = FloatArray(appsList.size)
            
            for (i in appsList.indices) {
                val sumSecs = recordsInHour.filter { it.app == appsList[i] }.sumOf { it.durationSeconds }
                floatVals[i] = sumSecs / 60f // Convert to Minutes
            }
            
            // Note: HorizontalBarChart stacks from bottom to top, so we reverse the hour index
            barEntries.add(BarEntry((23 - hour).toFloat(), floatVals))
        }

        val barDataSet = BarDataSet(barEntries, "")
        barDataSet.colors = dynamicColors // Sync colors with Donut chart
        barDataSet.setDrawValues(false) // Hide tiny text inside the stacked bars for cleanliness

        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(hourLabels.reversedArray())
        barChart.xAxis.labelCount = 24
        
        barChart.data = BarData(barDataSet)
        barChart.invalidate()
    }
}
