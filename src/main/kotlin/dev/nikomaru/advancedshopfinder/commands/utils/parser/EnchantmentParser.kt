package dev.nikomaru.advancedshopfinder.commands.utils.parser

import dev.nikomaru.advancedshopfinder.utils.translate.TranslateManager
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
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

class EnchantmentParser<CommandSender> :
    ArgumentParser<CommandSender, Enchantment>,
    BlockingSuggestionProvider.Strings<CommandSender>,
    KoinComponent {
    val manager: TranslateManager by inject()

    companion object {
        fun enchantmentParser(): ParserDescriptor<CommandSender, Enchantment> =
            ParserDescriptor.of(EnchantmentParser(), Enchantment::class.java)
    }

    override fun parse(
        commandContext: CommandContext<CommandSender & Any>,
        commandInput: CommandInput,
    ): ArgumentParseResult<Enchantment> {
        val locale =
            if (commandContext.sender() is Player) {
                (commandContext.sender() as Player).locale()
            } else {
                Locale.getDefault()
            }
        val input = commandInput.readStringSkipWhitespace()
        val enchantmentKey =
            getKeys(manager.getTranslateMap(locale), input)?.key?.replace("enchantment.minecraft.", "")
                ?: input
        val enchantment =
            RegistryAccess.registryAccess().getRegistry<Enchantment>(RegistryKey.ENCHANTMENT).get(
                NamespacedKey.minecraft(enchantmentKey),
            )
        return if (enchantment != null) {
            ArgumentParseResult.success(enchantment)
        } else {
            ArgumentParseResult.failure(Exception("No enchantment found"))
        }
    }

    val enchantments =
        RegistryAccess.registryAccess().getRegistry<Enchantment>(RegistryKey.ENCHANTMENT).map { it.key.key }

    // RegistryAccess.registryAccess().getRegistry<Enchantment>(RegistryKey.ENCHANTMENT).get()
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
            enchantments +
                enchantments.mapNotNull {
                    manager.getTranslateMap(locale)[NamespacedKey.minecraft("enchantment.minecraft.$it")]
                }
        ).toMutableList()
    }

    private fun <K, V> getKeys(
        map: Map<K, V>,
        value: V,
    ): K? {
        for (key in map.keys) {
            if (value == map[key]) {
                return key
            }
        }
        return null
    }
}
