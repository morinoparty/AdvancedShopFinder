package dev.nikomaru.advancedshopfinder.utils.data

/**
 * 並び替えの基準（向きを持たない）。GUI では 4 つの基準を常に並べ、それぞれの有効/無効・向き・位置を切り替える。
 *
 * 宣言順が GUI の初期配置になる。
 */
enum class SortCriterion(
    val label: String,
    val ascLabel: String,
    val descLabel: String,
    val asc: SortType,
    val desc: SortType,
) {
    PRICE_PER_ITEM("アイテム単価", "安い順", "高い順", SortType.ASC_PRICE_PER_ITEM, SortType.DESC_PRICE_PER_ITEM),
    DISTANCE("距離", "近い順", "遠い順", SortType.ASC_DISTANCE, SortType.DESC_DISTANCE),
    DISTANCE_NEAREST("最寄りの町", "近い順", "遠い順", SortType.ASC_DISTANCE_NEAREST, SortType.DESC_DISTANCE_NEAREST),
    PRICE_PER_STACK("スタック単価", "安い順", "高い順", SortType.ASC_PRICE_PER_STACK, SortType.DESC_PRICE_PER_STACK),
    ;

    fun sortType(descending: Boolean): SortType = if (descending) desc else asc

    companion object {
        fun of(type: SortType): SortCriterion = entries.first { it.asc == type || it.desc == type }
    }
}

/**
 * GUI 上の 1 基準分の状態。リスト内の位置が優先度（左ほど優先）を表す。
 */
data class SortEntry(
    val criterion: SortCriterion,
    val descending: Boolean = false,
    val enabled: Boolean = false,
) {
    fun toSortType(): SortType = criterion.sortType(descending)
}

/**
 * 保存形式の優先順位リストを、全基準を 1 つずつ含む [SortEntry] のリストに変換する。
 *
 * 有効な基準を保存順に先頭へ並べ（同じ基準が重複していれば最初のものだけを採用）、
 * 残りの基準は無効として [SortCriterion] の宣言順で後ろに並べる。
 */
fun List<SortType>.toSortEntries(): List<SortEntry> {
    val enabled =
        map { SortEntry(SortCriterion.of(it), descending = it == SortCriterion.of(it).desc, enabled = true) }
            .distinctBy { it.criterion }
    val disabled =
        SortCriterion.entries
            .filter { criterion -> enabled.none { it.criterion == criterion } }
            .map { SortEntry(it) }
    return enabled + disabled
}

/** 有効な基準だけを優先順に並べた保存形式のリストに変換する。 */
fun List<SortEntry>.toSortTypes(): List<SortType> = filter { it.enabled }.map { it.toSortType() }
