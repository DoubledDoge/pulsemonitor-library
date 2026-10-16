package io.github.doubleddoge.pulsemonitor.ui

import android.R
import android.app.Activity
import android.view.Gravity
import android.widget.FrameLayout
import io.github.doubleddoge.pulsemonitor.core.ProfilerManager

object ProfilerOverlay {

    // One shared engine for the whole app.
    // Created on the first attach, then reused for every screen after that.
    private var profilerManager: ProfilerManager? = null

    // Returns the shared manager, creating and starting it the first time
    private fun sharedManager(activity: Activity): ProfilerManager =
        profilerManager ?: ProfilerManager(activity.applicationContext).also {
            profilerManager = it
            it.start()
        }

    fun attach(activity: Activity) {
        val root = activity.findViewById<FrameLayout>(
            R.id.content
        )

        val overlay = ProfilerOverlayView(activity, sharedManager(activity))

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