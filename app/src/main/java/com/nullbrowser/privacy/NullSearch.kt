package com.nullbrowser.privacy

import android.net.Uri

class NullSearchProvider {
    companion object {
        const val SEARCH_ENGINE_NAME = "Harsha Search"
        const val HOME_URL = "https://harsha.search.local/home"
        const val QUERY_URL_PREFIX = "https://harsha.search.local/query?q="
        const val SEARCH_URL_TEMPLATE = "https://search.brave.com/search?q=%s&source=web"

        fun buildSearchUrl(query: String): String {
            return String.format(SEARCH_URL_TEMPLATE, Uri.encode(query))
        }

        fun buildLocalQueryUrl(query: String): String {
            return QUERY_URL_PREFIX + Uri.encode(query)
        }

        fun getResultsPageHtml(query: String, results: List<HarshaResult>): String {
            val cards = results.joinToString("\n") { result ->
                """
                <a class="result" href="${result.url}">
                    <strong>${escape(result.title)}</strong>
                    <span>${escape(result.url)}</span>
                    <p>${escape(result.snippet)}</p>
                </a>
                """.trimIndent()
            }

            return """
                <html>
                <head>
                    <style>
                        body { margin: 0; padding: 24px; background: #050810; color: #f4f7fb; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
                        .top { border: 1px solid rgba(147,197,253,.22); border-radius: 24px; background: rgba(15,23,42,.82); padding: 22px; margin-bottom: 18px; }
                        h1 { margin: 0 0 8px; font-size: 2rem; }
                        .meta { color: #93c5fd; margin: 0; }
                        .result { display: block; text-decoration: none; color: #f4f7fb; border: 1px solid rgba(147,197,253,.18); border-radius: 18px; background: rgba(15,23,42,.72); padding: 18px; margin: 12px 0; }
                        .result strong { display: block; font-size: 1.15rem; margin-bottom: 6px; }
                        .result span { display: block; color: #2dd4bf; font-size: .86rem; margin-bottom: 8px; }
                        .result p { color: #cbd5e1; margin: 0; line-height: 1.45; }
                        .web { display: inline-block; margin-top: 14px; color: #93c5fd; }
                    </style>
                </head>
                <body>
                    <div class="top">
                        <h1>Harsha Search</h1>
                        <p class="meta">Local index results for "${escape(query)}"</p>
                        <a class="web" href="${buildSearchUrl(query)}">Search the web instead</a>
                    </div>
                    $cards
                </body>
                </html>
            """.trimIndent()
        }

        private fun escape(value: String): String {
            return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
        }

        fun getCustomLandingPageHtml(): String {
            return """
                <html>
                <head>
                    <style>
                        * { box-sizing: border-box; }
                        html { background: #070c18; }
                        body {
                            background:
                                radial-gradient(circle at 78% 8%, rgba(125, 211, 252, 0.24), transparent 25%),
                                radial-gradient(circle at 18% 84%, rgba(94, 234, 212, 0.15), transparent 28%),
                                linear-gradient(145deg, #070c18 0%, #0f1b2f 48%, #050711 100%);
                            color: #f4f7fb;
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                            display: flex;
                            flex-direction: column;
                            align-items: center;
                            justify-content: center;
                            min-height: 100vh;
                            margin: 0;
                            padding: 28px;
                            overflow: hidden;
                        }
                        body::before {
                            content: "";
                            position: fixed;
                            inset: -20%;
                            background-image:
                                linear-gradient(rgba(96, 165, 250, 0.18) 1px, transparent 1px),
                                linear-gradient(90deg, rgba(96, 165, 250, 0.09) 1px, transparent 1px);
                            background-size: 76px 76px;
                            transform: perspective(900px) rotateX(64deg) translateY(120px);
                            opacity: 0.20;
                            pointer-events: none;
                        }
                        .panel {
                            width: min(860px, 94vw);
                            position: relative;
                            border: 1px solid rgba(186, 230, 253, 0.20);
                            border-radius: 32px;
                            background: linear-gradient(145deg, rgba(17, 31, 52, 0.86), rgba(8, 14, 28, 0.82));
                            box-shadow: 0 34px 120px rgba(0,0,0,0.48), inset 0 1px 0 rgba(255,255,255,0.08);
                            padding: 36px 24px 26px;
                            backdrop-filter: blur(16px);
                            text-align: center;
                        }
                        .mark {
                            width: 82px;
                            height: 82px;
                            border-radius: 26px;
                            background: radial-gradient(circle at 32% 20%, #ffffff, #bae6fd 18%, #2563eb 58%, #0f172a 100%);
                            display: grid;
                            place-items: center;
                            color: white;
                            font-size: 2.25rem;
                            font-weight: 800;
                            margin: 0 auto 24px;
                            box-shadow: 0 0 42px rgba(96, 165, 250, 0.68), 0 24px 80px rgba(14, 165, 233, 0.28);
                        }
                        h1 { font-size: clamp(2.25rem, 9vw, 4rem); letter-spacing: 0; margin: 0 0 8px; line-height: 1.02; }
                        p { color: #b7cce5; margin: 0 auto 28px; font-size: 1rem; max-width: 620px; line-height: 1.55; }
                        .search-box {
                            width: min(760px, 92vw);
                            padding: 18px 22px;
                            border-radius: 20px;
                            border: 1px solid rgba(147, 197, 253, 0.28);
                            background: rgba(15, 23, 42, 0.82);
                            color: #f4f7fb;
                            font-size: 1.05rem;
                            outline: none;
                            box-shadow: inset 0 1px 0 rgba(255,255,255,0.06), 0 18px 60px rgba(0,0,0,0.34);
                        }
                        .search-box:focus { border-color: #60a5fa; box-shadow: 0 0 0 4px rgba(96, 165, 250, 0.14), 0 18px 60px rgba(0,0,0,0.34); }
                        .chips { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; margin-top: 20px; }
                        .chip { color: #dbeafe; border: 1px solid rgba(147, 197, 253, 0.20); background: rgba(30, 41, 59, 0.66); border-radius: 999px; padding: 8px 12px; font-size: 0.82rem; }
                        .cards { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 22px; text-align: left; }
                        .card { border: 1px solid rgba(147, 197, 253, 0.16); background: rgba(15, 23, 42, 0.56); border-radius: 20px; padding: 16px; }
                        .card strong { display: block; color: #f8fafc; margin-bottom: 6px; }
                        .card span { color: #9fb4ce; font-size: 0.88rem; line-height: 1.45; }
                        @media (max-width: 620px) { .cards { grid-template-columns: 1fr; } .panel { padding: 30px 18px 22px; } }
                    </style>
                </head>
                <body>
                    <div class="panel">
                        <div class="mark">N</div>
                        <h1>NullBrowser</h1>
                        <p>Search with Harsha, open pages privately, and keep controls close to your thumb. Local answers appear first; the web is only a fallback.</p>
                        <input autofocus type="text" class="search-box" placeholder="Search privately or enter a URL" onkeydown="if(event.key === 'Enter') window.location.href = '${QUERY_URL_PREFIX}' + encodeURIComponent(this.value)">
                        <div class="chips">
                            <span class="chip">working search</span>
                            <span class="chip">settings</span>
                            <span class="chip">downloads</span>
                            <span class="chip">privacy stats</span>
                            <span class="chip">policy</span>
                            <span class="chip">screen secure</span>
                        </div>
                        <div class="cards">
                            <div class="card"><strong>Private by default</strong><span>No app history database, secure screen, and session cleanup controls.</span></div>
                            <div class="card"><strong>Local Harsha index</strong><span>Instant built-in answers for browser features before web fallback.</span></div>
                            <div class="card"><strong>Control center</strong><span>Settings, downloads, policy, page info, stats, vault, and wipe actions.</span></div>
                            <div class="card"><strong>Mobile-first</strong><span>Bottom controls, readable spacing, and a calmer dark interface.</span></div>
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()
        }
    }
}
