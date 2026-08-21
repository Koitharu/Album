package org.koitharu.album.repository

import android.app.AlertDialog
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koitharu.album.R
import org.koitharu.album.util.ActivityContextProvider
import javax.inject.Inject
import kotlin.coroutines.resume

class MediaStoreConfirmationDialogs @Inject constructor(
    private val activityContextProvider: ActivityContextProvider,
) {

    suspend fun confirmDeletion(media: Collection<Uri>): Boolean {
        val context = activityContextProvider.get()
        return suspendCancellableCoroutine { cont ->
            val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.delete)
                .setMessage(
                    context.getString(
                        R.string.delete_confirmation,
                        context.resources.getQuantityString(R.plurals.items, media.size, media.size)
                    )
                ).setPositiveButton(R.string.delete) { _, _ ->
                    cont.resume(true)
                }.setNegativeButton(android.R.string.cancel, null)
                .setCancelable(true)
                .setOnDismissListener {
                    if (cont.isActive) {
                        cont.resume(false)
                    }
                }.create()
            cont.invokeOnCancellation {
                dialog.cancel()
            }
            dialog.show()
        }
    }
}