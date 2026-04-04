package com.example.gistapp
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.work.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
data class NotificationRecord(
    val id: String,
    val app: String,
    val title: String,
    val content: String,
    val time: String
)

class MainActivity : AppCompatActivity() {
    private lateinit var notificationContainer: LinearLayout
    private lateinit var controllerContainer: LinearLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var spinnerControllerDevice: Spinner
    private lateinit var spinnerCommand: Spinner
    private lateinit var etMessage: EditText
    private lateinit var btnActivate: Button
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var listView: ListView
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var btnDateFilter: ImageButton
    private lateinit var tvHeaderTitle: TextView
    private lateinit var spinnerDevice: Spinner
    
    // Search UI
    private lateinit var fabSearch: FloatingActionButton
    private lateinit var searchContainer: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var tvMatchCount: TextView
    private lateinit var btnCloseSearch: ImageButton
    
    private val client = OkHttpClient()
    
    private var allRecords = listOf<NotificationRecord>()
    private var currentCategory = "All Notifications"
    private var currentDevice = "Show all"
    private var currentSearchQuery = "" 
    private var currentDateFilter = "" // NEW: Stores the selected date

    private val autoRefreshHandler = Handler(Looper.getMainLooper())
    private val autoRefreshRunnable = object : Runnable {
        override fun run() {
            fetchGistData()
            autoRefreshHandler.postDelayed(this, 60000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        listView = findViewById(R.id.gistListView)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnMenu = findViewById(R.id.btnMenu)
        btnDateFilter = findViewById(R.id.btnDateFilter)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        spinnerDevice = findViewById(R.id.spinnerDevice)
        
        fabSearch = findViewById(R.id.fabSearch)
        searchContainer = findViewById(R.id.searchContainer)
        etSearch = findViewById(R.id.etSearch)
        tvMatchCount = findViewById(R.id.tvMatchCount)
        btnCloseSearch = findViewById(R.id.btnCloseSearch)

        // 1. Tapping the Title Bar instantly scrolls to the top
        tvHeaderTitle.setOnClickListener {
            listView.smoothScrollToPosition(0)
        }

        // 2. Calendar Date Picker Setup
        btnDateFilter.setOnClickListener {
            val calendar = Calendar.getInstance()
            val dpd = DatePickerDialog(this, { _, year, month, day ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, day)
                val format = SimpleDateFormat("dd MMM", Locale.US)
                
                currentDateFilter = format.format(selectedCal.time).lowercase()
                btnDateFilter.setColorFilter(Color.parseColor("#64FFDA")) // Turn icon Mint Green
                Toast.makeText(this, "Filtering by: $currentDateFilter", Toast.LENGTH_SHORT).show()
                updateListView(forceScrollTop = true)
                
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            
            // Add a "Clear" button to the Calendar to remove the filter
            dpd.setButton(DatePickerDialog.BUTTON_NEUTRAL, "Clear Filter") { _, _ ->
                currentDateFilter = ""
                btnDateFilter.setColorFilter(Color.parseColor("#FFFFFF")) // Revert icon to White
                updateListView(forceScrollTop = true)
            }
            dpd.show()
        }
        
        // Search Animations and Logic
        fabSearch.setOnClickListener {
            fabSearch.animate().scaleX(0f).scaleY(0f).setDuration(200).withEndAction {
                fabSearch.visibility = View.GONE
                searchContainer.visibility = View.VISIBLE
                searchContainer.alpha = 0f
                searchContainer.animate().alpha(1f).setDuration(200).start()
                etSearch.requestFocus()
                
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
            }.start()
        }

        btnCloseSearch.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
            
            etSearch.text.clear()
            searchContainer.animate().alpha(0f).setDuration(200).withEndAction {
                searchContainer.visibility = View.GONE
                fabSearch.visibility = View.VISIBLE
                fabSearch.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            }.start()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString().trim()
                updateListView(forceScrollTop = true) // Scroll to top when searching
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        btnRefresh.setOnClickListener {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable)
            autoRefreshHandler.postDelayed(autoRefreshRunnable, 60000)
            fetchGistData()
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            currentCategory = menuItem.title.toString()
            tvHeaderTitle.text = currentCategory
            drawerLayout.closeDrawer(GravityCompat.START)
            updateListView(forceScrollTop = true) // Scroll to top on category change
            true
        }

        spinnerDevice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position).toString()
                if (currentDevice != selected) {
                    currentDevice = selected
                    updateListView(forceScrollTop = true) // Scroll to top on device change
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        fetchGistData()
        setupBackgroundWorker()
    }

    override fun onResume() {
        super.onResume()
        autoRefreshHandler.post(autoRefreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable)
    }

    private fun fetchGistData() {
        // 1. START THE INFINITE SPIN ON THE UI THREAD
        runOnUiThread {
            // Only start spinning if it isn't already spinning from a background refresh
            if (btnRefresh.animation == null) {
                val rotate = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
                rotate.duration = 1000 // 1 second per full rotation
                rotate.repeatCount = Animation.INFINITE
                rotate.interpolator = LinearInterpolator() // Makes it smooth instead of pausing at the end of each spin
                btnRefresh.startAnimation(rotate)
            }
        }

        val url = "https://gist.githubusercontent.com/saikat-pundit/b529558252be113e01993f24429e8556/raw/notifications.csv?t=${System.currentTimeMillis()}"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 2. STOP SPINNING IF THE INTERNET FAILS
                runOnUiThread {
                    btnRefresh.clearAnimation()
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                val rawBody = response.body?.string() ?: ""
                
                var finalData = EncryptionHelper.decrypt(rawBody)
                if (finalData.isEmpty() && rawBody.contains(",")) {
                    finalData = rawBody
                }

                allRecords = parseCSV(finalData)

                runOnUiThread {
                    updateSidebarMenu()
                    updateDeviceSpinner()
                    updateListView(forceScrollTop = false)
                    
                    // 3. STOP SPINNING ONCE THE DATA IS FULLY LOADED ON SCREEN
                    btnRefresh.clearAnimation()
                }
            }
        })
    }

