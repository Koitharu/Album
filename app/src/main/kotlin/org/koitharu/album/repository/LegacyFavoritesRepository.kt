package org.koitharu.album.repository

import android.content.Context
import androidx.core.content.edit
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.koitharu.album.util.observeChanges
import javax.inject.Inject

@Reusable
class LegacyFavoritesRepository @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs = context.getSharedPreferences("fav", Context.MODE_PRIVATE)

    suspend fun setIsFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit(commit = true) {
            putBoolean(id.toString(), isFavorite)
        }
    }

    fun isFavorite(id: Long) = prefs.getBoolean(id.toString(), false)

    fun getFavorites(): Set<Long> {
        val values = prefs.all.entries
        return values.mapNotNullTo(HashSet(values.size)) {
            if (it.value == true) {
                it.key.toLongOrNull()
            } else {
                null
            }
        }
    }

    fun getFavoritesCount(): Int = prefs.all.entries.count {
        it.value == true
    }

    fun observeIsFavorite(id: Long) = prefs.observeChanges().map {
        isFavorite(id)
    }.onStart {
        emit(isFavorite(id))
    }.distinctUntilChanged()

    fun observeCount() = prefs.observeChanges().map {
        getFavoritesCount()
    }.onStart {
        emit(getFavoritesCount())
    }.distinctUntilChanged()
}