package io.github.doubleddoge.pulsemonitor.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.benchmarkly.internal.overlay.Palette
import io.github.doubleddoge.pulsemonitor.R
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

/**
 * Floating profiler overlay.
 *
 * Collapsed: a draggable pill showing CPU / RAM / Network.
 * Expanded: a panel with headline chips and a breakdown for each metric.
 *
 * Export / New Session / Close buttons only appear if you pass the matching
 * callback, so existing call sites like ProfilerOverlayView(context) still work.
 */
@SuppressLint("ViewConstructor")
class ProfilerOverlayView(
    context: Context,
    private val profilerManager: ProfilerManager,  // Shared engine, passed in by ProfilerOverlay
    private val onExport: (() -> Unit)? = null,
    private val onNewSession: (() -> Unit)? = null,
    private val onClose: (() -> Unit)? = null
) : FrameLayout(context) {

    private var expanded = false

    private val d = resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()
    private fun dpf(v: Float) = (v * d).toInt()

    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

    private val medium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    // Two states
    private val pill: View
    private val panel: View
    private lateinit var pillText: TextView

    // Latest formatted headline values, used to rebuild the pill text
    private var cpuStr = "--"
    private var ramStr = "--"
    private var netStr = "--"

    // Constants
    private val cornerRadius = dpf(24f).toFloat()

    //---------------------------------------------------------------------------
    //  Headline chips
    //---------------------------------------------------------------------------
    private lateinit var chipCpu: TextView
    private lateinit var chipRam: TextView
    private lateinit var chipNet: TextView

    //---------------------------------------------------------------------------
    //  RAM TextViews
    //---------------------------------------------------------------------------
    private lateinit var systemMemoryAvailableText: TextView
    private lateinit var memoryJavaHeapText: TextView
    private lateinit var memoryTotalPssText: TextView
    private lateinit var memorySessionPeakPssText: TextView

    //---------------------------------------------------------------------------
    //  CPU TextViews
    //---------------------------------------------------------------------------
    private lateinit var cpuMainThreadText: TextView
    private lateinit var cpuBackgroundText: TextView
    private lateinit var cpuTimeText: TextView

    //---------------------------------------------------------------------------
    //  Network TextViews
    //---------------------------------------------------------------------------
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
        elevation = dpf(14f).toFloat()

        pill = buildPill()
        panel = buildPanel()
        panel.visibility = GONE

        addView(pill)
        addView(panel)
    }

    // ---------------------------------------------------------------------
    //  Observing
    // ---------------------------------------------------------------------
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



        // Observe the latest performance snapshot.
        observationJob = scope.launch {
            profilerManager.performance.collect { snapshot ->
                if (snapshot == null) {
                    return@collect
                }
                renderMemory(snapshot.memory)
                renderCpu(snapshot.cpu)
                renderNetwork(snapshot.network)
                renderPill()
            }
        }
    }

    // Stop observing
    private fun stopObserving() {

        // Stop observing the StateFlow.
        observationJob?.cancel()
        observationJob = null


        // Cancel the view's observation scope.
        viewScope?.cancel()
        viewScope = null
    }

    // View attached to window
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Start collecting and observing data
        // when the overlay is added to the screen.
        startObserving()
    }

    // View detached from window
    override fun onDetachedFromWindow() {

        // Stop collection and observation before detaching.
        stopObserving()
        dragging = false

        super.onDetachedFromWindow()
    }

    private fun toggle() {
        expanded = !expanded

        panel.visibility = if (expanded) VISIBLE else GONE
        pill.visibility = if (!expanded) VISIBLE else GONE
    }

    // ---------------------------------------------------------------------
    //  Collapsed pill
    // ---------------------------------------------------------------------
    private fun buildPill(): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Palette.box(Palette.SURFACE, cornerRadius, Palette.BORDER)
            setPadding(dp(4), dp(4), dp(12), dp(4))
            setOnClickListener { toggle() }
        }

        // App icon in a circle
        row.addView(
            logo(dp(40)),
            LinearLayout.LayoutParams(dp(40), dp(40))
        )

        pillText = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Palette.TEXT)
            typeface = medium
            text = "starting"
        }
        row.addView(pillText, LinearLayout.LayoutParams(WRAP, WRAP).apply {
            leftMargin = dp(10)
            rightMargin = dp(10)
        })

        row.addView(
            icon(R.drawable.pm_ic_chevron_down, Palette.TEXT_MUTED),
            LinearLayout.LayoutParams(dp(18), dp(18))
        )
        return row
    }

    /** Value in white, unit in grey, which keeps the pill readable. */
    private fun renderPill() {
        pillText.text = SpannableStringBuilder().apply {
            unit(cpuStr, " CPU")
            muted("  ·  ")
            unit(ramStr, "")
            muted("  ·  ")
            unit(netStr, "")
        }
    }

    private fun SpannableStringBuilder.unit(value: String, suffix: String) {
        val split = value.indexOfFirst { it == ' ' || it == '%' }
        if (split <= 0) {
            append(value)
        } else {
            append(value.substring(0, split))
            muted(value.substring(split))
        }
        if (suffix.isNotEmpty()) muted(suffix)
    }

    private fun SpannableStringBuilder.muted(text: String) {
        val start = length
        append(text)
        setSpan(
            ForegroundColorSpan(Palette.TEXT_MUTED),
            start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    // ---------------------------------------------------------------------
    //  Expanded panel
    // ---------------------------------------------------------------------
    private fun buildPanel(): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = Palette.box(Palette.SURFACE, cornerRadius, Palette.BORDER)
            layoutParams = LayoutParams(dp(268), WRAP)
        }

        // Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(12), dp(8))
        }
        header.addView(
            logo(dp(34)),
            LinearLayout.LayoutParams(dp(34), dp(34))
        )
        header.addView(TextView(context).apply {
            text = "Pulse Monitor"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Palette.TEXT)
            typeface = medium
        }, LinearLayout.LayoutParams(0, WRAP, 1f).apply { leftMargin = dp(10) })

        header.addView(
            icon(R.drawable.pm_ic_collapse, Palette.TEXT_MUTED) { toggle() },
            LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(10) }
        )

        if (onClose != null) {
            header.addView(
                icon(R.drawable.pm_close_icon, Palette.TEXT_MUTED) { onClose.invoke() },
                LinearLayout.LayoutParams(dp(22), dp(22))
            )
        }
        root.addView(header)
        root.addView(divider())

        // Headline chips
        val chips = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        chipCpu = TextView(context)
        chipRam = TextView(context)
        chipNet = TextView(context)
        chips.addView(chip("CPU", chipCpu), chipParams(first = true))
        chips.addView(chip("RAM", chipRam), chipParams(first = false))
        chips.addView(chip("Network", chipNet), chipParams(first = false))
        root.addView(chips)
        root.addView(divider())

        // Detail sections
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(4))
        }

        body.addView(sectionHeader("CPU Detail"))
        cpuMainThreadText = TextView(context)
        cpuBackgroundText = TextView(context)
        cpuTimeText = TextView(context)
        body.addView(detailRow("Main Thread", cpuMainThreadText))
        body.addView(detailRow("Background Threads", cpuBackgroundText))
        body.addView(detailRow("App CPU time (session)", cpuTimeText))

        body.addView(sectionHeader("RAM Detail"))
        systemMemoryAvailableText = TextView(context)
        memoryJavaHeapText = TextView(context)
        memoryTotalPssText = TextView(context)
        memorySessionPeakPssText = TextView(context)
        body.addView(detailRow("System Available", systemMemoryAvailableText))
        body.addView(detailRow("Java/Kotlin Heap", memoryJavaHeapText))
        body.addView(detailRow("PSS (total)", memoryTotalPssText))
        body.addView(detailRow("Peak this session", memorySessionPeakPssText))

        body.addView(sectionHeader("Network Detail"))
        networkReceivedBytesText = TextView(context)
        networkSentBytesText = TextView(context)
        body.addView(detailRow("Received", networkReceivedBytesText))
        body.addView(detailRow("Sent", networkSentBytesText))

        root.addView(body)

        // Actions (only built for callbacks that were provided)
        if (onExport != null || onNewSession != null) {
            val actions = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(12), dp(12), dp(12), dp(8))
            }
            if (onExport != null) {
                // YOUR ICON: export / download
                actions.addView(
                    button("Export", R.drawable.pm_ic_export, Palette.BUTTON_BLUE) { onExport.invoke() },
                    LinearLayout.LayoutParams(0, WRAP, 1f).apply {
                        rightMargin = if (onNewSession != null) dp(6) else 0
                    }
                )
            }
            if (onNewSession != null) {
                // YOUR ICON: refresh / new session
                actions.addView(
                    button("New Session", R.drawable.pm_ic_refresh, Palette.BUTTON_BLUE) { onNewSession.invoke() },
                    LinearLayout.LayoutParams(0, WRAP, 1f).apply {
                        leftMargin = if (onExport != null) dp(6) else 0
                    }
                )
            }
            root.addView(actions)
        }

        if (onClose != null) {
            // YOUR ICON: power / stop
            root.addView(
                button("Close", R.drawable.pm_ic_power, Palette.BUTTON_RED) { onClose.invoke() },
                LinearLayout.LayoutParams(MATCH, WRAP).apply {
                    leftMargin = dp(12); rightMargin = dp(12); bottomMargin = dp(12)
                    topMargin = if (onExport == null && onNewSession == null) dp(12) else 0
                }
            )
        } else {
            // Bottom breathing room when there are no buttons
            root.addView(View(context), LinearLayout.LayoutParams(MATCH, dp(8)))
        }
        return root
    }

    // ---------------------------------------------------------------------
    //  Building blocks
    // ---------------------------------------------------------------------
    /** Tinted drawable icon. Pass onClick to make it tappable. */
    private fun icon(resId: Int, tint: Int, onClick: (() -> Unit)? = null): ImageView {
        return ImageView(context).apply {
            setImageResource(resId)
            setColorFilter(tint)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            if (onClick != null) setOnClickListener { onClick() }
        }
    }

    /** App icon (R.drawable.icon) sitting in a dark circle. */
    private fun logo(sizePx: Int): View {
        return ImageView(context).apply {
            setImageResource(R.drawable.pm_icon)
            setColorFilter(Palette.ACCENT)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = Palette.box(Palette.SURFACE_SUNKEN, (sizePx / 2).toFloat())
            val pad = sizePx / 4
            setPadding(pad, pad, pad, pad)
        }
    }

    private fun divider() = View(context).apply {
        setBackgroundColor(Palette.DIVIDER)
        layoutParams = LinearLayout.LayoutParams(MATCH, dp(1))
    }

    private fun chipParams(first: Boolean) =
        LinearLayout.LayoutParams(0, WRAP, 1f).apply {
            if (!first) leftMargin = dp(6)
        }

    private fun chip(label: String, value: TextView): View {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = Palette.box(Palette.CHIP, dpf(10f).toFloat(), Palette.BORDER)
            setPadding(dp(6), dp(8), dp(6), dp(8))
        }
        box.addView(TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Palette.TEXT_MUTED)
        })
        value.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Palette.TEXT)
            typeface = medium
            text = "--"
        }
        box.addView(value, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(2) })
        return box
    }

    private fun sectionHeader(title: String) = TextView(context).apply {
        text = title
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setTextColor(Palette.TEXT)
        typeface = medium
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply {
            topMargin = dp(8)
            bottomMargin = dp(4)
        }
    }

    // Row for each stat: muted label on the left, value on the right.
    private fun detailRow(label: String, value: TextView): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(3) }
        }
        row.addView(TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Palette.TEXT_MUTED)
        }, LinearLayout.LayoutParams(0, WRAP, 1f))

        value.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Palette.TEXT)
            gravity = Gravity.END
            text = "--"
        }
        row.addView(value, LinearLayout.LayoutParams(WRAP, WRAP))
        return row
    }

    private fun button(
        label: String,
        iconRes: Int,
        colour: Int,
        onClick: () -> Unit
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = Palette.box(colour, dpf(10f).toFloat())
            setPadding(dp(8), dp(11), dp(8), dp(11))
            isClickable = true
            setOnClickListener { onClick() }
        }
        row.addView(
            icon(iconRes, Palette.TEXT),
            LinearLayout.LayoutParams(dp(17), dp(17)).apply { rightMargin = dp(7) }
        )
        row.addView(TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Palette.TEXT)
            typeface = medium
        })
        return row
    }

    // ---------------------------------------------------------------------
    //  Rendering
    // ---------------------------------------------------------------------
    // Show the memory readings on screen
    private fun renderMemory(memory: MemoryStats) {
        ramStr = "%.1f MB".format(memory.javaHeapUsedMb)
        chipRam.text = ramStr

        systemMemoryAvailableText.text = "%.2f MB".format(memory.systemAvailableRamMb)
        memoryJavaHeapText.text = "%.2f MB".format(memory.javaHeapUsedMb)
        memoryTotalPssText.text = "%.2f MB".format(memory.totalPssMb)
        memorySessionPeakPssText.text = "%.2f MB".format(memory.sessionPeakPssMB)
    }

    // Show the CPU readings on screen
    private fun renderCpu(cpu: CpuStats) {
        cpuStr = "%.1f%%".format(cpu.usagePercent)
        chipCpu.text = cpuStr

        cpuMainThreadText.text = "%.2f%%".format(cpu.mainThreadPercent)
        cpuBackgroundText.text = "%.2f%%".format(cpu.backgroundPercent)
        cpuTimeText.text = "%.2f s".format(cpu.cpuTimeMs / MS_PER_SECOND)
    }

    // Show the network readings on screen
    private fun renderNetwork(network: NetworkStats) {
        netStr = formatRate(network.bytesPerSecond)
        chipNet.text = netStr

        networkReceivedBytesText.text = formatBytes(network.receivedBytes)
        networkSentBytesText.text = formatBytes(network.sentBytes)
    }

    // Converts a bytes/second reading into B/s, KB/s or MB/s
    private fun formatRate(bps: Long): String {
        val kb = 1024.0
        val mb = kb * 1024.0
        return when {
            bps >= mb -> "%.1f MB/s".format(bps / mb)
            bps >= kb -> "%.1f KB/s".format(bps / kb)
            else -> "$bps B/s"
        }
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

    // ---------------------------------------------------------------------
    //  Dragging
    // ---------------------------------------------------------------------
    private var downX = 0f
    private var downY = 0f
    private var startTx = 0f
    private var startTy = 0f
    private var dragging = false
    private val slop = 10f * d

    /**
     * Steals the gesture from the child views only once the finger has moved
     * past the slop threshold. Below it, taps pass through untouched.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                startTx = translationX
                startTy = translationY
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (kotlin.math.abs(ev.rawX - downX) > slop ||
                    kotlin.math.abs(ev.rawY - downY) > slop
                ) {
                    dragging = true
                    return true
                }
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    translationX = startTx + (event.rawX - downX)
                    translationY = startTy + (event.rawY - downY)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    companion object {
        // Bytes in one megabyte
        private const val BYTES_PER_MB = 1024.0 * 1024.0

        // Milliseconds in one second
        private const val MS_PER_SECOND = 1000.0
    }
}