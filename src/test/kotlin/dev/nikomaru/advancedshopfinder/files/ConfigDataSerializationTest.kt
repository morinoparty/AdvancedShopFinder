package dev.nikomaru.advancedshopfinder.files

import dev.nikomaru.advancedshopfinder.files.server.Config
import dev.nikomaru.advancedshopfinder.files.server.ConfigData
import dev.nikomaru.advancedshopfinder.files.server.PlaceData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * [PlaceData] に追加した `world` フィールドの後方互換性を検証する。
 * 既存サーバーの config.json には `world` が無いため、省略時はオーバーワールド扱いになる必要がある。
 */
class ConfigDataSerializationTest {
    @Test
    fun legacyPlaceDataWithoutWorldDefaultsToOverworld() {
        val legacyJson =
            """
            {
              "placeData": [
                { "x": -532, "z": -85, "placeName": "もりもと" }
              ]
            }
            """.trimIndent()

        val decoded = Config.json.decodeFromString<ConfigData>(legacyJson)

        assertEquals(1, decoded.placeData.size)
        assertEquals("world", decoded.placeData.first().world)
    }

    @Test
    fun placeDataWorldRoundTrip() {
        val original =
            ConfigData(
                placeData =
                    listOf(
                        PlaceData(-532, -85, "もりもと"),
                        PlaceData(100, 200, "エンド拠点", world = "world_the_end"),
                    ),
            )

        val decoded = Config.json.decodeFromString<ConfigData>(Config.json.encodeToString(original))

        assertEquals(original, decoded)
        assertEquals("world_the_end", decoded.placeData[1].world)
    }
}
