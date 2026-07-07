package dev.nikomaru.advancedshopfinder.search

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.ghostchu.quickshop.api.QuickShopAPI
import com.ghostchu.quickshop.api.shop.Shop
import dev.nikomaru.advancedshopfinder.files.server.ConfigData
import dev.nikomaru.advancedshopfinder.search.error.ShopSearchError
import dev.nikomaru.advancedshopfinder.utils.ComponentUtils.toPlainText
import dev.nikomaru.advancedshopfinder.utils.translate.TranslateManager
import org.koin.core.component.inject
import java.util.*

class FuzzySearcher : Searcher<Pair<String, Locale>> {
    val quickShopAPI: QuickShopAPI by inject()
    val translateManager: TranslateManager by inject()
    val config: ConfigData by inject()

    override fun search(query: Pair<String, Locale>): Either<ShopSearchError, List<Shop>> {
        val keys =
            translateManager
                .getTranslateMap(query.second)
                .filter { (k, v) ->
                    v.contains(query.first) || k.key.contains(query.first)
                }.map {
                    it.key
                }

        val limit = config.fuzzySearchLimit

        if (keys.size > limit) {
            return ShopSearchError.LIMIT_EXCEED.left()
        }

        val shop =
            quickShopAPI.shopManager.allShops.filter {
                keys.map { key -> key.key }.contains(
                    it.item.type
                        .translationKey()
                        .lowercase(),
                ) ||
                    it.item
                        .displayName()
                        .toPlainText()
                        .contains(query.first)
            }

        if (shop.isEmpty()) {
            return ShopSearchError.NO_SHOP_FOUND.left()
        }

        if (shop.size > limit) {
            return ShopSearchError.LIMIT_EXCEED.left()
        }
        return shop.filterNotNull().right()
    }
}
