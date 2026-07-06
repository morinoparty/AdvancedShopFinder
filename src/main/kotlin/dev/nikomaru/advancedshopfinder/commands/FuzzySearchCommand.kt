package dev.nikomaru.advancedshopfinder.commands

import com.ghostchu.quickshop.api.QuickShopAPI
import dev.nikomaru.advancedshopfinder.commands.utils.resolveFindOption
import dev.nikomaru.advancedshopfinder.files.server.ConfigData
import dev.nikomaru.advancedshopfinder.utils.translate.TranslateManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.util.Locale

@Command("advancedshopfinder|asf|shopfinder|sf")
object FuzzySearchCommand : KoinComponent {
    private val translateManager: TranslateManager by inject()
    private val quickShop: QuickShopAPI by inject()

    @Command("fuzzy-search <name>")
    suspend fun fuzzySearch(
        sender: CommandSender,
        @Argument("name") name: String,
        @Flag(value = "profile", aliases = ["p"]) profile: String?,
    ) {
        val options = resolveFindOption(sender, profile) ?: return
        val locale = if (sender is Player) sender.locale() else Locale.getDefault()
        val needle = name.lowercase()
        val keySet = translateManager.getTranslateMap(locale)
            .filter { (k, v) -> v.lowercase().contains(needle) || k.key.lowercase().contains(needle) }
            .keys
            .map { it.key.lowercase() }
            .toHashSet()

        val plain = PlainTextComponentSerializer.plainText()
        val shop = quickShop.shopManager.allShops.filter {
            keySet.contains(it.item.type.translationKey().lowercase())
                || plain.serialize(it.item.displayName()).lowercase().contains(needle)
        }
        if (shop.isEmpty()) {
            sender.sendRichMessage("検索結果: 0件")
            return
        }
        if (shop.size > get<ConfigData>().fuzzySearchLimit) {
            sender.sendRichMessage("検索結果が多すぎます。絞り込んでください。")
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

        sender.sendRichMessage("<color:green>${name} の検索結果: ${sum}件")
        sender.sendMessage(message)
    }
}
