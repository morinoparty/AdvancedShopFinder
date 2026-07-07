package dev.nikomaru.advancedshopfinder.files

import dev.nikomaru.advancedshopfinder.files.server.Config
import dev.nikomaru.advancedshopfinder.utils.data.FindOption
import dev.nikomaru.advancedshopfinder.utils.data.LimitAmountOption
import dev.nikomaru.advancedshopfinder.utils.data.SortOption
import dev.nikomaru.advancedshopfinder.utils.data.SortType
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 複数プロファイルを持つ [PlayerFindOption] が JSON ラウンドトリップで保持されることを検証する。
 * `sf setting` / `-p` 機能はこのシリアライズ契約に依存している。
 */
class PlayerFindOptionSerializationTest {
    @Test
    fun multiProfileRoundTrip() {
        val original =
            PlayerFindOption(
                setting = "mining",
                findOptions =
                    hashMapOf(
                        "default" to FindOption(),
                        "mining" to
                            FindOption(
                                limitAmountOption = LimitAmountOption(buyFindLimit = 5, sellFindLimit = 10),
                                sortOption =
                                    SortOption(
                                        buySortTypes = listOf(SortType.ASC_DISTANCE, SortType.DESC_PRICE_PER_ITEM),
                                        sellSortTypes = listOf(SortType.DESC_DISTANCE_NEAREST),
                                    ),
                                showNoStockShop = true,
                            ),
                    ),
            )

        val json = Config.json.encodeToString(original)
        val decoded = Config.json.decodeFromString<PlayerFindOption>(json)

        assertEquals(original, decoded)
        assertEquals("mining", decoded.setting)
        assertEquals(2, decoded.findOptions.size)
        assertEquals(
            listOf(SortType.ASC_DISTANCE, SortType.DESC_PRICE_PER_ITEM),
            decoded.findOptions["mining"]!!.sortOption.buySortTypes,
        )
        assertEquals(5, decoded.findOptions["mining"]!!.limitAmountOption.buyFindLimit)
    }
}
