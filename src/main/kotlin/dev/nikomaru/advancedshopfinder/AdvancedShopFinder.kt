package dev.nikomaru.advancedshopfinder

import com.comphenix.protocol.ProtocolLibrary
import com.ghostchu.quickshop.api.QuickShopAPI
import dev.nikomaru.advancedshopfinder.commands.EnchantFindCommand
import dev.nikomaru.advancedshopfinder.commands.FuzzySearchCommand
import dev.nikomaru.advancedshopfinder.commands.HelpCommand
import dev.nikomaru.advancedshopfinder.commands.ReloadCommand
import dev.nikomaru.advancedshopfinder.commands.SettingCommand
import dev.nikomaru.advancedshopfinder.commands.ShopSearchCommand
import dev.nikomaru.advancedshopfinder.commands.utils.parser.EnchantmentParser
import dev.nikomaru.advancedshopfinder.commands.utils.parser.MaterialArrayParser
import dev.nikomaru.advancedshopfinder.files.server.Config
import dev.nikomaru.advancedshopfinder.utils.translate.TranslateManager
import dev.nikomaru.advancedshopfinder.utils.translate.TranslateManagerImpl
import kotlinx.serialization.ExperimentalSerializationApi
import net.milkbowl.vault.economy.Economy
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.bukkit.CloudBukkitCapabilities
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.setting.ManagerSetting
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

open class AdvancedShopFinder : JavaPlugin() {
    @OptIn(ExperimentalSerializationApi::class)
    override fun onEnable() { // Plugin startup logic
        setupKoin()
        Config.loadConfig()
        setCommand()
    }

    private fun setupKoin() {
        GlobalContext.getOrNull() ?: GlobalContext.startKoin {
            modules(
                module {
                    single { this@AdvancedShopFinder }
                    single { QuickShopAPI.getInstance() }
                    single { ProtocolLibrary.getProtocolManager() }
                    single<TranslateManager> { TranslateManagerImpl() }
                    single<Economy> {
                        server.servicesManager.getRegistration(Economy::class.java)?.provider
                            ?: throw IllegalStateException("Vault Economy plugin not found")
                    }
                },
            )
        }
    }

    override fun onDisable() { // Plugin shutdown logic
    }

    private fun setCommand() {
        val commandManager =
            LegacyPaperCommandManager.createNative(
                this,
                ExecutionCoordinator.simpleCoordinator(),
            )

        if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            commandManager.registerAsynchronousCompletions()
        }

        commandManager.settings().set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true)

        commandManager.parserRegistry().registerParser(MaterialArrayParser.materialArrayParser())
        commandManager.parserRegistry().registerParser(EnchantmentParser.enchantmentParser())

        val annotationParser = AnnotationParser(commandManager, CommandSender::class.java)
        annotationParser.installCoroutineSupport()

        with(annotationParser) {
            parse(
                EnchantFindCommand,
                FuzzySearchCommand,
                ReloadCommand,
                ShopSearchCommand,
                HelpCommand,
                SettingCommand,
            )
        }
    }
}
