package org.koitharu.album.util.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

class LongRangeSerializer : KSerializer<LongRange> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlin.ranges.LongRange") {
            element<Long>("from")
            element<Long>("to")
        }

    override fun serialize(
        encoder: Encoder,
        value: LongRange
    ) = encoder.encodeStructure(descriptor) {
        encodeLongElement(descriptor, 0, value.first)
        encodeLongElement(descriptor, 1, value.last)
    }

    override fun deserialize(decoder: Decoder): LongRange = decoder.decodeStructure(descriptor) {
        var first = 0L
        var last = 0L

        // Uses decodeElementIndex to ensure correct decoding regardless of order
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> first = decodeLongElement(descriptor, 0)
                1 -> last = decodeLongElement(descriptor, 1)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }

        LongRange(first, last)
    }
}