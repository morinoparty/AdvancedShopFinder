package dev.nikomaru.advancedshopfinder.commands

import com.ghostchu.quickshop.api.QuickShopAPI
import dev.nikomaru.advancedshopfinder.commands.utils.resolveFindOption
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


@Command("advancedshopfinder|asf|shopfinder|sf")
object EnchantFindCommand: KoinComponent {
    private val quickShop: QuickShopAPI by inject()

    @Command("search-book <enchantment>")
    suspend fun enchantFind(
        sender: CommandSender,
        @Argument("enchantment") enchantment: Enchantment,
        @Flag(value = "profile", aliases = ["p"]) profile: String?,
    ) {
        val options = resolveFindOption(sender, profile) ?: return
        val shop = quickShop.shopManager.allShops.filter {
            it.item.type == Material.ENCHANTED_BOOK && (it.item.itemMeta as EnchantmentStorageMeta).hasStoredEnchant(
                enchantment
            )
        }

        if (shop.isEmpty()) {
            sender.sendRichMessage("検索結果: 0件")
            return
        }
        var message: Component = Component.text("")
        var sum = 0

        val (newSellMessage, newSellSum) = ShopSearchCommand.processShops(
            shop, sender, message, sum, options, buying = false
        )
        message = newSellMessage
        sum = newSellSum


        val (newBuyMessage, newBuySum) = ShopSearchCommand.processShops(
            shop, sender, message, sum, options, buying = true
        )
        message = newBuyMessage
        sum = newBuySum

        sender.sendRichMessage("<color:green><lang:${enchantment.key.key}> の検索結果: ${sum}件")
        sender.sendMessage(message)

    }

}