package org.koitharu.album.repository

import android.content.Context
import androidx.core.content.edit
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Reusable
class FavoritesRepository @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences("fav", Context.MODE_PRIVATE)

    suspend fun setIsFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit(commit = true) {
            putBoolean(id.toString(), isFavorite)
        }
    }

    suspend fun isFavorite(id: Long) = withContext(Dispatchers.IO) {
        prefs.getBoolean(id.toString(), false)
    }

    suspend fun getFavorites(): Set<Long> = withContext(Dispatchers.IO) {
        val values = prefs.all.entries
        values.mapNotNullTo(HashSet(values.size)) {
            if (it.value == true) {
                it.key.toLongOrNull()
            } else {
                null
            }
        }
    }

    suspend fun getFavoritesCount(): Int = withContext(Dispatchers.IO) {
        prefs.all.entries.count {
            it.value == true
        }
    }
}