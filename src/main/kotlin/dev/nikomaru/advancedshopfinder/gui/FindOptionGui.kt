package dev.nikomaru.advancedshopfinder.gui

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import dev.nikomaru.advancedshopfinder.AdvancedShopFinder
import dev.nikomaru.advancedshopfinder.utils.data.FindOption
import dev.nikomaru.advancedshopfinder.utils.data.PlayerFindOptionUtils.setActiveProfile
import dev.nikomaru.advancedshopfinder.utils.data.PlayerFindOptionUtils.upsertProfile
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 検索オプション（プロファイル）をインベントリ GUI で編集する画面。
 *
 * クリックで並び順・件数上限・表示フラグを変更し、保存ボタンで永続化する。
 */
class FindOptionGui(
    private val player: Player,
    private val profileName: String,
    initial: FindOption,
) : KoinComponent {
    private val plugin: AdvancedShopFinder by inject()

    private var option: FindOption = initial

    private val gui = ChestGui(6, "検索設定: $profileName")
    private val pane = StaticPane(0, 0, 9, 6)

    private val buySortItem = GuiItem(ItemStack(Material.GOLD_INGOT)) { event ->
        event.isCancelled = true
        val sort = option.sortOption
        val next = if (event.isRightClick) sort.buySortType.previous() else sort.buySortType.next()
        option = option.copy(sortOption = sort.copy(buySortType = next))
        redraw()
    }
    private val sellSortItem = GuiItem(ItemStack(Material.EMERALD)) { event ->
        event.isCancelled = true
        val sort = option.sortOption
        val next = if (event.isRightClick) sort.sellSortType.previous() else sort.sellSortType.next()
        option = option.copy(sortOption = sort.copy(sellSortType = next))
        redraw()
    }
    private val buyLimitItem = GuiItem(ItemStack(Material.CHEST)) { event ->
        event.isCancelled = true
        val limit = option.limitAmountOption
        option = option.copy(limitAmountOption = limit.copy(buyFindLimit = nextLimit(limit.buyFindLimit, event.click)))
        redraw()
    }
    private val sellLimitItem = GuiItem(ItemStack(Material.HOPPER)) { event ->
        event.isCancelled = true
        val limit = option.limitAmountOption
        option = option.copy(limitAmountOption = limit.copy(sellFindLimit = nextLimit(limit.sellFindLimit, event.click)))
        redraw()
    }
    private val noStockItem = GuiItem(ItemStack(Material.BARRIER)) { event ->
        event.isCancelled = true
        option = option.copy(showNoStockShop = !option.showNoStockShop)
        redraw()
    }
    private val ownerMoneyItem = GuiItem(ItemStack(Material.GOLD_NUGGET)) { event ->
        event.isCancelled = true
        option = option.copy(showOwnerNotHasEnoughMoneyShop = !option.showOwnerNotHasEnoughMoneyShop)
        redraw()
    }
    private val activeItem = GuiItem(ItemStack(Material.NAME_TAG)) { event ->
        event.isCancelled = true
        plugin.launch {
            player.setActiveProfile(profileName)
            player.sendRichMessage("<green>プロファイル '$profileName' を使用中に設定しました。")
        }
    }
    private val saveItem = GuiItem(ItemStack(Material.LIME_CONCRETE)) { event ->
        event.isCancelled = true
        val snapshot = option
        plugin.launch {
            player.upsertProfile(profileName, snapshot)
            player.sendRichMessage("<green>プロファイル '$profileName' を保存しました。")
        }
        player.closeInventory()
    }
    private val closeItem = GuiItem(ItemStack(Material.RED_CONCRETE)) { event ->
        event.isCancelled = true
        player.closeInventory()
    }

    init {
        gui.setOnGlobalClick { it.isCancelled = true }
        pane.addItem(buySortItem, 1, 1)
        pane.addItem(sellSortItem, 3, 1)
        pane.addItem(buyLimitItem, 1, 2)
        pane.addItem(sellLimitItem, 3, 2)
        pane.addItem(noStockItem, 5, 1)
        pane.addItem(ownerMoneyItem, 5, 2)
        pane.addItem(activeItem, 7, 1)
        pane.addItem(saveItem, 3, 5)
        pane.addItem(closeItem, 5, 5)
        gui.addPane(pane)
        redraw()
    }

    /** GUI を表示する（メインスレッドで呼び出すこと）。 */
    fun open() {
        gui.show(player)
    }

    private fun redraw() {
        val sort = option.sortOption
        val limit = option.limitAmountOption

        buySortItem.item =
            icon(
                Material.GOLD_INGOT,
                "<gold>買取ショップの並び順",
                "<white>${sort.buySortType.name}",
                "<gray>${sort.buySortType.description}",
                "",
                "<yellow>左クリック: 次へ / 右クリック: 前へ",
            )
        sellSortItem.item =
            icon(
                Material.EMERALD,
                "<green>販売ショップの並び順",
                "<white>${sort.sellSortType.name}",
                "<gray>${sort.sellSortType.description}",
                "",
                "<yellow>左クリック: 次へ / 右クリック: 前へ",
            )
        buyLimitItem.item =
            icon(
                Material.CHEST,
                "<gold>買取ショップの取得件数",
                "<white>${limitText(limit.buyFindLimit)}",
                "",
                "<yellow>左クリック: +1 / 右クリック: -1 / Shift: 無制限",
            )
        sellLimitItem.item =
            icon(
                Material.HOPPER,
                "<green>販売ショップの取得件数",
                "<white>${limitText(limit.sellFindLimit)}",
                "",
                "<yellow>左クリック: +1 / 右クリック: -1 / Shift: 無制限",
            )
        noStockItem.item =
            icon(
                if (option.showNoStockShop) Material.LIME_DYE else Material.GRAY_DYE,
                "<aqua>在庫切れショップを表示",
                "<white>${onOff(option.showNoStockShop)}",
                "",
                "<yellow>クリックで切り替え",
            )
        ownerMoneyItem.item =
            icon(
                if (option.showOwnerNotHasEnoughMoneyShop) Material.LIME_DYE else Material.GRAY_DYE,
                "<aqua>オーナーの所持金不足ショップを表示",
                "<white>${onOff(option.showOwnerNotHasEnoughMoneyShop)}",
                "",
                "<yellow>クリックで切り替え",
            )
        activeItem.item =
            icon(
                Material.NAME_TAG,
                "<light_purple>このプロファイルを使用中にする",
                "<white>プロファイル: $profileName",
                "",
                "<yellow>クリックで使用中に設定",
            )
        saveItem.item =
            icon(
                Material.LIME_CONCRETE,
                "<green>保存",
                "<gray>変更を保存して閉じます",
            )
        closeItem.item =
            icon(
                Material.RED_CONCRETE,
                "<red>閉じる",
                "<gray>保存せずに閉じます",
            )

        gui.update()
    }

    private fun nextLimit(
        current: Int,
        click: ClickType,
    ): Int =
        when {
            click.isShiftClick -> -1
            click.isRightClick -> if (current <= 0) -1 else current - 1
            else -> if (current < 0) 1 else current + 1
        }

    private fun limitText(value: Int): String = if (value < 0) "無制限" else "$value 件"

    private fun onOff(value: Boolean): String = if (value) "ON" else "OFF"

    private fun icon(
        material: Material,
        name: String,
        vararg lore: String,
    ): ItemStack {
        val mm = MiniMessage.miniMessage()
        return ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(mm.deserialize(name).decoration(TextDecoration.ITALIC, false))
                if (lore.isNotEmpty()) {
                    meta.lore(
                        lore.map { line ->
                            if (line.isEmpty()) {
                                Component.empty()
                            } else {
                                mm.deserialize(line).decoration(TextDecoration.ITALIC, false)
                            }
                        },
                    )
                }
            }
        }
    }
}
