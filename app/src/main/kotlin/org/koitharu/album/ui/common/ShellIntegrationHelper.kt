package org.koitharu.album.ui.common

import android.content.Intent
import android.util.ArraySet
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.print.PrintHelper
import dagger.Reusable
import org.koitharu.album.R
import org.koitharu.album.util.ActivityContextProvider
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Reusable
class ShellIntegrationHelper @Inject constructor(
    private val activityContextProvider: ActivityContextProvider,
) {

    suspend fun shareMedia(media: AlbumItem.Media) {
        val context = activityContextProvider.get()
        ShareCompat.IntentBuilder(context)
            .addStream(media.uri)
            .setType(media.mimeType)
            .startChooser()
    }

    suspend fun shareMedia(media: Collection<AlbumItem.Media>) {
        media.singleOrNull()?.let {
            return shareMedia(it)
        }
        val context = activityContextProvider.get()
        ShareCompat.IntentBuilder(context)
            .apply {
                val types = ArraySet<String>(media.size)
                for (m in media) {
                    addStream(m.uri)
                    types.add(m.mimeType)
                }
                setType(aggregateMimeType(types))
            }.startChooser()
    }

    suspend fun shareImage(uri: String) {
        val context = activityContextProvider.get()
        ShareCompat.IntentBuilder(context)
            .addStream(uri.toUri())
            .setType("image/*")
            .startChooser()
    }

    suspend fun openUseAs(image: AlbumItem.Image) {
        val context = activityContextProvider.get()
        val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
            setDataAndType(image.uri, image.mimeType)
            addCategory(Intent.CATEGORY_DEFAULT)
            putExtra("mimeType", image.mimeType)
        }
        val chooserIntent = Intent.createChooser(intent, context.getString(R.string.use_as))
        context.startActivity(chooserIntent)
    }

    suspend fun openImageEditor(image: AlbumItem.Image) {
        val context = activityContextProvider.get()
        val intent = Intent(Intent.ACTION_EDIT)
            .setDataAndType(image.uri, image.mimeType)
        val chooserIntent = Intent.createChooser(intent, context.getString(R.string.edit))
        context.startActivity(chooserIntent)
    }

    @Suppress("SuspendCoroutineLacksCancellationGuarantees")
    suspend fun print(image: AlbumItem.Image) {
        val context = activityContextProvider.get()
        val printHelper = PrintHelper(context)
        printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
        val jobName = image.name
            ?: image.uri.lastPathSegment
            ?: context.getString(R.string.app_name)
        suspendCoroutine { cont ->
            printHelper.printBitmap(jobName, image.uri) {
                cont.resume(Unit)
            }
        }
    }

    private companion object {

        const val TYPE_ANY = "*"

        fun aggregateMimeType(mimeTypes: Collection<String>): String {
            val types = ArraySet<String>(mimeTypes.size)
            val subtypes = ArraySet<String>(mimeTypes.size)
            for (mime in mimeTypes) {
                val type = mime.substringBefore('/', TYPE_ANY)
                val subtype = mime.substringAfter('/', TYPE_ANY)
                if (type != TYPE_ANY) {
                    types.add(type)
                }
                if (subtype != TYPE_ANY) {
                    subtypes.add(subtype)
                }
            }
            val type = types.singleOrNull() ?: TYPE_ANY
            val subtype = subtypes.singleOrNull() ?: TYPE_ANY
            return "$type/$subtype"
        }
    }
}