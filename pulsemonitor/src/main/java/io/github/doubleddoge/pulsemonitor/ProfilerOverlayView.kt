package io.github.doubleddoge.pulsemonitor

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import io.github.doubleddoge.pulsemonitor.core.ProfilerManager
import io.github.doubleddoge.pulsemonitor.models.CpuStats
import io.github.doubleddoge.pulsemonitor.models.MemoryStats
import io.github.doubleddoge.pulsemonitor.models.NetworkStats


// Coroutine imports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ProfilerOverlayView(context: Context) : FrameLayout(context) {

    private var expanded = false
    private val button: ImageView
    private val panel: LinearLayout
    private val profilerManager = ProfilerManager(context)

    //---------------------------------------------------------------------------
    //  RAM TextViews
    //---------------------------------------------------------------------------
    private lateinit var systemMemoryAvailableText: TextView
    private lateinit var memoryChangeText: TextView
    private lateinit var memorySessionPeakPssText: TextView
    private lateinit var memoryTotalPssText: TextView
    private lateinit var memoryJavaHeapText: TextView

    //---------------------------------------------------------------------------
    //  CPU TextViews
    //---------------------------------------------------------------------------
    private lateinit var cpuText: TextView
    private lateinit var cpuMainThreadText: TextView
    private lateinit var cpuBackgroundText: TextView
    private lateinit var cpuTimeText: TextView

    //---------------------------------------------------------------------------
    //  Network TextViews
    //---------------------------------------------------------------------------
    private lateinit var networkText: TextView
    private lateinit var networkReceivedBytesText: TextView
    private lateinit var networkSentBytesText: TextView


    // Coroutine scope used to observe the StateFlow.
    // It is created when the view is attached and
    // cancelled when the view is detached.
    private var viewScope: CoroutineScope? = null

    // Job responsible for collecting the StateFlow.
    private var observationJob: Job? = null

    init {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )

        // Floating button
        button = ImageView(context).apply {

            setImageResource(R.drawable.icon)

            // White icon
            setColorFilter(Color.WHITE)

            // Button background
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL

                // Main blue
                setColor(Color.rgb(37, 99, 235))

                // Subtle blue border
                setStroke(
                    dp(2),
                    Color.rgb(96, 165, 250)
                )
            }

            scaleType = ImageView.ScaleType.CENTER_INSIDE

            setPadding(
                dp(14),
                dp(14),
                dp(14),
                dp(14)
            )

            // Makes the button feel like it is floating above the app
            elevation = dp(6).toFloat()

            setOnClickListener {
                toggle()
            }
        }

        val buttonSize = dp(56)

        addView(
            button,
            LayoutParams(buttonSize, buttonSize)
        )

        // Expandable panel
        panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(16),
                dp(8),
                dp(8),
                dp(16)
            )

            background = GradientDrawable().apply {
                // Rounded rectangular shape
                cornerRadius = dp(12).toFloat()

                // Main blue
                setColor(Color.rgb(37, 99, 235))

                // Subtle blue border
                setStroke(
                    dp(2),
                    Color.rgb(96, 165, 250)
                )
            }

            // Makes the panel feel like it is floating above the app
            elevation = dp(6).toFloat()

            visibility = GONE
        }

        // Header containing title and close button
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Title
        val title = TextView(context).apply {
            text = "PROFILER"
            textSize = 18f
            setTextColor(Color.WHITE)

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        // Close button
        val closeButton = ImageButton(context).apply {
            setImageResource(R.drawable.close_icon)

            // White icon
            setColorFilter(Color.WHITE)

            // Remove default ImageButton background
            background = null

            scaleType = ImageView.ScaleType.CENTER_INSIDE

            setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
            )

            setOnClickListener {
                toggle()
            }

            layoutParams = LinearLayout.LayoutParams(
                dp(40),
                dp(40)
            )
        }

        // Add title and close button to header
        header.addView(title)
        header.addView(closeButton)

        // Add header to panel
        panel.addView(header)

        // RAM TextViews
        systemMemoryAvailableText = TextView(context).apply {
            text = "-- MB"
            textSize = 22f
            setTextColor(Color.WHITE)
        }
        panel.addView(systemMemoryAvailableText)

        memoryChangeText = TextView(context).apply {
            text = "Change: --"
            textSize = 12f
            setTextColor(Color.WHITE)
        }
        panel.addView(memoryChangeText)

        memorySessionPeakPssText = TextView(context).apply {
            text = "Peak: --"
            textSize = 12f
            setTextColor(Color.WHITE)
        }
        panel.addView(memorySessionPeakPssText)

        // Detailed Memory
        val memoryDetailsTitle = TextView(context).apply {
            text = "MEMORY"
            textSize = 13f
            setTextColor(Color.WHITE)

            setPadding(
                0,
                dp(12),
                0,
                dp(2)
            )
        }
        panel.addView(memoryDetailsTitle)

        // Java/Heap
        val javaHeapRow = createStatRow("Java/Heap")
        memoryJavaHeapText = javaHeapRow.second

        // Total PSS Memory
        val totalPssRow = createStatRow("PSS")
        memoryTotalPssText = totalPssRow.second

        // Peak PSS
        val peakPssRow = createStatRow("Session Peak")
        memorySessionPeakPssText = peakPssRow.second


        //CPU total (styled)
        cpuText = TextView(context).apply {
            text = "CPU: --"
            textSize = 14f
            setTextColor(Color.WHITE)
        }
        panel.addView(cpuText)

        // CPU Detail
        val cpuDetailsTitle = TextView(context).apply {
            text = "CPU"
            textSize = 13f
            setTextColor(Color.WHITE)

            setPadding(
                0,
                dp(12),
                0,
                dp(2)
            )
        }
        panel.addView(cpuDetailsTitle)

        val cpuMainThread = createStatRow("Main Thread")
        cpuMainThreadText = cpuMainThread.second

        val cpuBackground = createStatRow("Background")
        cpuBackgroundText = cpuBackground.second

        val cpuTime = createStatRow("CPU Time")
        cpuTimeText = cpuTime.second

        // Network speed (main reading)
        networkText = TextView(context).apply {
            text = "Network: --"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(
                0,
                dp(12),
                0,
                dp(2)
            )
        }
        panel.addView(networkText)

        val networkReceivedBytes = createStatRow("Received")
        networkReceivedBytesText = networkReceivedBytes.second

        val networkSentBytes = createStatRow("Sent")
        networkSentBytesText = networkSentBytes.second

        addView(
            panel,
            LayoutParams(
                dp(180),
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                bottomMargin = dp(64)
            }
        )
    }

    // Start observing the performance data
    private fun startObserving() {
        // Avoid starting a second observer.
        if (observationJob?.isActive == true) {
            return
        }

        // Create a new scope for this attachment.
        val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate
        )

        viewScope = scope

        // Start collecting metrics.
        profilerManager.start()

        // Observe the latest performance snapshot.
        observationJob = scope.launch {
            profilerManager.performance.collect { snapshot ->
                if (snapshot == null) {
                    return@collect
                }
                renderMemory(snapshot.memory)
                renderCpu(snapshot.cpu)
                renderNetwork(snapshot.network)
            }
        }
    }

    // Stop observing
    private fun stopObserving() {

        // Stop observing the StateFlow.
        observationJob?.cancel()
        observationJob = null

        // Stop the metric collection loop.
        profilerManager.stop()

        // Cancel the view's observation scope.
        viewScope?.cancel()
        viewScope = null
    }

    // View attached to window
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Start collecting and observing RAM data
        // when the overlay is added to the screen.
        startObserving()
    }

    // View detached from window
    override fun onDetachedFromWindow() {

        // Stop collection and observation before detaching.
        stopObserving()

        super.onDetachedFromWindow()
    }

    private fun toggle() {
        expanded = !expanded

        panel.visibility =
            if (expanded) VISIBLE else GONE
        button.visibility =
            if (!expanded) VISIBLE else GONE
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // Method for creating the rows of each stat metric.
    private fun createStatRow(label: String): Pair<TextView, TextView> {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
            }
        }

        // Label for the stat
        val labelText = TextView(context).apply {
            text = label
            textSize = 13f
            setTextColor(Color.WHITE)

            layoutParams = LinearLayout.LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        // Exact stat value
        val valueText = TextView(context).apply {
            text = "--"
            textSize = 13f
            setTextColor(Color.WHITE)

            gravity = Gravity.END
        }

        row.addView(labelText)
        row.addView(valueText)
        panel.addView(row)

        return Pair(labelText, valueText)
    }

    // Show the memory readings on screen
    private fun renderMemory(memory: MemoryStats){
        systemMemoryAvailableText.text = "%.2f MB".format(memory.systemAvailableRamMb)
        //memoryChangeText.text = "Change: ${formatSignedBytes(memory.pssChangeBytes)}"
        memoryJavaHeapText.text = "%.2f MB".format(memory.javaHeapUsedMb)
        memoryTotalPssText.text = "%.2f MB".format(memory.totalPssMb)
        memorySessionPeakPssText.text = "%.2f MB".format(memory.sessionPeakPssMB)
    }

    // Show the CPU readings on screen
    private fun renderCpu(cpu: CpuStats){
        cpuText.text = "%.2f%%".format(cpu.usagePercent)
        cpuMainThreadText.text = "%.2f%%".format(cpu.mainThreadPercent)
        cpuBackgroundText.text = "%.2f%%".format(cpu.backgroundPercent)
        cpuTimeText.text = "%.2f s".format(cpu.cpuTimeMs / MS_PER_SECOND)
    }

    // Show the network readings on screen
    private fun renderNetwork(network: NetworkStats) {
        networkText.text = "%.2f B/s".format(network.bytesPerSecond.toDouble())
        networkReceivedBytesText.text =  "%.2f B".format(network.receivedBytes.toDouble())
        networkSentBytesText.text = "%.2f B".format(network.sentBytes.toDouble())
    }

    // Converts stats into KB, MB, or GB
    fun formatBytes(bytes: Long?): String {
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0

        if (bytes == null) return "N/A"

        return when {
            bytes >= gb -> "%.2f GB".format(bytes / gb)
            bytes >= mb -> "%.2f MB".format(bytes / mb)
            bytes >= kb -> "%.2f KB".format(bytes / kb)
            else -> "$bytes B"
        }
    }

    // Converts changing RAM readings to signed versions
    // + for increasing
    // - for decreasing
    private fun formatSignedBytes(bytes: Long): String {

        if (bytes == 0L) {
            return "0 B"
        }
        val sign = if (bytes > 0) "+" else "-"

        return sign + formatBytes(kotlin.math.abs(bytes))
    }

    companion object {
        // Bytes in one megabyte
        private const val BYTES_PER_MB = 1024.0 * 1024.0

        // Milliseconds in one second
        private const val MS_PER_SECOND = 1000.0
    }
}