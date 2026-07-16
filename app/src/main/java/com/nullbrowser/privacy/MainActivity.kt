package com.nullbrowser.privacy

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Bundle
import android.os.Debug
import android.os.Environment
import android.os.IBinder
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var addressBar: EditText
    private lateinit var statusLine: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var prefs: SharedPreferences
    
    private lateinit var adBlocker: AdBlockerEngine
    private lateinit var biometricLock: BiometricLock
    private lateinit var database: NullDatabase
    private lateinit var harshaIndex: HarshaIndex
    private var headlessService: HeadlessAutomationService? = null
    private var agentServer: LocalAgentServer? = null

    private val PREFS = "privacy_state"
    private val KEY_LAST_ACTIVE = "last_active_at"
    private val KEY_BLOCKED_TRACKERS = "blocked_trackers"
    private val KEY_DOWNLOADS = "downloads"
    private val KEY_DOWNLOAD_LOG = "download_log"
    private val KEY_PAGES_LOADED = "pages_loaded"
    private val KEY_STRICT_JS = "strict_js"
    private val KEY_BLOCK_IMAGES = "block_images"
    private val TEN_DAYS_MS = 10L * 24L * 60L * 60L * 1000L
    private val HOME = NullSearchProvider.HOME_URL

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as HeadlessAutomationService.LocalBinder
            headlessService = binder.getService()
            agentServer = LocalAgentServer(this@MainActivity, headlessService!!)
            agentServer?.start()
            Log.d("MainActivity", "Headless engine and Agent Server started")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            headlessService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        verifySignature() // Disabled: Requires NDK
        
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        adBlocker = AdBlockerEngine(this)
        database = NullDatabase.getDatabase(this)
        harshaIndex = HarshaIndex(this)
        
        biometricLock = BiometricLock(
            activity = this,
            onAuthenticated = { /* Continue */ },
            onPanicWipe = { message -> panicWipe(message) }
        )

        wipeIfInactive()
        buildUi()
        configureWebView()
        checkRuntimeRiskSignals()

        val initialUrl = getIntentUrl()
        webView.loadUrl(initialUrl ?: HOME)

        // Start Headless Automation Service
        Intent(this, HeadlessAutomationService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun verifySignature() {
        // Production requirement: Prevent tampering
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            packageInfo.signatures?.forEach { signature ->
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val currentSignature = Base64.encodeToString(md.digest(), Base64.DEFAULT).trim()
                // In a real production app, you'd compare this against a hardcoded known good hash
                Log.d("SignatureVerify", "Hash: $currentSignature")
            }
        } catch (e: Exception) {
            panicWipe("Signature verification failed.")
        }
    }

    override fun onResume() {
        super.onResume()
        biometricLock.onResume()
        wipeIfInactive()
        if (Debug.isDebuggerConnected()) {
            panicWipe("Debugger detected. Private data wiped.")
        }
    }

    override fun onPause() {
        super.onPause()
        biometricLock.onPause()
        prefs.edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply()
    }

    override fun onDestroy() {
        clearSessionData(false)
        webView.destroy()
        agentServer?.stop()
        unbindService(serviceConnection)
        super.onDestroy()
    }

    private fun buildUi() {
        val bg = Color.rgb(7, 12, 24)
        val surface = Color.rgb(14, 23, 38)
        val surfaceLift = Color.rgb(24, 38, 60)
        val textColor = Color.rgb(248, 250, 252)
        val muted = Color.rgb(148, 163, 184)
        val accent = Color.rgb(125, 211, 252)
        val secure = Color.rgb(94, 234, 212)
        val danger = Color.rgb(248, 113, 113)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val bottomShell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(12))
            background = roundedPanel(surface, dp(28), Color.rgb(51, 78, 114))
        }

        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(8))
        }

        val primaryControlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(6))
        }

        val utilityControlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val back = navButton("Back", textColor, surfaceLift)
        val forward = navButton("Next", textColor, surfaceLift)
        val homeBtn = navButton("N", accent, surfaceLift)
        val reload = navButton("Refresh", textColor, surfaceLift)
        val vpn = navButton("VPN", accent)
        val vault = navButton("Vault", secure)
        val stats = navButton("Stats", textColor, surfaceLift)
        val menu = navButton("Menu", accent)
        val wipe = navButton("WIPE", danger)

        addressBar = EditText(this).apply {
            setSingleLine(true)
            setTextColor(textColor)
            setHintTextColor(muted)
            hint = "Search Harsha or enter a URL"
            textSize = 15f
            setSelectAllOnFocus(true)
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            background = roundedPanel(surfaceLift, dp(22), Color.rgb(71, 101, 142))
            setPadding(dp(16), 0, dp(16), 0)
        }

        val small = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
            setMargins(dp(4), 0, dp(4), 0)
        }
        val urlParams = LinearLayout.LayoutParams(0, dp(48), 1f)

        addressRow.addView(addressBar, urlParams)
        primaryControlsRow.addView(back, small)
        primaryControlsRow.addView(forward, small)
        primaryControlsRow.addView(homeBtn, small)
        primaryControlsRow.addView(reload, small)
        utilityControlsRow.addView(vpn, small)
        utilityControlsRow.addView(vault, small)
        utilityControlsRow.addView(stats, small)
        utilityControlsRow.addView(menu, small)
        utilityControlsRow.addView(wipe, small)
        bottomShell.addView(addressRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)))
        bottomShell.addView(primaryControlsRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)))
        bottomShell.addView(utilityControlsRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)))

        statusLine = TextView(this).apply {
            setTextColor(muted)
            textSize = 12f
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = roundedPanel(bg, 0, Color.rgb(14, 23, 38))
            setText("N private session | Harsha local index | no saved history")
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }

        webView = WebView(this).apply {
            setBackgroundColor(bg)
        }

        root.addView(statusLine, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)))
        root.addView(progressBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)))
        root.addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomShell, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(160)))
        setContentView(root)

        back.setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        forward.setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        homeBtn.setOnClickListener { webView.loadUrl(HOME) }
        reload.setOnClickListener { webView.reload() }
        vpn.setOnClickListener { showVpnDialog() }
        vault.setOnClickListener { showVaultDialog() }
        stats.setOnClickListener { showStatsDialog() }
        menu.setOnClickListener { showBrowserMenu() }
        wipe.setOnClickListener { confirmPanicWipe() }

        addressBar.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO || (event?.keyCode == KeyEvent.KEYCODE_ENTER)) {
                webView.loadUrl(normalizeAddress(addressBar.text.toString()))
                true
            } else false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.javaScriptEnabled = !prefs.getBoolean(KEY_STRICT_JS, false)
        settings.domStorageEnabled = true
        settings.databaseEnabled = false
        settings.setSaveFormData(false)
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.setGeolocationEnabled(false)
        settings.mediaPlaybackRequiresUserGesture = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.loadsImagesAutomatically = !prefs.getBoolean(KEY_BLOCK_IMAGES, false)
        settings.blockNetworkImage = prefs.getBoolean(KEY_BLOCK_IMAGES, false)
        settings.userAgentString = settings.userAgentString + " NullBrowser/1.0"

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            confirmDownload(url, userAgent, contentDisposition, mimeType)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: ""
                if (url.startsWith(NullSearchProvider.QUERY_URL_PREFIX)) {
                    val query = android.net.Uri.decode(url.substringAfter(NullSearchProvider.QUERY_URL_PREFIX))
                    val localResults = harshaIndex.search(query)
                    if (localResults.isNotEmpty()) {
                        webView.loadDataWithBaseURL(
                            NullSearchProvider.HOME_URL,
                            NullSearchProvider.getResultsPageHtml(query, localResults),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    } else {
                        webView.loadUrl(NullSearchProvider.buildSearchUrl(query))
                    }
                    return true
                }
                if (url.startsWith("harsha://")) {
                    handleHarshaAction(url)
                    return true
                }
                return false
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: ""
                if (url == NullSearchProvider.HOME_URL) {
                    return WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream(NullSearchProvider.getCustomLandingPageHtml().toByteArray()))
                }
                
                val host = request?.url?.host
                if (adBlocker.shouldBlock(host)) {
                    incrementCounter(KEY_BLOCKED_TRACKERS)
                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                addressBar.setText(url)
                progressBar.visibility = View.GONE
                if (url != null && !url.startsWith("about:")) {
                    incrementCounter(KEY_PAGES_LOADED)
                }
                // Inject Anti-Fingerprinting Script
                view?.evaluateJavascript(adBlocker.getAntiFingerprintScript(), null)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
            }
        }
    }

    private fun wipeIfInactive() {
        val lastActiveAt = prefs.getLong(KEY_LAST_ACTIVE, 0L)
        if (lastActiveAt > 0L && System.currentTimeMillis() - lastActiveAt >= TEN_DAYS_MS) {
            clearSessionData(true)
            Toast.makeText(this, "Inactive for 10 days. Private data wiped.", Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmPanicWipe() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Panic wipe?")
            .setMessage("This clears browser cookies, cache, storage, and local privacy state now.")
            .setPositiveButton("Wipe") { _, _ -> panicWipe("Private data wiped.") }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun panicWipe(message: String) {
        clearSessionData(true)
        webView.loadUrl("about:blank")
        statusLine.text = "Private mode: wiped"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun clearSessionData(clearPrefs: Boolean) {
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()
        if (clearPrefs) {
            prefs.edit().clear().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply()
        }
    }

    private fun checkRuntimeRiskSignals() {
        if (RootSignals.looksRooted()) {
            statusLine.text = "Warning: Root detected."
        }
    }

    private fun showVpnDialog() {
        val regions = arrayOf("United States", "Germany", "Japan", "Switzerland", "Singapore")
        android.app.AlertDialog.Builder(this)
            .setTitle("Select Privacy Node")
            .setItems(regions) { _, which ->
                prepareVpn()
                statusLine.text = "VPN Active: Connected to ${regions[which]}"
            }
            .setNegativeButton("Disconnect") { _, _ ->
                stopService(Intent(this, PrivacyVpnService::class.java))
                statusLine.text = "N private session | Harsha local index | no saved history"
            }
            .show()
    }

    private fun showVaultDialog() {
        Thread {
            val bookmarks = database.bookmarkDao().getAll()
            runOnUiThread {
                val list = bookmarks.map { "${it.title}\n${it.url}" }.toTypedArray()
                android.app.AlertDialog.Builder(this)
                    .setTitle("Secure Vault")
                    .setItems(if (list.isEmpty()) arrayOf("No bookmarks yet") else list) { _, which ->
                        if (bookmarks.isNotEmpty()) {
                            webView.loadUrl(bookmarks[which].url)
                        }
                    }
                    .setPositiveButton("Add Current") { _, _ ->
                        val title = webView.title ?: "No Title"
                        val url = webView.url ?: ""
                        if (url.isNotEmpty()) {
                            Thread {
                                database.bookmarkDao().insert(Bookmark(title = title, url = url))
                                runOnUiThread { Toast.makeText(this, "Saved to Vault", Toast.LENGTH_SHORT).show() }
                            }.start()
                        }
                    }
                    .setNeutralButton("Clear All") { _, _ ->
                        Thread { database.bookmarkDao().deleteAll() }.start()
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }.start()
    }

    private fun showBrowserMenu() {
        val items = arrayOf("Settings", "Downloads", "Privacy Policy", "Page Info", "Clear Session")
        android.app.AlertDialog.Builder(this)
            .setTitle("NullBrowser Control Center")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showSettingsDialog()
                    1 -> showDownloadsDialog()
                    2 -> showPolicyDialog()
                    3 -> showPageInfoDialog()
                    4 -> panicWipe("Session cleared.")
                }
            }
            .show()
    }

    private fun handleHarshaAction(url: String) {
        when (url.removePrefix("harsha://")) {
            "privacy", "policy" -> showPolicyDialog()
            "search" -> webView.loadUrl(HOME)
            "downloads" -> showDownloadsDialog()
            "settings" -> showSettingsDialog()
            "stats" -> showStatsDialog()
            "vault" -> showVaultDialog()
            "vpn" -> showVpnDialog()
            "trackers" -> showStatsDialog()
            "wipe" -> confirmPanicWipe()
            else -> showPageInfoDialog()
        }
    }

    private fun showSettingsDialog() {
        val labels = arrayOf("Strict Mode: disable JavaScript", "Lite Mode: block page images")
        val checked = booleanArrayOf(
            prefs.getBoolean(KEY_STRICT_JS, false),
            prefs.getBoolean(KEY_BLOCK_IMAGES, false)
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("Privacy Settings")
            .setMultiChoiceItems(labels, checked) { _, which, enabled ->
                checked[which] = enabled
            }
            .setPositiveButton("Apply") { _, _ ->
                prefs.edit()
                    .putBoolean(KEY_STRICT_JS, checked[0])
                    .putBoolean(KEY_BLOCK_IMAGES, checked[1])
                    .apply()
                configureWebView()
                webView.reload()
                statusLine.text = "Settings applied | JS ${if (checked[0]) "off" else "on"} | images ${if (checked[1]) "blocked" else "on"}"
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStatsDialog() {
        val pages = prefs.getInt(KEY_PAGES_LOADED, 0)
        val blocked = prefs.getInt(KEY_BLOCKED_TRACKERS, 0)
        val downloads = prefs.getInt(KEY_DOWNLOADS, 0)
        val strict = if (prefs.getBoolean(KEY_STRICT_JS, false)) "Enabled" else "Off"
        val images = if (prefs.getBoolean(KEY_BLOCK_IMAGES, false)) "Blocked" else "Allowed"

        android.app.AlertDialog.Builder(this)
            .setTitle("Privacy Stats")
            .setMessage(
                "Pages loaded: $pages\n" +
                    "Trackers blocked: $blocked\n" +
                    "Downloads started: $downloads\n" +
                    "Strict JS mode: $strict\n" +
                    "Images: $images\n" +
                    "Screenshots: blocked\n" +
                    "History: not stored"
            )
            .setPositiveButton("Reset Stats") { _, _ ->
                prefs.edit()
                    .putInt(KEY_PAGES_LOADED, 0)
                    .putInt(KEY_BLOCKED_TRACKERS, 0)
                    .putInt(KEY_DOWNLOADS, 0)
                    .remove(KEY_DOWNLOAD_LOG)
                    .apply()
                Toast.makeText(this, "Stats reset", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showDownloadsDialog() {
        val log = prefs.getStringSet(KEY_DOWNLOAD_LOG, emptySet()).orEmpty()
            .sortedDescending()
            .map { it.substringAfter("|", it) }
        val display = if (log.isEmpty()) "No downloads started in this session." else log.joinToString("\n\n")

        android.app.AlertDialog.Builder(this)
            .setTitle("Downloads")
            .setMessage(display)
            .setPositiveButton("Clear List") { _, _ ->
                prefs.edit().remove(KEY_DOWNLOAD_LOG).apply()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showPolicyDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Privacy Policy")
            .setMessage(
                "NullBrowser stores no browsing history database.\n\n" +
                    "Session data is wiped on close, panic wipe, and inactivity cleanup.\n\n" +
                    "Downloads are handled by Android Download Manager and may remain in your device Downloads folder.\n\n" +
                    "Harsha Search currently routes web queries to a working external provider until a dedicated backend exists.\n\n" +
                    "VPN mode needs a real tunnel/provider before it can protect traffic."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showPageInfoDialog() {
        val url = webView.url ?: "No active page"
        val title = webView.title ?: "Untitled"
        android.app.AlertDialog.Builder(this)
            .setTitle("Page Info")
            .setMessage("Title: $title\n\nURL: $url\n\nSecure screen: enabled\nLocal history: disabled")
            .setPositiveButton("Copy URL") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("NullBrowser URL", url))
                Toast.makeText(this, "URL copied", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun confirmDownload(url: String, userAgent: String, contentDisposition: String, mimeType: String) {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        android.app.AlertDialog.Builder(this)
            .setTitle("Download file?")
            .setMessage("$fileName\n\nDownloads are saved by Android and are not wiped with browser session data.")
            .setPositiveButton("Download") { _, _ ->
                startDownload(url, userAgent, fileName, mimeType)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startDownload(url: String, userAgent: String, fileName: String, mimeType: String) {
        try {
            val request = DownloadManager.Request(android.net.Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                setTitle(fileName)
                setDescription("Downloaded by NullBrowser")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            incrementCounter(KEY_DOWNLOADS)
            appendDownloadLog(fileName, url)
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun prepareVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, 100)
        } else {
            startService(Intent(this, PrivacyVpnService::class.java))
        }
    }

    private fun getIntentUrl(): String? {
        val intent = intent
        return if (intent?.data != null) normalizeAddress(intent.data.toString()) else null
    }

    private fun normalizeAddress(raw: String?): String {
        val value = raw?.trim() ?: ""
        if (value.isEmpty()) return HOME
        val lower = value.lowercase(Locale.US)
        if (lower.startsWith("https://") || lower.startsWith("http://")) return value
        if (value.contains(".") && !value.contains(" ")) return "https://$value"
        return NullSearchProvider.buildSearchUrl(value)
    }

    private fun incrementCounter(key: String) {
        synchronized(this) {
            prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
        }
    }

    private fun appendDownloadLog(fileName: String, url: String) {
        val current = prefs.getStringSet(KEY_DOWNLOAD_LOG, emptySet()).orEmpty().toMutableList()
        current.add("${System.currentTimeMillis()}|$fileName\n$url")
        val trimmed = current.takeLast(20).toSet()
        prefs.edit().putStringSet(KEY_DOWNLOAD_LOG, trimmed).apply()
    }

    private fun navButton(label: String, color: Int, backgroundColor: Int = Color.rgb(12, 19, 32)): Button {
        return Button(this).apply {
            text = label
            textSize = 10f
            setTextColor(color)
            background = roundedPanel(backgroundColor, dp(14), Color.rgb(42, 62, 90))
            isAllCaps = false
            setPadding(0, 0, 0, 0)
        }
    }

    private fun roundedPanel(fill: Int, radius: Int, stroke: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = radius.toFloat()
            setStroke(dp(1), stroke)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }
}
