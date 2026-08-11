package org.koitharu.album.util

import androidx.annotation.Px
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Stable
class DirectedNestedScrollConnection(
    @Px
    private val minDelta: Float,
) : NestedScrollConnection {

    val direction: IntState
        field = mutableIntStateOf(0)

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.y
        when {
            delta > minDelta -> direction.intValue = 1
            delta < -minDelta -> direction.intValue = -1
        }
        return super.onPreScroll(available, source)
    }
}

@Composable
fun rememberNestedScrollDirectionConnection(
    minDelta: Dp,
): DirectedNestedScrollConnection {
    val density = LocalDensity.current
    return remember(density) {
        val deltaPx = with(density) { minDelta.toPx() }
        DirectedNestedScrollConnection(deltaPx)
    }
}
