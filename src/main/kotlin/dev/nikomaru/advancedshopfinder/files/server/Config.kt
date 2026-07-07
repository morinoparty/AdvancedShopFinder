package dev.nikomaru.advancedshopfinder.files.server

import dev.nikomaru.advancedshopfinder.AdvancedShopFinder
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

object Config : KoinComponent {
    private val plugin: AdvancedShopFinder by inject()
    val json: Json =
        Json {
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    fun loadConfig() {
        val configFile = plugin.dataFolder.resolve("config.json")

        if (!configFile.exists()) {
            val defaultConfigData =
                ConfigData(
                    arrayListOf(
                        PlaceData(-532, -85, "もりもと"),
                    ),
                )
            plugin.dataFolder.mkdir()
            configFile.createNewFile()
            configFile.writeText(json.encodeToString(defaultConfigData))
        }
        val config = json.decodeFromString<ConfigData>(configFile.readText())
        loadKoinModules(
            module {
                single { config }
            },
        )
    }
}

@Serializable
data class ConfigData(
    val placeData: List<PlaceData>,
    val fuzzySearchLimit: Int = 50,
    val format: String = "<shop-type>: オーナー:<green><player-name></green> 値段: <green><price>/<shop-stacking-amount></green>個 在庫: <green><count></green> \n座標: <yellow><world></yellow> x:<blue><x></blue> y:<blue><y></blue> z:<blue><z></blue> 距離: <green><distance></green>ブロック 最寄り: <near-town>から<green><near-town-distance></green>ブロック",
)

@Serializable
data class PlaceData(
    val x: Int,
    val z: Int,
    val placeName: String,
)
