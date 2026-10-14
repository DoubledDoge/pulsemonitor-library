package com.example.demoapp

import android.os.SystemClock
import android.util.Log
import java.net.URL
import java.net.HttpURLConnection

// Uses up each resource on purpose so we can check the overlay reacts
object ResourceBurner {

    private const val BACKGROUND_BURN_MS = 5_000L   // How long the background CPU burn runs
    private const val MAIN_THREAD_BURN_MS = 2_000L  // How long the main thread freezes
    private const val BYTES_PER_MB = 1024 * 1024
    const val ALLOCATION_MB = 20                    // Memory added per tap
    private const val FILL_VALUE: Byte = 1          // Writing a value forces Android to really use the memory
    private const val DOWNLOAD_BUFFER_BYTES = 8 * 1024
    private const val END_OF_STREAM = -1            // read() returns -1 when the download is done
    private const val DOWNLOAD_URL = "https://speed.cloudflare.com/__down?bytes=10000000" // 10 MB test file
    private const val UPLOAD_URL = "https://speed.cloudflare.com/__up" // Test endpoint that accepts uploads
    const val UPLOAD_MB = 5                 // How much to send per tap
    private const val POST_METHOD = "POST"          // HTTP method for sending data
    private const val LOG_TAG = "ResourceBurner"

    // Holds the allocated memory. Without this, Android would clean it up straight away
    private val heldMemory = mutableListOf<ByteArray>()

    // How much memory we're currently holding, shown on the screen
    val heldMemoryMb: Int
        get() = heldMemory.size * ALLOCATION_MB

    // CPU: runs a busy loop on a separate thread, so the screen stays usable
    fun burnCpuInBackground() {
        Thread { spin(BACKGROUND_BURN_MS) }.start()
    }

    // CPU: runs the busy loop on the main thread, so the screen freezes
    fun burnCpuOnMainThread() {
        spin(MAIN_THREAD_BURN_MS)
    }

    // MEMORY: creates a 20 MB block, fills it, and keeps it
    fun allocateMemory() {
        val block = ByteArray(ALLOCATION_MB * BYTES_PER_MB)
        block.fill(FILL_VALUE)
        heldMemory.add(block)
    }

    // MEMORY: lets go of everything and asks Android to clean up
    fun freeMemory() {
        heldMemory.clear()
        Runtime.getRuntime().gc()
    }

    // NETWORK: downloads a file on a background thread and throws the data away
    fun downloadData() {
        Thread {
            runCatching {
                URL(DOWNLOAD_URL).openStream().use { stream ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                    while (stream.read(buffer) != END_OF_STREAM) { }
                }
            }.onFailure { Log.e(LOG_TAG, "Download failed", it) }
        }.start()
    }

    // NETWORK: uploads 5 MB of dummy data on a background thread so "Sent" goes up
    fun uploadData() {
        Thread {
            runCatching {
                val data = ByteArray(UPLOAD_MB * BYTES_PER_MB)
                val connection = URL(UPLOAD_URL).openConnection() as HttpURLConnection
                connection.requestMethod = POST_METHOD
                connection.doOutput = true                        // We are sending a body
                connection.setFixedLengthStreamingMode(data.size) // Streams it instead of holding a second copy
                connection.outputStream.use { it.write(data) }
                connection.responseCode                           // Waits for the server to finish receiving
                connection.disconnect()
            }.onFailure { Log.e(LOG_TAG, "Upload failed", it) }
        }.start()
    }

    // Keeps the CPU busy until the time is up
    private fun spin(durationMs: Long) {
        val end = SystemClock.elapsedRealtime() + durationMs
        while (SystemClock.elapsedRealtime() < end) { }
    }
}