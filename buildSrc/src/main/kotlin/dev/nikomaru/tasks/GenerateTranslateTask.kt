package dev.nikomaru.tasks

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.bukkit.NamespacedKey
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

open class GenerateTranslateTask : DefaultTask() {

    @TaskAction
    fun runTask() {
        val resourceDir: Path =
            Paths.get(project.projectDir.absolutePath, "src", "main", "resources", "minecraft")
        if (!Files.exists(resourceDir)) {
            Files.createDirectories(resourceDir)
        }

        val url = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

        val data = URI(url).toURL().readText()

        val gson = Gson()

//        val version = gson.fromJson(data, JsonObject::class.java).get("latest").asJsonObject.get("release").asString
        val version = "1.21.11"
        println("latestVersion: $version")


        val langListUrl =
            "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/$version/assets/minecraft/lang/_list.json"


        val lang = URI(langListUrl).toURL().readText()

        val langJson = gson.fromJson(lang, JsonObject::class.java)

        val langList = langJson.get("files").asJsonArray.toList().map { it.asString.split(".")[0] }

        println("detectedLang: $langList")

        langList.parallelStream().forEach { lang ->
            val individualLangUri =
                "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/$version/assets/minecraft/lang/$lang.json"
            val text = URI(individualLangUri).toURL().readText()
            val map = gson.fromJson(text, Map::class.java)

            val translateMap = mutableMapOf<String, String>()
            map.filter { (t, _) ->
                t.toString().startsWith("item.minecraft") || t.toString().startsWith("block.minecraft") || t.toString().startsWith("enchantment.minecraft")
            }.forEach { (t, u) ->
                translateMap += t.toString() to u.toString().replace(" ","_")
            }

            val translateMap2 = translateMap.map { (k, v) ->
                try {
                    NamespacedKey.minecraft(k)
                } catch (e: Exception) {
                    println("error: $k, $v")
                }
                Pair(NamespacedKey.minecraft(k), v)
            }.toMap()

            val json = Json {
                prettyPrint = true
                isLenient = true
                encodeDefaults = true
                ignoreUnknownKeys = true
            }

            val output = json.encodeToString(MapSerializer(NamespacedKeySerializer, String.serializer()), translateMap2)

            val outputFile = resourceDir.resolve("$lang.json").toFile()
            outputFile.parentFile.mkdirs()
            outputFile.createNewFile()
            outputFile.writeText(output)
        }
    }
}