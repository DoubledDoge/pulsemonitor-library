package io.github.doubleddoge.pulsemonitor.ui

import android.R
import android.app.Activity
import android.app.Application          // NEW
import android.os.Bundle                // NEW
import android.view.Gravity
import android.view.View                // NEW
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

    // NEW: A label we stick on the overlay so we can tell if a screen already has one
    private const val OVERLAY_TAG = "pulsemonitor_overlay"

    // NEW: Remembers if install() has already run, so it only happens once
    private var installed = false

    // NEW: The app calls this ONCE. After that, every screen gets the overlay automatically.
    fun install(activity: Activity) {
        if (installed) return
        installed = true

        // Ask Android to tell us whenever any screen in the app changes
        activity.application.registerActivityLifecycleCallbacks(screenWatcher)
    }

    // NEW: Android calls these automatically as screens open and close
    private val screenWatcher = object : Application.ActivityLifecycleCallbacks {

        // A screen is about to show, so add the overlay to it
        override fun onActivityStarted(activity: Activity) = attach(activity)

        // We don't need these, but Android requires them to exist
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    fun attach(activity: Activity) {
        val root = activity.findViewById<FrameLayout>(
            R.id.content
        )

        // NEW: If this screen already has an overlay (e.g. you came back to it), don't add another
        if (root.findViewWithTag<View>(OVERLAY_TAG) != null) return

        // NEW: .apply { tag = ... } labels the overlay so the check above can find it
        val overlay = ProfilerOverlayView(activity, sharedManager(activity)).apply {
            tag = OVERLAY_TAG
        }

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