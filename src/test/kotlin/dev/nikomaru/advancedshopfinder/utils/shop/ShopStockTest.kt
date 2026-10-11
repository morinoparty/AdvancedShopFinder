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
        // 在庫が分からないショップを在庫切れとして落とさない
        assertTrue(ShopStock.Unknown.isVisible(showNoStockShop = false))
        assertTrue(ShopStock.Unlimited.isVisible(showNoStockShop = false))
    }

    @Test
    fun outOfStockShopsFollowTheProfileSetting() {
        val empty = ShopStock.Counted(amount = 0)
        assertFalse(empty.isVisible(showNoStockShop = false))
        assertTrue(empty.isVisible(showNoStockShop = true))
        assertTrue(ShopStock.Counted(amount = 3).isVisible(showNoStockShop = false))
    }

    @Test
    fun limitTakesTheFirstEntries() {
        val shops = listOf(1, 2, 3, 4, 5)
        assertEquals(listOf(1, 2), shops.limitedTo(2))
        assertEquals(shops, shops.limitedTo(10))
    }

    @Test
    fun negativeLimitMeansUnlimited() {
        val shops = listOf(1, 2, 3)
        assertEquals(shops, shops.limitedTo(-1))
    }

    @Test
    fun zeroLimitShowsNothing() {
        // プロファイルで 0 件にした種類（買取・販売）は検索結果に出さない
        assertEquals(emptyList<Int>(), listOf(1, 2, 3).limitedTo(0))
    }

    @Test
    fun buyingShopIsLimitedByOwnerBalance() {
        // 1 取引 10 円で所持金 35 円なら、空き容量が 5 でも 3 回分しか買い取れない
        assertEquals(ShopStock.Counted(3, limitedByMoney = true), ShopStock.buyable(space = 5, price = 10.0, balance = 35.0))
        // 所持金が十分なら空き容量で決まる
        assertEquals(ShopStock.Counted(5), ShopStock.buyable(space = 5, price = 10.0, balance = 1000.0))
        // 所持金が分からない・価格が 0 の場合は空き容量だけで数える
        assertEquals(ShopStock.Counted(5), ShopStock.buyable(space = 5, price = 10.0, balance = null))
        assertEquals(ShopStock.Counted(5), ShopStock.buyable(space = 5, price = 0.0, balance = 0.0))
        // 借金（負の所持金）は 0 として扱う
        assertEquals(ShopStock.Counted(0, limitedByMoney = true), ShopStock.buyable(space = 5, price = 10.0, balance = -50.0))
    }

    @Test
    fun ownerWithoutMoneyFollowsTheProfileSetting() {
        val broke = ShopStock.Counted(amount = 0, limitedByMoney = true)
        assertFalse(broke.isVisible(showNoStockShop = true, showOwnerNotHasEnoughMoneyShop = false))
        assertTrue(broke.isVisible(showNoStockShop = false, showOwnerNotHasEnoughMoneyShop = true))
    }
}
