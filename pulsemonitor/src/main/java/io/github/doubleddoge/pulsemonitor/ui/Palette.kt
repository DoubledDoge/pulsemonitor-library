package io.benchmarkly.internal.overlay

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

/**
 * Every colour and shape the overlay uses, in one place.
 *
 * Hardcoded rather than themed on purpose. The overlay sits on top of somebody
 * else's app and must stay readable whatever their theme does, so it is always
 * dark with light text.
 */
internal object Palette {

    internal const val SURFACE = 0xFF1B1B1D.toInt()
    internal const val SURFACE_SUNKEN = 0xFF0E0E10.toInt()
    internal const val CHIP = 0xFF242427.toInt()
    internal const val BORDER = 0xFF3A3A3C.toInt()
    internal const val DIVIDER = 0xFF2B2B2D.toInt()

    internal const val TEXT = Color.WHITE
    internal const val TEXT_MUTED = 0xFF8E8E93.toInt()

    internal const val ACCENT = 0xFF4C8DD9.toInt()
    internal const val BUTTON_BLUE = 0xFF5B8FD4.toInt()
    internal const val BUTTON_RED = 0xFFC0504D.toInt()

    /** Rounded rectangle, optionally with a hairline border. */
    internal fun box(fill: Int, radiusPx: Float, border: Int? = null, borderPx: Int = 1) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = radiusPx
            if (border != null) setStroke(borderPx, border)
        }
}
