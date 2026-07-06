package dev.nikomaru.advancedshopfinder.utils.data

enum class SortType(
    val description: String,
) {
    ASC_PRICE_PER_STACK("スタック当たりの金額が安い順に並び替えを行います"),
    DESC_PRICE_PER_STACK("スタック当たりの金額が高い順に並び替えを行います"),
    ASC_PRICE_PER_ITEM("アイテム当たりの金額が安い順に並び替えを行います"),
    DESC_PRICE_PER_ITEM("アイテム当たりの金額が高い順に並び替えを行います"),
    ASC_DISTANCE("距離が近い順に並び替えを行います"),
    DESC_DISTANCE("距離が遠い順に並び替えを行います"),
    ASC_DISTANCE_NEAREST("最も近い順に並び替えを行います"),
    DESC_DISTANCE_NEAREST("最も遠い順に並び替えを行います"),
    ;

    /** 次の並び順（末尾なら先頭へ循環）。GUI でのクリック切り替えに使う。 */
    fun next(): SortType = entries[(ordinal + 1) % entries.size]

    /** 前の並び順（先頭なら末尾へ循環）。 */
    fun previous(): SortType = entries[(ordinal - 1 + entries.size) % entries.size]
}