    private fun updateSidebarMenu() {
        val menu = navView.menu
        menu.clear()
        menu.add(0, 0, 0, "All Notifications")
        val uniqueApps = allRecords.map { it.app }.distinct()
        uniqueApps.forEachIndexed { index, appName -> menu.add(0, index + 1, 0, appName) }
    }

    private fun updateDeviceSpinner() {
        val devices = mutableListOf("Show all")
        devices.addAll(allRecords.map { it.id }.distinct())

        var selectedIndex = devices.indexOf(currentDevice)
        if (selectedIndex == -1) {
            currentDevice = "Show all"
            selectedIndex = 0
        }

        val adapter = ArrayAdapter(this, R.layout.spinner_item, devices)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDevice.adapter = adapter
        spinnerDevice.setSelection(selectedIndex, false)
    }

    // 3. Updated function to accept a "Force Jump to Top" trigger
    private fun updateListView(forceScrollTop: Boolean = false) {
        val currentPosition = listView.firstVisiblePosition
        val childView = listView.getChildAt(0)
        val topOffset = if (childView == null) 0 else childView.top - listView.paddingTop

        // QUADRUPLE Filter: Category + Device + Date + Search
        val filteredRecords = allRecords.filter { record ->
            val matchesCategory = (currentCategory == "All Notifications" || record.app == currentCategory)
            val matchesDevice = (currentDevice == "Show all" || record.id == currentDevice)
            val matchesDate = if (currentDateFilter.isEmpty()) true else record.time.startsWith(currentDateFilter, ignoreCase = true)
            val matchesSearch = if (currentSearchQuery.isEmpty()) true else {
                record.title.contains(currentSearchQuery, ignoreCase = true) || 
                record.content.contains(currentSearchQuery, ignoreCase = true) ||
                record.app.contains(currentSearchQuery, ignoreCase = true) ||
                record.id.contains(currentSearchQuery, ignoreCase = true)
            }
            matchesCategory && matchesDevice && matchesDate && matchesSearch
        }

        if (currentSearchQuery.isNotEmpty()) {
            tvMatchCount.text = "${filteredRecords.size} found"
        } else {
            tvMatchCount.text = ""
        }

        val oldItemCount = listView.adapter?.count ?: 0
        val newItemCount = filteredRecords.size
        val addedItemsCount = if (newItemCount > oldItemCount) newItemCount - oldItemCount else 0

        val adapter = NotificationAdapter(this@MainActivity, filteredRecords)
        listView.adapter = adapter

        // Logic to instantly jump or smoothly restore position based on trigger
        if (forceScrollTop) {
            listView.setSelection(0)
        } else {
            listView.setSelectionFromTop(currentPosition + addedItemsCount, topOffset)
        }
    }

