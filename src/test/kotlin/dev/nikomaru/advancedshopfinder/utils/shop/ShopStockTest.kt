package dev.nikomaru.advancedshopfinder.utils.shop

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 検索結果の絞り込み（在庫切れの除外）と件数上限を検証する。
 */
class ShopStockTest {
    @Test
    fun unknownAndUnlimitedShopsAreAlwaysShown() {
        // チャンクが読み込まれていないショップ（在庫不明）を在庫切れとして落とさない
        assertTrue(ShopStock.Unknown.isVisible(showNoStockShop = false))
        assertTrue(ShopStock.Unlimited.isVisible(showNoStockShop = false))
    }

    @Test
    fun outOfStockShopsFollowTheProfileSetting() {
        val empty = ShopStock.Counted(amount = 0, cached = false)
        assertFalse(empty.isVisible(showNoStockShop = false))
        assertTrue(empty.isVisible(showNoStockShop = true))
        assertTrue(ShopStock.Counted(amount = 3, cached = true).isVisible(showNoStockShop = false))
    }

    @Test
    fun limitTakesTheFirstEntries() {
        val shops = listOf(1, 2, 3, 4, 5)
        assertEquals(listOf(1, 2), shops.limitedTo(2))
        assertEquals(shops, shops.limitedTo(10))
    }

    @Test
    fun nonPositiveLimitMeansUnlimited() {
        val shops = listOf(1, 2, 3)
        assertEquals(shops, shops.limitedTo(-1))
        assertEquals(shops, shops.limitedTo(0))
    }
}
