package dev.nikomaru.advancedshopfinder.commands.utils

import dev.nikomaru.advancedshopfinder.utils.data.FindOption
import dev.nikomaru.advancedshopfinder.utils.data.PlayerFindOptionUtils.getPlayerFindOption
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 検索コマンドの `-p` / `--profile` フラグを解決し、使用する [FindOption] を返す。
 *
 * - コンソール等プレイヤー以外の場合はデフォルトの [FindOption]。
 * - フラグ未指定の場合は使用中プロファイル（無ければデフォルト）。
 * - フラグ指定かつ該当プロファイルが存在しない場合は、エラーメッセージを送信して null を返す。
 */
suspend fun resolveFindOption(
    sender: CommandSender,
    profile: String?,
): FindOption? {
    if (sender !is Player) return FindOption()
    if (profile == null) return sender.getPlayerFindOption() ?: FindOption()
    return sender.getPlayerFindOption(profile) ?: run {
        sender.sendRichMessage(
            "<red>プロファイル '$profile' が見つかりません。<gray>/sf setting list で一覧を確認できます。",
        )
        null
    }
}
