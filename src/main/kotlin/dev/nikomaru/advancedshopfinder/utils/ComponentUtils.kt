package dev.nikomaru.advancedshopfinder.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

object ComponentUtils {
    fun Component.toPlainText(): String = PlainTextComponentSerializer.plainText().serialize(this)

    fun Component.toLegacyText(): String = LegacyComponentSerializer.builder().build().serialize(this)

    fun Component.toGsonText(): String = GsonComponentSerializer.gson().serialize(this)

    fun String.toComponent(): Component = MiniMessage.miniMessage().deserialize(this)

    fun Component.toMiniMessage(): String = MiniMessage.miniMessage().serialize(this)
}
