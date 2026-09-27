package dev.nikomaru.advancedshopfinder.commands.utils

import dev.nikomaru.advancedshopfinder.utils.data.PlayerFindOptionUtils.listProfiles
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext

/**
 * プロファイル名の補完。`setting <profile>` などの引数と、検索コマンドの `-p` / `--profile` フラグで使う。
 *
 * 補完名を参照するコマンドより先に AnnotationParser へ渡すこと。
 */
object ProfileSuggestions {
    const val PROFILES: String = "profiles"

    @Suggestions(PROFILES)
    suspend fun profiles(
        context: CommandContext<CommandSender>,
        input: String,
    ): List<String> {
        val player = context.sender() as? Player ?: return emptyList()
        return player.listProfiles().sorted()
    }
}
