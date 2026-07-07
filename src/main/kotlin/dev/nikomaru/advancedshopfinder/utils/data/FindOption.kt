package dev.nikomaru.advancedshopfinder.utils.data

import kotlinx.serialization.Serializable

@Serializable
data class FindOption(
    val version: Int = 1,
    val limitAmountOption: LimitAmountOption = LimitAmountOption(),
    val sortOption: SortOption = SortOption(),
    val showNoStockShop: Boolean = false,
    val showOwnerNotHasEnoughMoneyShop: Boolean = false,
)

@Serializable
data class LimitAmountOption(
    val buyFindLimit: Int = -1,
    val sellFindLimit: Int = -1,
)

/**
 * 並び替えの優先順位。リストの先頭が第一ソート基準となり、値が同じ場合に次の基準で比較する。
 */
@Serializable
data class SortOption(
    val buySortTypes: List<SortType> = listOf(SortType.DESC_PRICE_PER_ITEM),
    val sellSortTypes: List<SortType> = listOf(SortType.ASC_PRICE_PER_ITEM),
)
