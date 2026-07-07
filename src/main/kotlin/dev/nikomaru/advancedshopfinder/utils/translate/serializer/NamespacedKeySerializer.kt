package dev.nikomaru.advancedshopfinder.utils.translate.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.NamespacedKey

object NamespacedKeySerializer : KSerializer<NamespacedKey> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NamespacedKey", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: NamespacedKey,
    ) {
        encoder.encodeString("${value.namespace}:${value.key}")
    }

    override fun deserialize(decoder: Decoder): NamespacedKey {
        val string = decoder.decodeString()
        val parts = string.split(":")
        return NamespacedKey(parts[0], parts[1])
    }
}
