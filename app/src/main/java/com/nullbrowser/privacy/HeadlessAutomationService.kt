package com.nullbrowser.privacy

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.concurrent.CompletableFuture

class HeadlessAutomationService : Service() {
    private val binder = LocalBinder()
    private lateinit var headlessWebView: WebView
    private val mainHandler = Handler(Looper.getMainLooper())

    inner class LocalBinder : Binder() {
        fun getService(): HeadlessAutomationService = this@HeadlessAutomationService
    }

    override fun onCreate() {
        super.onCreate()
        mainHandler.post {
            setupHeadlessWebView()
        }
    }

    private fun setupHeadlessWebView() {
        headlessWebView = WebView(this)
        val settings = headlessWebView.settings
        
        // Zero-rendering / Ultra-light configuration
        settings.javaScriptEnabled = true
        settings.loadsImagesAutomatically = false
        settings.blockNetworkImage = true
        settings.setSupportZoom(false)
        settings.setGeolocationEnabled(false)
        settings.allowFileAccess = false
        
        // Disable hardware acceleration for the headless view if possible
        headlessWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)

        headlessWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }
    }

    fun navigate(url: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        mainHandler.post {
            headlessWebView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    future.complete(true)
                }
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    future.complete(false)
                }
            }
            headlessWebView.loadUrl(url)
        }
        return future
    }

    fun evaluateJavaScript(script: String): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        mainHandler.post {
            headlessWebView.evaluateJavascript(script) { result ->
                future.complete(result ?: "")
            }
        }
        return future
    }

    fun extractMarkdown(): CompletableFuture<String> {
        // Advanced DOM to Markdown script
        val script = """
            (function() {
                function toMarkdown(node) {
                    if (node.nodeType === Node.TEXT_NODE) return node.textContent;
                    if (node.nodeType !== Node.ELEMENT_NODE) return "";
                    
                    let tag = node.tagName.toLowerCase();
                    let children = Array.from(node.childNodes).map(toMarkdown).join("");
                    
                    switch(tag) {
                        case 'h1': return "\n# " + children + "\n";
                        case 'h2': return "\n## " + children + "\n";
                        case 'h3': return "\n### " + children + "\n";
                        case 'p': return "\n" + children + "\n";
                        case 'a': return "[" + children + "](" + node.href + ")";
                        case 'ul': return "\n" + children;
                        case 'li': return "- " + children + "\n";
                        case 'br': return "\n";
                        case 'script':
                        case 'style':
                        case 'nav':
                        case 'footer': return "";
                        default: return children;
                    }
                }
                return toMarkdown(document.body);
            })()
        """.trimIndent()
        
        val future = CompletableFuture<String>()
        evaluateJavaScript(script).thenAccept { result ->
            // WebView returns results wrapped in quotes and escaped
            val clean = result.removeSurrounding("\"")
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
            future.complete(clean)
        }
        return future
    }

    fun click(selector: String): CompletableFuture<Boolean> {
        val script = "document.querySelector('$selector').click();"
        val future = CompletableFuture<Boolean>()
        evaluateJavaScript(script).thenAccept {
            future.complete(true)
        }
        return future
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        mainHandler.post {
            headlessWebView.destroy()
        }
        super.onDestroy()
    }
}
