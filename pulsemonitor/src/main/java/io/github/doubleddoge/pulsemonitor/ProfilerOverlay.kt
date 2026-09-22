package io.github.doubleddoge.pulsemonitor

import android.R
import android.app.Activity
import android.view.Gravity
import android.widget.FrameLayout

object ProfilerOverlay {

    fun attach(activity: Activity) {
        val root = activity.findViewById<FrameLayout>(
            R.id.content
        )

        val overlay = ProfilerOverlayView(activity)

        root.addView(
            overlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                gravity =
                    Gravity.TOP or
                            Gravity.END

                rightMargin = dp(activity, 16)
                topMargin = dp(activity, 64)
            }
        )
    }

    private fun dp(
        activity: Activity,
        value: Int
    ): Int {
        return (
                value *
                        activity.resources.displayMetrics.density
                ).toInt()
    }
}