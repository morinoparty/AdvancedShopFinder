package dev.nikomaru.advancedshopfinder.commands

import dev.nikomaru.advancedshopfinder.gui.FindOptionGui
import dev.nikomaru.advancedshopfinder.utils.coroutines.minecraft
import dev.nikomaru.advancedshopfinder.utils.data.FindOption
import dev.nikomaru.advancedshopfinder.utils.data.PlayerFindOptionUtils.createProfile
import dev.nikomaru.advancedshopfinder.utils.data.PlayerFindOptionUtils.deleteProfile
import dev.nikomaru.advancedshopfinder.utils.data.PlayerFindOptionUtils.getPlayerFindOption
import dev.nikomaru.advancedshopfinder.utils.data.PlayerFindOptionUtils.loadPlayerFindOption
import dev.nikomaru.advancedshopfinder.utils.data.PlayerFindOptionUtils.setActiveProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.koin.core.component.KoinComponent

@Command("advancedshopfinder|asf|shopfinder|sf")
object SettingCommand : KoinComponent {
    @Command("setting")
    @CommandDescription("使用中プロファイルの検索設定GUIを開きます")
    suspend fun openActive(sender: CommandSender) {
        val player = sender.asPlayer() ?: return
        val config = player.loadPlayerFindOption()
        openGui(player, config.setting)
    }

    @Command("setting <profile>")
    @CommandDescription("指定プロファイルの検索設定GUIを開きます（無ければ作成）")
    suspend fun openProfile(
        sender: CommandSender,
        @Argument("profile") profile: String,
    ) {
        val player = sender.asPlayer() ?: return
        player.createProfile(profile)
        openGui(player, profile)
    }

    @Command("setting list")
    @CommandDescription("検索プロファイルの一覧を表示します")
    suspend fun list(sender: CommandSender) {
        val player = sender.asPlayer() ?: return
        val config = player.loadPlayerFindOption()
        sender.sendRichMessage("<green>検索プロファイル一覧:")
        config.findOptions.keys.sorted().forEach { name ->
            val marker = if (name == config.setting) " <gray>(使用中)" else ""
            sender.sendRichMessage("<gray>- <white>$name$marker")
        }
    }

    @Command("setting use <profile>")
    @CommandDescription("使用する検索プロファイルを切り替えます")
    suspend fun use(
        sender: CommandSender,
        @Argument("profile") profile: String,
    ) {
        val player = sender.asPlayer() ?: return
        if (player.setActiveProfile(profile)) {
            sender.sendRichMessage("<green>プロファイル '$profile' を使用します。")
        } else {
            sender.sendRichMessage("<red>プロファイル '$profile' が見つかりません。")
        }
    }

    @Command("setting delete <profile>")
    @CommandDescription("検索プロファイルを削除します")
    suspend fun delete(
        sender: CommandSender,
        @Argument("profile") profile: String,
    ) {
        val player = sender.asPlayer() ?: return
        if (player.deleteProfile(profile)) {
            sender.sendRichMessage("<green>プロファイル '$profile' を削除しました。")
        } else {
            sender.sendRichMessage("<red>プロファイル '$profile' は削除できません（存在しない、または default）。")
        }
    }

    private suspend fun openGui(
        player: Player,
        profileName: String,
    ) {
        val option = player.getPlayerFindOption(profileName) ?: FindOption()
        withContext(Dispatchers.minecraft) {
            FindOptionGui(player, profileName, option).open()
        }
    }

    private fun CommandSender.asPlayer(): Player? {
        if (this is Player) return this
        sendRichMessage("<red>このコマンドはプレイヤーのみ実行できます。")
        return null
    }
}
