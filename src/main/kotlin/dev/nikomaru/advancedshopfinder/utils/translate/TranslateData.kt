package dev.nikomaru.advancedshopfinder.utils.translate

import org.bukkit.NamespacedKey
import java.util.Locale

data class TranslateData(
    val data: MutableMap<Locale, Map<NamespacedKey, String>>,
)
