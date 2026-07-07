package dev.nikomaru.advancedshopfinder.utils.translate

import org.bukkit.NamespacedKey
import java.util.Locale

interface TranslateManager {
    fun getTranslateMap(locale: Locale): Map<NamespacedKey, String>
}
