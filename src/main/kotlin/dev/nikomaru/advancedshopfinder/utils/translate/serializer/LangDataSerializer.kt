package dev.nikomaru.advancedshopfinder.utils.translate.serializer

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import org.bukkit.NamespacedKey
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
object LangDataSerializer : JsonContentPolymorphicSerializer<Map<NamespacedKey, String>>(
    Map::class as KClass<Map<NamespacedKey, String>>,
) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Map<NamespacedKey, String>> =
        MapSerializer(NamespacedKeySerializer, String.serializer())
}
