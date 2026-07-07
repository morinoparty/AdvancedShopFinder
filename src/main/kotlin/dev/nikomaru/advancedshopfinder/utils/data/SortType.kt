package dev.nikomaru.advancedshopfinder.utils.data

enum class SortType(
    val label: String,
    val description: String,
) {
    ASC_PRICE_PER_STACK("スタック単価 安い順", "スタック当たりの金額が安い順に並び替えを行います"),
    DESC_PRICE_PER_STACK("スタック単価 高い順", "スタック当たりの金額が高い順に並び替えを行います"),
    ASC_PRICE_PER_ITEM("アイテム単価 安い順", "アイテム当たりの金額が安い順に並び替えを行います"),
    DESC_PRICE_PER_ITEM("アイテム単価 高い順", "アイテム当たりの金額が高い順に並び替えを行います"),
    ASC_DISTANCE("距離 近い順", "距離が近い順に並び替えを行います"),
    DESC_DISTANCE("距離 遠い順", "距離が遠い順に並び替えを行います"),
    ASC_DISTANCE_NEAREST("最寄り拠点 近い順", "最寄りの拠点が近い順に並び替えを行います"),
    DESC_DISTANCE_NEAREST("最寄り拠点 遠い順", "最寄りの拠点が遠い順に並び替えを行います"),
    ;

    /** 次の並び順（末尾なら先頭へ循環）。GUI でのクリック切り替えに使う。 */
    fun next(): SortType = entries[(ordinal + 1) % entries.size]

    /** 前の並び順（先頭なら末尾へ循環）。 */
    fun previous(): SortType = entries[(ordinal - 1 + entries.size) % entries.size]
}
