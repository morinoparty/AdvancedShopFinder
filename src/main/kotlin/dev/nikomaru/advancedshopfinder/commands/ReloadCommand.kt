package dev.nikomaru.advancedshopfinder.commands

import dev.nikomaru.advancedshopfinder.files.server.Config
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Command("advancedshopfinder|asf|shopfinder|sf")
@Permission("advancedshopfinder.admin")
object ReloadCommand {
    @Command("reload")
    fun reload(sender: CommandSender) {
        Config.loadConfig()
        sender.sendRichMessage("<color:green>コンフィグをリロードしました")
    }
}
