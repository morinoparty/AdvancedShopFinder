package dev.nikomaru.advancedshopfinder.search

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.ghostchu.quickshop.api.QuickShopAPI
import com.ghostchu.quickshop.api.shop.Shop
import dev.nikomaru.advancedshopfinder.files.server.ConfigData
import dev.nikomaru.advancedshopfinder.search.error.ShopSearchError
import dev.nikomaru.advancedshopfinder.utils.translate.TranslateManager
import org.bukkit.Material
import org.koin.core.component.inject

class ItemSearcher : Searcher<Array<Material>> {
    val quickShopAPI: QuickShopAPI by inject()
    val translateManager: TranslateManager by inject()
    val config: ConfigData by inject()

    override fun search(query: Array<Material>): Either<ShopSearchError, List<Shop>> {
        val shop =
            quickShopAPI.shopManager.allShops.filter {
                query.contains(it.item.type)
            }
        if (shop.isEmpty()) return ShopSearchError.NO_SHOP_FOUND.left()
        return shop.filterNotNull().right()
    }
}
