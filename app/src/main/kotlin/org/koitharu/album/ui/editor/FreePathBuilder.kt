package org.koitharu.album.ui.editor

import androidx.annotation.CheckResult
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Path

@ConsistentCopyVisibility
@Immutable
data class FreePathBuilder private constructor(
    private val path: Path,
    private val generation: Int,
) {

    constructor(path: Path) : this(path = path, generation = 0)

    constructor() : this(path = Path(), generation = 0)

    @CheckResult
    fun moveTo(x: Float, y: Float): FreePathBuilder {
        path.moveTo(x, y)
        return nextCopy()
    }

    @CheckResult
    fun lineBy(dx: Float, dy: Float): FreePathBuilder {
        path.relativeLineTo(dx, dy)
        return nextCopy()
    }

    fun toPath() = path

    private fun nextCopy() = copy(generation = generation + 1)
}
