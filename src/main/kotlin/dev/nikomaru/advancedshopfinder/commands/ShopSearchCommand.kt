package dev.nikomaru.advancedshopfinder.commands

import dev.nikomaru.advancedshopfinder.commands.utils.resolveFindOption
import dev.nikomaru.advancedshopfinder.search.ItemSearcher
import dev.nikomaru.advancedshopfinder.services.ShopListPresenter
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Flag
import org.koin.core.component.KoinComponent

@Command("advancedshopfinder|asf|shopfinder|sf")
object ShopSearchCommand : KoinComponent {
    private val presenter = ShopListPresenter()

    @Command("search <item>")
    @CommandDescription("アイテムを検索します")
    suspend fun searchItem(
        sender: CommandSender,
        @Argument("item") itemArray: Array<Material>,
        @Flag(value = "profile", aliases = ["p"]) profile: String?,
    ) {
        val playerFindOption = resolveFindOption(sender, profile) ?: return
        val searcher = ItemSearcher()
        val header =
            MiniMessage.miniMessage().deserialize("<color:green><lang:${itemArray.first().translationKey()}>を検索中")
        searcher
            .search(itemArray)
            .mapLeft {
                sender.sendRichMessage(it.message)
            }.map { shops ->
                presenter.present(sender, shops, playerFindOption, header)
            }
    }
}
