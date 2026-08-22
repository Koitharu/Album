package org.koitharu.album.repository.mediastore

class MediaStoreException(
    message: String?,
    cause: Throwable? = null
) : RuntimeException(message, cause)