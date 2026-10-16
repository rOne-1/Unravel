package com.unravel.core.engine.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Serializer for [kotlin.uuid.Uuid].
 */
@OptIn(ExperimentalUuidApi::class)
object UuidSerializer : KSerializer<Uuid> {
    override val descriptor: SerialDescriptor =
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("kotlin.uuid.Uuid", kotlinx.serialization.descriptors.PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uuid) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uuid {
        return Uuid.parse(decoder.decodeString())
    }
}

/**
 * Generic Serializer for [ImmutableList].
 */
class ImmutableListSerializer<T>(
    dataSerializer: KSerializer<T>
) : KSerializer<ImmutableList<T>> {
    private val delegate = ListSerializer(dataSerializer)
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: ImmutableList<T>) {
        delegate.serialize(encoder, value.toList())
    }

    override fun deserialize(decoder: Decoder): ImmutableList<T> {
        return delegate.deserialize(decoder).toImmutableList()
    }
}

/**
 * Generic Serializer for [ImmutableSet].
 */
class ImmutableSetSerializer<T>(
    dataSerializer: KSerializer<T>
) : KSerializer<ImmutableSet<T>> {
    private val delegate = SetSerializer(dataSerializer)
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: ImmutableSet<T>) {
        delegate.serialize(encoder, value.toSet())
    }

    override fun deserialize(decoder: Decoder): ImmutableSet<T> {
        return delegate.deserialize(decoder).toImmutableSet()
    }
}

/**
 * Generic Serializer for [ImmutableMap].
 */
class ImmutableMapSerializer<K, V>(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>
) : KSerializer<ImmutableMap<K, V>> {
    private val delegate = MapSerializer(keySerializer, valueSerializer)
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: ImmutableMap<K, V>) {
        delegate.serialize(encoder, value.toMap())
    }

    override fun deserialize(decoder: Decoder): ImmutableMap<K, V> {
        return delegate.deserialize(decoder).toImmutableMap()
    }
}

/**
 * Serializer for [IntRange].
 */
object IntRangeSerializer : KSerializer<IntRange> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("kotlin.ranges.IntRange") {
        element<Int>("start")
        element<Int>("endInclusive")
    }

    override fun serialize(encoder: Encoder, value: IntRange) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.first)
            encodeIntElement(descriptor, 1, value.last)
        }
    }

    override fun deserialize(decoder: Decoder): IntRange {
        return decoder.decodeStructure(descriptor) {
            var start = 0
            var endInclusive = 0
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> start = decodeIntElement(descriptor, 0)
                    1 -> endInclusive = decodeIntElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            start..endInclusive
        }
    }
}
