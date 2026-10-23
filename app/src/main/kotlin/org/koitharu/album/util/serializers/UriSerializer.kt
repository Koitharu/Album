package org.koitharu.album.util.serializers

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class UriSerializer : KSerializer<Uri> {

    override val descriptor = PrimitiveSerialDescriptor("android.net.Uri", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Uri
    ) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Uri = decoder.decodeString().toUri()
}