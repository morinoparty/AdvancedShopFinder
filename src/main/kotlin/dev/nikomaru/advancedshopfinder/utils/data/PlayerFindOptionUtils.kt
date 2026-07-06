package dev.nikomaru.advancedshopfinder.utils.data

import dev.nikomaru.advancedshopfinder.AdvancedShopFinder
import dev.nikomaru.advancedshopfinder.files.PlayerFindOption
import dev.nikomaru.advancedshopfinder.files.server.Config.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * プレイヤーごとの検索オプション（プロファイル）を永続化・操作するユーティリティ。
 *
 * データは `plugins/AdvancedShopFinder/playerdata/<uuid>/config.json` に保存される。
 */
object PlayerFindOptionUtils : KoinComponent {
    private val plugin: AdvancedShopFinder by inject()

    /** default プロファイルは常に存在し、削除できない。 */
    const val DEFAULT_PROFILE: String = "default"

    /**
     * プレイヤーごとの設定ファイルへの load-modify-save を直列化するためのロック。
     * これが無いと、GUI の「使用中に設定」と「保存」を素早く連続クリックした際などに、
     * 並行する 2 つのコルーチンが同じファイルを読み込み・上書きし、片方の変更が失われうる。
     */
    private val locks = ConcurrentHashMap<UUID, Mutex>()

    private fun Player.lock(): Mutex = locks.getOrPut(uniqueId) { Mutex() }

    private fun Player.configFile(): File =
        plugin.dataFolder
            .resolve("playerdata")
            .resolve("$uniqueId")
            .resolve("config.json")

    /**
     * プレイヤーの [PlayerFindOption] 全体を読み込む。ファイルが無ければデフォルトを生成する。
     */
    suspend fun Player.loadPlayerFindOption(): PlayerFindOption =
        withContext(Dispatchers.IO) {
            val file = configFile()
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
                file.writeText(json.encodeToString(PlayerFindOption()))
            }
            json.decodeFromString<PlayerFindOption>(file.readText())
        }

    /**
     * プレイヤーの [PlayerFindOption] 全体を書き込む。
     */
    suspend fun Player.savePlayerFindOption(option: PlayerFindOption): Unit =
        withContext(Dispatchers.IO) {
            val file = configFile()
            file.parentFile.mkdirs()
            if (!file.exists()) file.createNewFile()
            file.writeText(json.encodeToString(option))
        }

    /**
     * 現在使用中（`setting`）のプロファイルの [FindOption] を返す。
     * 使用中プロファイルが存在しない場合は null。
     */
    suspend fun Player.getPlayerFindOption(): FindOption? {
        val config = loadPlayerFindOption()
        return config.findOptions[config.setting]
    }

    /**
     * 名前を指定してプロファイルの [FindOption] を返す。存在しなければ null。
     */
    suspend fun Player.getPlayerFindOption(profileName: String): FindOption? = loadPlayerFindOption().findOptions[profileName]

    /** プロファイル名の一覧。 */
    suspend fun Player.listProfiles(): Set<String> = loadPlayerFindOption().findOptions.keys

    /** 現在使用中のプロファイル名。 */
    suspend fun Player.getActiveProfileName(): String = loadPlayerFindOption().setting

    /**
     * 使用するプロファイルを切り替える。存在しないプロファイル名なら false。
     */
    suspend fun Player.setActiveProfile(profileName: String): Boolean =
        lock().withLock {
            val config = loadPlayerFindOption()
            if (!config.findOptions.containsKey(profileName)) return@withLock false
            savePlayerFindOption(config.copy(setting = profileName))
            true
        }

    /**
     * 新しいプロファイルを作成する。既に存在する場合は何もせず false を返す。
     */
    suspend fun Player.createProfile(
        profileName: String,
        base: FindOption = FindOption(),
    ): Boolean =
        lock().withLock {
            val config = loadPlayerFindOption()
            if (config.findOptions.containsKey(profileName)) return@withLock false
            val newOptions = HashMap(config.findOptions).apply { put(profileName, base) }
            savePlayerFindOption(config.copy(findOptions = newOptions))
            true
        }

    /**
     * プロファイルを新規作成または上書き保存する。
     */
    suspend fun Player.upsertProfile(
        profileName: String,
        option: FindOption,
    ): Unit =
        lock().withLock {
            val config = loadPlayerFindOption()
            val newOptions = HashMap(config.findOptions).apply { put(profileName, option) }
            savePlayerFindOption(config.copy(findOptions = newOptions))
        }

    /**
     * プロファイルを削除する。default は削除不可。使用中プロファイルを削除した場合は
     * 使用中プロファイルを default に戻す。存在しない・削除不可の場合は false。
     */
    suspend fun Player.deleteProfile(profileName: String): Boolean {
        if (profileName == DEFAULT_PROFILE) return false
        return lock().withLock {
            val config = loadPlayerFindOption()
            if (!config.findOptions.containsKey(profileName)) return@withLock false
            val newOptions = HashMap(config.findOptions).apply { remove(profileName) }
            val newSetting = if (config.setting == profileName) DEFAULT_PROFILE else config.setting
            savePlayerFindOption(config.copy(setting = newSetting, findOptions = newOptions))
            true
        }
    }
}
