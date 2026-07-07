package dev.nikomaru.advancedshopfinder.gui

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.github.stefvanschie.inventoryframework.pane.util.Slot
import dev.nikomaru.advancedshopfinder.AdvancedShopFinder
import dev.nikomaru.advancedshopfinder.utils.data.FindOption
import dev.nikomaru.advancedshopfinder.utils.data.PlayerFindOptionUtils.setActiveProfile
import dev.nikomaru.advancedshopfinder.utils.data.PlayerFindOptionUtils.upsertProfile
import dev.nikomaru.advancedshopfinder.utils.data.SortType
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
 * 並び順は単一ではなく「優先順位リスト」として編集でき、先頭が第一ソート基準になる。
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
    private val pane = StaticPane(9, 6)

    private val buyLabelItem = GuiItem(ItemStack(Material.PAPER)) { it.isCancelled = true }
    private val sellLabelItem = GuiItem(ItemStack(Material.PAPER)) { it.isCancelled = true }

    // 並び順の優先度スロット（買取・販売それぞれ MAX_SORT 個ずつ常設し、内容を再描画で切り替える）
    private val buySortSlots = createSortSlots(buy = true)
    private val sellSortSlots = createSortSlots(buy = false)

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
        pane.addItem(buyLabelItem, 0, 1)
        buySortSlots.forEachIndexed { i, item -> pane.addItem(item, i + 1, 1) }
        pane.addItem(sellLabelItem, 0, 2)
        sellSortSlots.forEachIndexed { i, item -> pane.addItem(item, i + 1, 2) }
        pane.addItem(buyLimitItem, 1, 3)
        pane.addItem(sellLimitItem, 3, 3)
        pane.addItem(noStockItem, 5, 3)
        pane.addItem(ownerMoneyItem, 7, 3)
        pane.addItem(activeItem, 2, 5)
        pane.addItem(saveItem, 4, 5)
        pane.addItem(closeItem, 6, 5)
        gui.addPane(Slot.fromXY(0, 0), pane)
        redraw()
    }

    /** GUI を表示する（メインスレッドで呼び出すこと）。 */
    fun open() {
        gui.show(player)
    }

    private fun createSortSlots(buy: Boolean): List<GuiItem> =
        (0 until MAX_SORT).map { index ->
            GuiItem(ItemStack(Material.GRAY_STAINED_GLASS_PANE)) { event ->
                event.isCancelled = true
                onSortSlotClick(buy, index, event.click)
                redraw()
            }
        }

    private fun sortList(buy: Boolean): List<SortType> =
        if (buy) option.sortOption.buySortTypes else option.sortOption.sellSortTypes

    private fun setSortList(
        buy: Boolean,
        list: List<SortType>,
    ) {
        val sort = option.sortOption
        option =
            option.copy(
                sortOption = if (buy) sort.copy(buySortTypes = list) else sort.copy(sellSortTypes = list),
            )
    }

    private fun onSortSlotClick(
        buy: Boolean,
        index: Int,
        click: ClickType,
    ) {
        val list = sortList(buy)
        when {
            index < list.size -> {
                val mutable = list.toMutableList()
                when (click) {
                    ClickType.LEFT -> mutable[index] = mutable[index].next()
                    ClickType.RIGHT -> mutable.removeAt(index)
                    ClickType.SHIFT_LEFT -> if (index > 0) mutable.swap(index, index - 1)
                    ClickType.SHIFT_RIGHT -> if (index < mutable.size - 1) mutable.swap(index, index + 1)
                    else -> return
                }
                setSortList(buy, mutable)
            }
            index == list.size && list.size < MAX_SORT ->
                if (click == ClickType.LEFT) setSortList(buy, list + SortType.entries.first())
            else -> {}
        }
    }

    private fun MutableList<SortType>.swap(
        a: Int,
        b: Int,
    ) {
        val tmp = this[a]
        this[a] = this[b]
        this[b] = tmp
    }

    private fun redraw() {
        val limit = option.limitAmountOption

        buyLabelItem.item = icon(Material.PAPER, "<gold>買取ショップの並び順", "<gray>優先度の高い順（左が最優先）")
        sellLabelItem.item = icon(Material.PAPER, "<green>販売ショップの並び順", "<gray>優先度の高い順（左が最優先）")
        renderSortSlots(buy = true, slots = buySortSlots)
        renderSortSlots(buy = false, slots = sellSortSlots)

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
        saveItem.item = icon(Material.LIME_CONCRETE, "<green>保存", "<gray>変更を保存して閉じます")
        closeItem.item = icon(Material.RED_CONCRETE, "<red>閉じる", "<gray>保存せずに閉じます")

        gui.update()
    }

    private fun renderSortSlots(
        buy: Boolean,
        slots: List<GuiItem>,
    ) {
        val list = sortList(buy)
        slots.forEachIndexed { i, guiItem ->
            guiItem.item =
                when {
                    i < list.size -> {
                        val type = list[i]
                        icon(
                            sortMaterial(type),
                            "<white>優先度 ${i + 1}: <yellow>${type.label}",
                            "<gray>${type.description}",
                            "",
                            "<yellow>左: 種類変更 / 右: 削除",
                            "<yellow>Shift左: 上へ / Shift右: 下へ",
                            amount = i + 1,
                        )
                    }
                    i == list.size && list.size < MAX_SORT ->
                        icon(Material.LIME_DYE, "<green>+ 並び順を追加", "<yellow>左クリックで基準を追加")
                    else -> icon(Material.GRAY_STAINED_GLASS_PANE, " ")
                }
        }
    }

    private fun sortMaterial(type: SortType): Material =
        when {
            type.name.contains("PRICE") -> Material.GOLD_INGOT
            type.name.contains("NEAREST") -> Material.LODESTONE
            else -> Material.COMPASS
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
        amount: Int = 1,
    ): ItemStack {
        val mm = MiniMessage.miniMessage()
        return ItemStack(material, amount).apply {
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

    companion object {
        /** 各方向で設定できる並び順基準の最大数。 */
        private const val MAX_SORT = 5
    }
}
