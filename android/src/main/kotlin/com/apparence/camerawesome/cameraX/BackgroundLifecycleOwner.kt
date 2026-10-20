package com.apparence.camerawesome.cameraX

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * A [LifecycleOwner] that follows the Activity's lifecycle, except that it can be told to stay
 * STARTED while the Activity is in the background.
 *
 * CameraX closes the camera, and ends any recording, as soon as the lifecycle it is bound to drops
 * below STARTED. Bound to the Activity, that happens the moment the app is backgrounded, even if a
 * foreground service of type `camera` is keeping the process alive. With [keepAliveInBackground]
 * on, this owner holds STARTED through onPause/onStop so a recording continues. It still follows
 * the Activity to DESTROYED, so the camera is released when the Activity goes away.
 *
 * Only turn [keepAliveInBackground] on while a foreground service of type `camera` is running.
 * Android refuses background camera access without one.
 *
 * Must be used from the main thread, like any lifecycle.
 */
class BackgroundLifecycleOwner(private val activity: LifecycleOwner) : LifecycleOwner,
    LifecycleEventObserver {
    private val registry = LifecycleRegistry(this)

    /** When true, the camera keeps running while the Activity is paused or stopped. */
    var keepAliveInBackground: Boolean = false
        set(value) {
            field = value
            sync()
        }

    init {
        activity.lifecycle.addObserver(this)
        sync()
    }

    override val lifecycle: Lifecycle
        get() = registry

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) = sync()

    /** Stops following the Activity. Call when the Activity is detached from the plugin. */
    fun release() {
        activity.lifecycle.removeObserver(this)
        if (registry.currentState != Lifecycle.State.DESTROYED) {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    private fun sync() {
        val activityState = activity.lifecycle.currentState
        val target =
            if (keepAliveInBackground &&
                activityState != Lifecycle.State.DESTROYED &&
                !activityState.isAtLeast(Lifecycle.State.STARTED)
            ) {
                Lifecycle.State.STARTED
            } else {
                activityState
            }
        if (registry.currentState != Lifecycle.State.DESTROYED && registry.currentState != target) {
            registry.currentState = target
        }
    }
}
