@file:UseSerializers(LongRangeSerializer::class)

package org.koitharu.album.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.koitharu.album.util.serializers.LongRangeSerializer
import java.util.StringJoiner

@Immutable
@Serializable
data class MediaQuery(
    val includeImages: Boolean = true,
    val includeVideos: Boolean = true,
    val isTrashed: Boolean = false,
    val bucketIds: ImmutableSet<String> = persistentSetOf(),
    val isFavorites: Boolean = false,
    val isHidden: Boolean = false,
    val dateRange: LongRange? = null,
    val searchQuery: String? = null,
    val pathPattern: String? = null,
) {

    fun stringKey(): String {
        val joiner = StringJoiner("|")
        joiner.add(includeImages.toString())
        joiner.add(includeVideos.toString())
        joiner.add(isTrashed.toString())
        bucketIds.forEach {
            joiner.add(it)
        }
        joiner.add(isFavorites.toString())
        joiner.add(isHidden.toString())
        joiner.add(dateRange.toString())
        joiner.add(searchQuery.toString())
        joiner.add(pathPattern.toString())
        return joiner.toString()
    }

    companion object {

        val DEFAULT = MediaQuery()
    }
}
