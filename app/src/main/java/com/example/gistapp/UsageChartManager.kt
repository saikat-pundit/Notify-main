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
    private val timelineBarChart: HorizontalBarChart,
    private val bubbleChartConstellation: com.github.mikephil.charting.charts.BubbleChart,
    private val flowBarChart: HorizontalBarChart
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
        setupBubbleChart()
        setupFlowChart()
    }
    private fun setupBubbleChart() {
        bubbleChartConstellation.description.isEnabled = false
        bubbleChartConstellation.setDrawGridBackground(false)
        bubbleChartConstellation.xAxis.textColor = Color.WHITE
        bubbleChartConstellation.xAxis.position = XAxis.XAxisPosition.BOTTOM
        bubbleChartConstellation.axisLeft.textColor = Color.WHITE
        bubbleChartConstellation.axisRight.isEnabled = false
        bubbleChartConstellation.legend.textColor = Color.WHITE
        bubbleChartConstellation.setPinchZoom(true)
        bubbleChartConstellation.xAxis.spaceMin = 1.5f
        bubbleChartConstellation.xAxis.spaceMax = 1.5f
        bubbleChartConstellation.axisLeft.spaceBottom = 20f
        bubbleChartConstellation.axisLeft.spaceTop = 20f
    }

    private fun setupFlowChart() {
        flowBarChart.description.isEnabled = false
        flowBarChart.setDrawGridBackground(false)
        flowBarChart.setDrawValueAboveBar(false)
        flowBarChart.xAxis.isEnabled = false
        flowBarChart.axisLeft.textColor = Color.WHITE
        flowBarChart.axisRight.isEnabled = false
        flowBarChart.legend.isEnabled = false
        flowBarChart.setScaleEnabled(true)
    }
    private fun setupDonutChart() {
        donutChart.setUsePercentValues(true)
        donutChart.description.isEnabled = false
        donutChart.isDrawHoleEnabled = true
        donutChart.setHoleColor(Color.parseColor("#121212"))
        donutChart.setTransparentCircleAlpha(0)
        donutChart.holeRadius = 55f
        donutChart.setDrawEntryLabels(false)
        donutChart.setExtraOffsets(0f, 0f, 0f, 0f)
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
        val filteredTotals = sortedTotals.filter { it.value >= 60L }.reversed()

        filteredTotals.forEachIndexed { index, entry ->
            val originalName = entry.key
            val trimmedName = if (originalName.length > 12) originalName.take(10) + ".." else originalName
            appBarEntries.add(BarEntry(index.toFloat(), entry.value / 60f, originalName))
            appNames.add(trimmedName)
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
        appUsageData.barWidth = 0.75f
        val appParams = appUsageBarChart.layoutParams
        appParams.height = (appNames.size * 80) + 100
        appUsageBarChart.layoutParams = appParams
        
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
        params.height = (activeHours.size * 80) + 100 // 100 pixels per row + padding
        timelineBarChart.layoutParams = params

        val timelineData = BarData(timelineDataSet)
        timelineData.barWidth = 0.95f
        timelineBarChart.data = timelineData
        timelineBarChart.invalidate()
        // 4. UPDATE BUBBLE CHART (Constellation)
        val bubbleEntries = ArrayList<BubbleEntry>()
        val groupedByApp = records.groupBy { it.app }
        
        var colorIndex = 0
        val bubbleDataSets = ArrayList<com.github.mikephil.charting.interfaces.datasets.IBubbleDataSet>()

        groupedByApp.forEach { (appName, appRecords) ->
            val frequency = appRecords.size.toFloat() // X: How many times opened
            val totalSecsSpent = appRecords.sumOf { it.durationSeconds }
            val avgDuration = (totalSecsSpent / frequency) / 60f // Y: Avg minutes per session
            val bubbleSize = totalSecsSpent.toFloat() // Size: Total time

            val entry = BubbleEntry(frequency, avgDuration, bubbleSize, appName)
            val dataSet = BubbleDataSet(listOf(entry), appName)
            dataSet.color = colors[colorIndex % colors.size]
            dataSet.valueTextColor = Color.TRANSPARENT // Hide ugly numbers on bubbles
            bubbleDataSets.add(dataSet)
            colorIndex++
        }
        
        bubbleChartConstellation.data = BubbleData(bubbleDataSets)
        bubbleChartConstellation.invalidate()
    }
    fun updateFlowChart(records: List<UsageRecord>, startHour: Int, endHour: Int) {
        // Filter chronologically
        val flowRecords = records.filter { it.startHour in startHour..endHour }.sortedBy { it.startTimeMs }
        if (flowRecords.isEmpty()) {
            flowBarChart.clear()
            return
        }

        val flowValues = ArrayList<Float>()
        val flowColors = ArrayList<Int>()
        val flowLabels = ArrayList<String>()

        var lastEndTimeMs = -1L

        // Map apps to their dedicated colors so it matches the Donut Chart
        val uniqueApps = records.map { it.app }.distinct()
        val colorMap = uniqueApps.mapIndexed { index, app -> app to colors[index % colors.size] }.toMap()

        for (record in flowRecords) {
            // If there is a gap between apps, insert a transparent "Screen Off" block!
            if (lastEndTimeMs != -1L && record.startTimeMs > lastEndTimeMs) {
                val gapSecs = (record.startTimeMs - lastEndTimeMs) / 1000f
                if (gapSecs > 0) {
                    flowValues.add(gapSecs / 60f) // Convert to Minutes
                    flowColors.add(Color.TRANSPARENT)
                    flowLabels.add("Screen Off")
                }
            }
            
            // Add the actual App Usage block
            flowValues.add(record.durationSeconds / 60f)
            flowColors.add(colorMap[record.app] ?: Color.GRAY)
            flowLabels.add(record.app)

            lastEndTimeMs = record.startTimeMs + (record.durationSeconds * 1000L)
        }

        // We pass ALL segments into a single BarEntry so they stack side-by-side horizontally
        val entry = BarEntry(0f, flowValues.toFloatArray(), flowLabels)
        
        val dataSet = BarDataSet(listOf(entry), "")
        dataSet.colors = flowColors
        dataSet.setDrawValues(false)

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f // Thickness of the waterfall bar

        flowBarChart.data = barData
        flowBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Flow"))
        flowBarChart.xAxis.labelCount = 1
        flowBarChart.invalidate()
    }
}
