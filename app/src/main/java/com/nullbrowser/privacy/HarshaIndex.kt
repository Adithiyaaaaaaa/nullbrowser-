package com.nullbrowser.privacy

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

data class HarshaResult(
    val title: String,
    val url: String,
    val snippet: String,
    val keywords: String
)

class HarshaIndex(context: Context) {
    private val results: List<HarshaResult> = loadResults(context)

    fun search(query: String): List<HarshaResult> {
        val tokens = query.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 }
            .toSet()
        if (tokens.isEmpty()) return emptyList()

        return results
            .map { result -> result to score(result, tokens) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(6)
            .map { it.first }
    }

    private fun score(result: HarshaResult, tokens: Set<String>): Int {
        val title = result.title.lowercase()
        val url = result.url.lowercase()
        val snippet = result.snippet.lowercase()
        val keywords = result.keywords.lowercase()
        return tokens.sumOf { token ->
            var value = 0
            if (title.contains(token)) value += 8
            if (keywords.contains(token)) value += 5
            if (snippet.contains(token)) value += 2
            if (url.contains(token)) value += 1
            value
        }
    }

    private fun loadResults(context: Context): List<HarshaResult> {
        return try {
            context.assets.open("search/harsha_index.tsv").use { stream ->
                BufferedReader(InputStreamReader(stream)).lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .mapNotNull { line ->
                        val parts = line.split("\t")
                        if (parts.size < 4) null else HarshaResult(parts[0], parts[1], parts[2], parts[3])
                    }
                    .toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
