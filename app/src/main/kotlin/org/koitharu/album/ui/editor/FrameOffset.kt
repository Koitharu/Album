package org.koitharu.album.ui.editor

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

@Immutable
data class FrameOffset(
    @Stable val left: Float,
    @Stable val top: Float,
    @Stable val right: Float,
    @Stable val bottom: Float,
) {

    constructor(
        topLeft: Offset,
        bottomRight: Offset,
    ) : this(
        left = topLeft.x,
        top = topLeft.y,
        right = bottomRight.x,
        bottom = bottomRight.y,
    )

    @Stable
    val topLeft: Offset
        get() = Offset(left, top)

    @Stable
    val bottomRight: Offset
        get() = Offset(right, bottom)

    @Stable
    val bottomLeft: Offset
        get() = Offset(left, bottom)

    @Stable
    val topRight: Offset
        get() = Offset(right, top)

    companion object {

        val Zero = FrameOffset(0f, 0f, 0f, 0f)
    }
}
