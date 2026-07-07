package dev.nikomaru.advancedshopfinder.search

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.ghostchu.quickshop.api.QuickShopAPI
import com.ghostchu.quickshop.api.shop.Shop
import dev.nikomaru.advancedshopfinder.search.error.ShopSearchError
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.koin.core.component.inject

class EnchantBookSearcher : Searcher<Enchantment> {
    val quickShopAPI: QuickShopAPI by inject()

    override fun search(query: Enchantment): Either<ShopSearchError, List<Shop>> {
        val shop =
            quickShopAPI.shopManager.allShops.filter {
                it.item.type == Material.ENCHANTED_BOOK &&
                    (it.item.itemMeta as EnchantmentStorageMeta).hasStoredEnchant(
                        query,
                    )
            }
        if (shop.isEmpty()) return ShopSearchError.NO_SHOP_FOUND.left()
        return shop.filterNotNull().right()
    }
}
