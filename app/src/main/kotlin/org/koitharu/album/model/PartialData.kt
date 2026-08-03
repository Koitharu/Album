package org.koitharu.album.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
class PartialData<T>(
    val total: Int,
    val data: ImmutableList<T>,
)