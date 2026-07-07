package dev.nikomaru.advancedshopfinder.search.error

enum class ShopSearchError(
    val message: String,
) {
    NO_SHOP_FOUND("<yellow>指定されたショップが見つかりませんでした"),
    LIMIT_EXCEED("<red>検索結果が多すぎます"),
}
