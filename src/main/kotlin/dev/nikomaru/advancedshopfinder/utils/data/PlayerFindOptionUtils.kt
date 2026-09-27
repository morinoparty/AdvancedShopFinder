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
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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

    /** 1 プレイヤーが持てるプロファイル数の上限（default を含む）。 */
    const val MAX_PROFILES: Int = 3

    /** [createProfile] の結果。 */
    enum class CreateProfileResult { CREATED, ALREADY_EXISTS, LIMIT_REACHED }

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
     * プレイヤーの [PlayerFindOption] 全体を読み込む。ファイルが無い・空の場合はデフォルトを返す（ファイルも作る）。
     *
     * タブ補完などから並行して呼ばれるため、書き込みは [writeAtomically] で行い、読み込み側が
     * 書きかけ・空のファイルを見ないようにしている。
     */
    suspend fun Player.loadPlayerFindOption(): PlayerFindOption =
        withContext(Dispatchers.IO) {
            val file = configFile()
            val text = if (file.exists()) file.readText() else ""
            if (text.isBlank()) {
                // 以前の版では作成途中の空ファイルが残ることがあったので、空もデフォルトとして扱う
                PlayerFindOption().also { file.writeAtomically(json.encodeToString(it)) }
            } else {
                json.decodeFromString<PlayerFindOption>(text)
            }
        }

    /**
     * プレイヤーの [PlayerFindOption] 全体を書き込む。
     */
    suspend fun Player.savePlayerFindOption(option: PlayerFindOption): Unit =
        withContext(Dispatchers.IO) {
            configFile().writeAtomically(json.encodeToString(option))
        }

    /** 同じディレクトリの一時ファイルに書いてから置き換え、読み込み側に書きかけの内容を見せない。 */
    private fun File.writeAtomically(text: String) {
        parentFile.mkdirs()
        val temp = File.createTempFile("$name.", ".tmp", parentFile)
        try {
            temp.writeText(text)
            Files.move(temp.toPath(), toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            temp.delete()
        }
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
     * 新しいプロファイルを作成する。既に存在する場合や上限 [MAX_PROFILES] に達している場合は何もしない。
     */
    suspend fun Player.createProfile(
        profileName: String,
        base: FindOption = FindOption(),
    ): CreateProfileResult =
        lock().withLock {
            val config = loadPlayerFindOption()
            when {
                config.findOptions.containsKey(profileName) -> CreateProfileResult.ALREADY_EXISTS
                config.findOptions.size >= MAX_PROFILES -> CreateProfileResult.LIMIT_REACHED
                else -> {
                    val newOptions = HashMap(config.findOptions).apply { put(profileName, base) }
                    savePlayerFindOption(config.copy(findOptions = newOptions))
                    CreateProfileResult.CREATED
                }
            }
        }

    /**
     * プロファイルを新規作成または上書き保存する。
     * 新規作成になる場合に上限 [MAX_PROFILES] に達していれば保存せず false を返す。
     */
    suspend fun Player.upsertProfile(
        profileName: String,
        option: FindOption,
    ): Boolean =
        lock().withLock {
            val config = loadPlayerFindOption()
            val isNew = !config.findOptions.containsKey(profileName)
            if (isNew && config.findOptions.size >= MAX_PROFILES) return@withLock false
            val newOptions = HashMap(config.findOptions).apply { put(profileName, option) }
            savePlayerFindOption(config.copy(findOptions = newOptions))
            true
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
