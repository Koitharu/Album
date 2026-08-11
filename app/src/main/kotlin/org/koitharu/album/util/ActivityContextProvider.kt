package org.koitharu.album.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.annotation.UiContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityContextProvider @Inject constructor() : Application.ActivityLifecycleCallbacks {

    private var currentActivity = MutableStateFlow<WeakReference<Activity>?>(null)

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) = Unit

    override fun onActivityStarted(activity: Activity) {
        currentActivity.value = WeakReference(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        currentActivity.update {
            val dereferenced = it?.get()
            if (dereferenced == null || dereferenced === activity) {
                null
            } else {
                it
            }
        }
    }

    @UiContext
    fun peek(): Context? = currentActivity.value?.get()

    @UiContext
    suspend fun get(): Context = currentActivity.mapNotNull {
        it?.get()
    }.first()
}