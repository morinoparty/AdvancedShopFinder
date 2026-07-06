package dev.nikomaru.advancedshopfinder.commands

import dev.nikomaru.advancedshopfinder.commands.utils.resolveFindOption
import dev.nikomaru.advancedshopfinder.search.EnchantBookSearcher
import dev.nikomaru.advancedshopfinder.search.Searcher
import dev.nikomaru.advancedshopfinder.services.ShopListPresenter
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.koin.core.component.KoinComponent

@Command("advancedshopfinder|asf|shopfinder|sf")
object EnchantFindCommand : KoinComponent {
    private val presenter = ShopListPresenter()

    @Command("search-book <enchantment>")
    suspend fun searchEnchantmentBook(
        sender: CommandSender,
        @Argument("enchantment") enchantment: Enchantment,
        @Flag(value = "profile", aliases = ["p"]) profile: String?,
    ) {
        val options = resolveFindOption(sender, profile) ?: return
        val searcher: Searcher<Enchantment> = EnchantBookSearcher()
        val header = MiniMessage.miniMessage().deserialize("<color:green>${enchantment.key.key}のエンチャント本を検索中")
        searcher
            .search(enchantment)
            .mapLeft {
                sender.sendRichMessage(it.message)
            }.map { shops ->
                presenter.present(sender, shops, options, header)
            }
    }
}
