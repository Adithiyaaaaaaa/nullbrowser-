package com.nullbrowser.privacy

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.os.Debug
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
    private var headlessService: HeadlessAutomationService? = null
    private var agentServer: LocalAgentServer? = null

    private val PREFS = "privacy_state"
    private val KEY_LAST_ACTIVE = "last_active_at"
    private val TEN_DAYS_MS = 10L * 24L * 60L * 60L * 1000L
    private val HOME = "https://duckduckgo.com/"

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

        verifySignature()
        
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        adBlocker = AdBlockerEngine(this)
        
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
            for (signature in packageInfo.signatures) {
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
        val ink = Color.rgb(17, 20, 24)
        val panel = Color.rgb(244, 246, 248)
        val accent = Color.rgb(14, 143, 110)
        val danger = Color.rgb(179, 38, 30)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(6), dp(6), dp(4))
            setBackgroundColor(panel)
        }

        val back = navButton("Back", ink)
        val forward = navButton("Next", ink)
        val homeBtn = navButton("Home", ink)
        val reload = navButton("Reload", ink)
        val vpn = navButton("VPN", accent)
        val wipe = navButton("WIPE", danger)

        addressBar = EditText(this).apply {
            setSingleLine(true)
            setTextColor(ink)
            setHintTextColor(Color.rgb(105, 112, 119))
            hint = "Search or enter address"
            textSize = 14f
            setSelectAllOnFocus(true)
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            setBackgroundColor(Color.WHITE)
            setPadding(dp(10), 0, dp(10), 0)
        }

        val small = LinearLayout.LayoutParams(dp(58), dp(44))
        val urlParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
            setMargins(dp(4), 0, dp(4), 0)
        }

        bar.addView(back, small)
        bar.addView(forward, small)
        bar.addView(homeBtn, small)
        bar.addView(addressBar, urlParams)
        bar.addView(reload, small)
        bar.addView(vpn, small)
        bar.addView(wipe, small)

        statusLine = TextView(this).apply {
            setTextColor(Color.rgb(74, 81, 88))
            textSize = 12f
            setPadding(dp(10), dp(3), dp(10), dp(5))
            setBackgroundColor(panel)
            text = "Human Mode: Protected | Anti-Fingerprint Active"
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }

        webView = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
        }

        root.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)))
        root.addView(statusLine, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(26)))
        root.addView(progressBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)))
        root.addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        back.setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        forward.setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        homeBtn.setOnClickListener { webView.loadUrl(HOME) }
        reload.setOnClickListener { webView.reload() }
        vpn.setOnClickListener { prepareVpn() }
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
        settings.domStorageEnabled = true
        settings.databaseEnabled = false
        settings.setSaveFormData(false)
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.setGeolocationEnabled(false)
        settings.mediaPlaybackRequiresUserGesture = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.userAgentString = settings.userAgentString + " NullBrowser/1.0"

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val host = request?.url?.host
                if (adBlocker.shouldBlock(host)) {
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
        return "https://duckduckgo.com/?q=${Uri.encode(value)}"
    }

    private fun navButton(label: String, color: Int): Button {
        return Button(this).apply {
            text = label
            textSize = 10f
            setTextColor(color)
            isAllCaps = false
            setPadding(0, 0, 0, 0)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }
}