    private fun parseCSV(data: String): List<NotificationRecord> {
        val lines = data.lines().filter { it.contains(",") }
        val records = mutableListOf<NotificationRecord>()
        
        for (i in 1 until lines.size) {
            val parts = lines[i].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
            if (parts.size >= 5) {
                records.add(NotificationRecord(
                    id = parts[0].replace("\"", ""),
                    app = getFriendlyAppName(parts[1].replace("\"", "")),
                    title = parts[2].replace("\"", ""),
                    content = parts[3].replace("\"", ""),
                    time = formatDate(parts[4].replace("\"", "")) 
                ))
            }
        }
        return records.reversed()
    }

    private fun formatDate(rawDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.US)
            val outputFormat = SimpleDateFormat("dd MMM hh:mma", Locale.US)
            val date = inputFormat.parse(rawDate)
            outputFormat.format(date!!).lowercase() 
        } catch (e: Exception) {
            rawDate 
        }
    }

    private fun getFriendlyAppName(packageName: String): String {
        return when (packageName.lowercase()) {
            "com.google.android.apps.messaging" -> "SMS"
            "com.whatsapp" -> "WhatsApp"
            "com.whatsapp.w4b" -> "WhatsApp Business"
            "com.google.android.dialer" -> "Phone"
            "com.google.android.packageinstaller" -> "Android Settings"
            "com.android.settings" -> "Settings"
            "com.android.systemui" -> "System UI"
            "com.android.vending" -> "Play Store"
            "com.instagram.android" -> "Instagram"
            "com.facebook.katana" -> "Facebook"
            "com.facebook.orca" -> "Messenger"
            "com.twitter.android" -> "X (Twitter)"
            "com.google.android.gm" -> "Gmail"
            "com.google.android.youtube" -> "YouTube"
            "com.snapchat.android" -> "Snapchat"
            "org.telegram.messenger" -> "Telegram"
            "com.zhiliaoapp.musically" -> "TikTok"
            "com.discord" -> "Discord"
            "com.linkedin.android" -> "LinkedIn"
            "com.truecaller" -> "Truecaller"
            else -> {
                val parts = packageName.split(".")
                if (parts.isNotEmpty()) parts.last().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } else packageName
            }
        }
    }

    private fun setupBackgroundWorker() {
        val workRequest = PeriodicWorkRequestBuilder<GistWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("GistTask", ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }

    private fun highlightText(text: String, query: String): CharSequence {
        if (query.isEmpty()) return text
        val spannable = SpannableString(text)
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var start = lowerText.indexOf(lowerQuery)
        
        while (start >= 0) {
            val end = start + query.length
            spannable.setSpan(BackgroundColorSpan(Color.parseColor("#64FFDA")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#121212")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            start = lowerText.indexOf(lowerQuery, end)
        }
        return spannable
    }

    inner class NotificationAdapter(context: AppCompatActivity, private val records: List<NotificationRecord>) :
        ArrayAdapter<NotificationRecord>(context, 0, records) {
        
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false)
            val record = records[position]

            val tvDevice = view.findViewById<TextView>(R.id.tvDevice)
            val tvApp = view.findViewById<TextView>(R.id.tvApp)
            val layoutTags = view.findViewById<LinearLayout>(R.id.layoutTags)

            // Hide/Show Device WITH Highlights
            if (currentDevice == "Show all") {
                tvDevice.visibility = View.VISIBLE
                tvDevice.text = highlightText("Device: ${record.id}", currentSearchQuery)
            } else {
                tvDevice.visibility = View.GONE
            }

            if (currentCategory == "All Notifications") {
                tvApp.visibility = View.VISIBLE
                tvApp.text = highlightText("App: ${record.app}", currentSearchQuery)
            } else {
                tvApp.visibility = View.GONE
            }
            
            layoutTags.visibility = if (tvDevice.visibility == View.GONE && tvApp.visibility == View.GONE) View.GONE else View.VISIBLE

            view.findViewById<TextView>(R.id.tvTitle).text = highlightText(record.title, currentSearchQuery)
            view.findViewById<TextView>(R.id.tvContent).text = highlightText(record.content, currentSearchQuery)
            view.findViewById<TextView>(R.id.tvTime).text = record.time
            return view
        }
    }
}
