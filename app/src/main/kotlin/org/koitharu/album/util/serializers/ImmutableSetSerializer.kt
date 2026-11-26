package org.koitharu.album.util.serializers

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.setSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class ImmutableSetSerializer<T>(
    private val dataSerializer: KSerializer<T>,
) : KSerializer<ImmutableSet<T>> {

    override val descriptor: SerialDescriptor = setSerialDescriptor(dataSerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: ImmutableSet<T>,
    ) = SetSerializer(dataSerializer).serialize(encoder, value)

    override fun deserialize(decoder: Decoder): ImmutableSet<T> =
        SetSerializer(dataSerializer).deserialize(decoder).toImmutableSet()
}