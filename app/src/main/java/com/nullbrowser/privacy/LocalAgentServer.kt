package com.nullbrowser.privacy

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class LocalAgentServer(private val context: Context, private val automationService: HeadlessAutomationService) {
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    private var running = false

    fun start(port: Int = 8080) {
        running = true
        thread {
            try {
                serverSocket = ServerSocket(port, 50, java.net.InetAddress.getByName("127.0.0.1"))
                while (running) {
                    val client = serverSocket?.accept() ?: break
                    executor.execute { handleClient(client) }
                }
            } catch (e: Exception) {
                Log.e("LocalAgentServer", "Server error", e)
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            val line = reader.readLine() ?: return
            val parts = line.split(" ")
            if (parts.size < 2) return
            
            val path = parts[1]
            val uri = Uri.parse(path)
            
            val response = when (uri.path) {
                "/goto" -> {
                    val url = uri.getQueryParameter("url")
                    if (url != null) {
                        automationService.navigate(url).get()
                        "OK"
                    } else "Missing url"
                }
                "/extract_markdown" -> {
                    automationService.extractMarkdown().get()
                }
                "/click" -> {
                    val selector = uri.getQueryParameter("selector")
                    if (selector != null) {
                        automationService.click(selector).get()
                        "OK"
                    } else "Missing selector"
                }
                "/evaluate" -> {
                    val script = uri.getQueryParameter("script")
                    if (script != null) {
                        automationService.evaluateJavaScript(script).get()
                    } else "Missing script"
                }
                else -> "Not Found"
            }
            
            val out = PrintWriter(socket.outputStream)
            out.println("HTTP/1.1 200 OK")
            out.println("Content-Type: text/plain")
            out.println("Content-Length: ${response.length}")
            out.println("Connection: close")
            out.println("")
            out.print(response)
            out.flush()
            socket.close()
        } catch (e: Exception) {
            Log.e("LocalAgentServer", "Client error", e)
        }
    }

    fun stop() {
        running = false
        serverSocket?.close()
        executor.shutdown()
    }
}
