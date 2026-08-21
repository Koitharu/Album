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
class HiddenMediaRepository @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs = context.getSharedPreferences("hidden", Context.MODE_PRIVATE)

    suspend fun setIsHidden(id: Long, isHidden: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit(commit = true) {
            putBoolean(id.toString(), isHidden)
        }
    }

    suspend fun setIsHidden(ids: Iterable<Long>, isHidden: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit(commit = true) {
            ids.forEach { id ->
                putBoolean(id.toString(), isHidden)
            }
        }
    }

    fun isHidden(id: Long) = prefs.getBoolean(id.toString(), false)

    fun getHiddenIds(): Set<Long> {
        val values = prefs.all.entries
        return values.mapNotNullTo(HashSet(values.size)) {
            if (it.value == true) {
                it.key.toLongOrNull()
            } else {
                null
            }
        }
    }

    fun getHiddenCount(): Int = prefs.all.entries.count {
        it.value == true
    }

    fun observeHiddenCount() = prefs.observeChanges()
        .map {
            getHiddenCount()
        }.onStart {
            emit(getHiddenCount())
        }.distinctUntilChanged()
}