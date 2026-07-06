package dev.nikomaru.advancedshopfinder.utils.translate

import dev.nikomaru.advancedshopfinder.utils.translate.serializer.NamespacedKeySerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.bukkit.NamespacedKey
import org.koin.core.component.KoinComponent
import java.io.InputStream
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@OptIn(ExperimentalSerializationApi::class)
class TranslateManagerImpl : TranslateManager, KoinComponent {
    var translateData: TranslateData

    private var langList: ArrayList<String> = arrayListOf()
    private val json = Json {
        prettyPrint = true
        isLenient = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun getTranslateMap(locale: Locale): Map<NamespacedKey, String> {

        if (!langList.contains(locale.toString().lowercase())) {
            println("Supported Lang :${langList.joinToString(", ")}",)
            throw IllegalArgumentException("Locale not found")
        }
        translateData.data[locale]?.let {
            return it
        }
        val classLoader = this.javaClass.classLoader
        val inputStream: InputStream = classLoader.getResourceAsStream("minecraft/${locale.toString().lowercase()}.json")!!
        val translateMap: Map<NamespacedKey, String> = json.decodeFromStream(MapSerializer(NamespacedKeySerializer, String.serializer()), inputStream)
        translateData.data += (locale to translateMap)
        return translateMap
    }

    init {
        val jarPath = Paths.get(
            this::class.java.protectionDomain.codeSource.location.toURI()
        ).toString()

        ZipFile(jarPath).use { zipFile ->
            val resourceList = listResources(zipFile, "minecraft/")

            langList.addAll(resourceList.map { it.split("/").last().split(".").first() })
        }
        val resourceDir = "minecraft"
        val classLoader = this.javaClass.classLoader


        val dataMap = mutableMapOf<Locale, Map<NamespacedKey, String>>()

        val preloadLang = listOf<String>("en_us", "ja_jp")

        for (lang in preloadLang) {
            val locale = Locale.of(lang)
            val inputStream: InputStream = classLoader.getResourceAsStream("$resourceDir/${lang}.json")!!
            val translateMap: Map<NamespacedKey, String> = json.decodeFromStream(MapSerializer(NamespacedKeySerializer, String.serializer()), inputStream)
            dataMap[locale] = translateMap
        }

        translateData = TranslateData(dataMap)
    }

    /**
     * Lists files within the given directory path inside the JAR (represented by the ZipFile).
     * @param zipFile The opened JAR file.
     * @param resourcePath Directory path inside the JAR to filter entries by (e.g., "resources/").
     * @return A list of file paths (entries) matching the given directory.
     */
    private fun listResources(zipFile: ZipFile, resourcePath: String): List<String> {
        val files = mutableListOf<String>()
        val normalizedPath = if (resourcePath.endsWith("/")) resourcePath else "$resourcePath/"

        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            val entry: ZipEntry = entries.nextElement()
            val entryName = entry.name
            // Check if the entry is not a directory and it starts with the specified path
            if (!entry.isDirectory && entryName.startsWith(normalizedPath)) {
                files.add(entryName)
            }
        }
        return files
    }
}