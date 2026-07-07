package dev.nikomaru.advancedshopfinder.commands.utils.parser

import dev.nikomaru.advancedshopfinder.utils.translate.TranslateManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale
import kotlin.collections.mapNotNull

class MaterialArrayParser<CommandSender> :
    ArgumentParser<CommandSender, Array<Material>>,
    BlockingSuggestionProvider.Strings<CommandSender>,
    KoinComponent {
    private val translateManager: TranslateManager by inject()

    companion object {
        fun materialArrayParser(): ParserDescriptor<CommandSender, Array<Material>> =
            ParserDescriptor.of(MaterialArrayParser(), Array<Material>::class.java)
    }

    override fun parse(
        commandContext: CommandContext<CommandSender & Any>,
        commandInput: CommandInput,
    ): ArgumentParseResult<Array<Material>> {
        val input = commandInput.readString()
        val locale =
            if (commandContext.sender() is Player) {
                (commandContext.sender() as Player).locale()
            } else {
                Locale.getDefault()
            }
        val maps = translateManager.getTranslateMap(locale)
        val item =
            getKeys(maps, input)?.map { keys -> Material.entries.find { NamespacedKey.minecraft(it.translationKey()) == keys } }
                ?: listOf(Material.matchMaterial(input))
        val res = item.filter { it != null }.map { it!! }.toTypedArray()
        return ArgumentParseResult.success(res)
    }

    override fun stringSuggestions(
        commandContext: CommandContext<CommandSender>,
        input: CommandInput,
    ): MutableIterable<String> {
        val locale =
            if (commandContext.sender() is Player) {
                (commandContext.sender() as Player).locale()
            } else {
                Locale.getDefault()
            }
        return (
            Material.entries.mapNotNull {
                translateManager.getTranslateMap(locale)[NamespacedKey.minecraft(it.translationKey())]
            } + Material.entries.map { it.name.lowercase() }
        ).toMutableList()
    }

    private fun <K, V> getKeys(
        map: Map<K, V>,
        value: V,
    ): List<K>? {
        val list = arrayListOf<K>()
        for (key in map.keys) {
            if (value == map[key]) {
                list.add(key)
            }
        }
        return if (list.isEmpty()) {
            null
        } else {
            list
        }
    }
}
