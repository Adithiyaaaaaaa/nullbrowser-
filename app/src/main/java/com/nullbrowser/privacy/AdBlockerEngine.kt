package com.nullbrowser.privacy

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class AdBlockerEngine(private val context: Context) {
    private val blocklist = ConcurrentHashMap.newKeySet<String>()
    private val executor = Executors.newSingleThreadExecutor()

    init {
        loadBlocklist()
    }

    private fun loadBlocklist() {
        executor.execute {
            try {
                context.assets.open("blocklists/trackers.txt").use { stream ->
                    BufferedReader(InputStreamReader(stream)).forEachLine { raw ->
                        val domain = raw.trim()
                        if (domain.isNotEmpty() && !domain.startsWith("#")) {
                            blocklist.add(domain.lowercase())
                        }
                    }
                }
                Log.d("AdBlockerEngine", "Loaded ${blocklist.size} tracker domains")
            } catch (e: Exception) {
                Log.w("AdBlockerEngine", "Asset blocklist unavailable, loading fallback list", e)
                blocklist.addAll(fallbackBlocklist)
            }
        }
    }

    fun shouldBlock(host: String?): Boolean {
        if (host == null) return false
        
        // Check exact match and subdomains
        var domain: String = host.lowercase()
        while (domain.contains(".")) {
            if (blocklist.contains(domain)) return true
            domain = domain.substringAfter(".", "")
            if (domain.isEmpty()) break
        }
        return false
    }

    private val fallbackBlocklist = setOf(
        "google-analytics.com",
        "doubleclick.net",
        "quantserve.com",
        "adnxs.com",
        "facebook.net",
        "scorecardresearch.com",
        "amazon-adsystem.com",
        "googletagservices.com",
        "taboola.com",
        "outbrain.com"
    )

    // Fingerprint Defense JS
    fun getAntiFingerprintScript(): String {
        return """
            (function() {
                const overwrite = (obj, prop, value) => {
                    Object.defineProperty(obj, prop, { get: () => value });
                };
                
                // Canvas Spoofing
                const originalGetImageData = CanvasRenderingContext2D.prototype.getImageData;
                CanvasRenderingContext2D.prototype.getImageData = function(x, y, w, h) {
                    const data = originalGetImageData.apply(this, arguments);
                    // Subtle noise injection
                    data.data[0] = data.data[0] ^ 1;
                    return data;
                };

                // Hardware Spoofing
                overwrite(navigator, 'hardwareConcurrency', 4);
                overwrite(navigator, 'deviceMemory', 8);
                
                // AudioContext Spoofing
                const originalGetChannelData = AudioBuffer.prototype.getChannelData;
                AudioBuffer.prototype.getChannelData = function() {
                    const data = originalGetChannelData.apply(this, arguments);
                    data[0] += 0.00001;
                    return data;
                };
            })();
        """.trimIndent()
    }
}
