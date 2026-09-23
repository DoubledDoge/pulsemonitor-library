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
import com.example.profiler_overlay.core.ProfilerManager
import io.github.doubleddoge.pulsemonitor.metrics.cpu.CpuCollector

// Coroutine imports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class ProfilerOverlayView(context: Context) : FrameLayout(context) {

    private var expanded = false
    private val button: ImageView
    private val panel: LinearLayout
    private val profilerManager = ProfilerManager(context)

    // Stores the TextView that displays the RAM reading.
    private lateinit var memoryText: TextView

    //Collects the CPU readings
    private val cpuCollector = CpuCollector()

    //TextViews that display the CPU readings
    private lateinit var cpuText: TextView
    private lateinit var mainThreadText: TextView
    private lateinit var backgroundText: TextView
    private lateinit var cpuTimeText: TextView

    // Coroutine scope used to observe the StateFlow.
    // It is created when the view is attached and
    // cancelled when the view is detached.
    private var viewScope: CoroutineScope? = null

    // Job responsible for collecting the StateFlow.
    private var observationJob: Job? = null

//    val snapshot = profilerManager.collectSnapshot()
//    val ramBytes = snapshot.memory?.usedBytes
//    val ramMb = ramBytes?.div((1024.0 * 1024.0))



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


        // Placeholder content
        memoryText = TextView(context).apply {
            // Initial text shown before the first reading arrives.
            text = "RAM: --"
            textSize = 14f
            setTextColor(Color.WHITE)
        }

        // Add the RAM reading to the panel.
        panel.addView(memoryText)

        //CPU total (styled)
        cpuText = TextView(context).apply {
            text = "CPU: --"
            textSize = 14f
            setTextColor(Color.WHITE)
        }

        //The three readings Underneath CPU (smaller text)
        mainThreadText = TextView(context).apply {
            text = "Main thread: --"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(dp(12), 0, 0, 0)
        }

        backgroundText = TextView(context).apply {
            text = "Background: --"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(dp(12), 0, 0, 0)
        }

        cpuTimeText = TextView(context).apply {
            text = "CPU time: --"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(dp(12), 0, 0, 0)
        }

        //Put the CPU readings in the panel under RAM
        panel.addView(cpuText)
        panel.addView(mainThreadText)
        panel.addView(backgroundText)
        panel.addView(cpuTimeText)

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
                if (snapshot == null) return@collect

                val memoryBytes = snapshot.memory.usedBytes
                val memoryMb = memoryBytes / (1024.0 * 1024.0)

                memoryText.text = "RAM: %.8f MB".format(memoryMb)

                //take CPU reading when ram updates
                val cpu = cpuCollector.collect()

                //show the CPU readings on screen
                cpuText.text = "CPU: %.1f%%".format(cpu.usagePercent)
                mainThreadText.text = "Main thread: %.1f%%".format(cpu.mainThreadPercent)
                backgroundText.text = "Background: %.1f%%".format(cpu.backgroundPercent)
                cpuTimeText.text = "CPU time: %.1f s".format(cpu.cpuTimeMs / 1000.0)
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
}