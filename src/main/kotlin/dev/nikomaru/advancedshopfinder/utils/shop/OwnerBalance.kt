package dev.nikomaru.advancedshopfinder.utils.shop

import com.ghostchu.quickshop.api.shop.Shop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.UUID

/**
 * Vault からショップのオーナーの所持金を取得する。
 *
 * Vault または経済プラグインが無い場合は何も取得しない（買取ショップは空き容量だけで数える）。
 */
object OwnerBalance : KoinComponent {
    /** オーナーごとの所持金。取得できなかったオーナーは含めない。 */
    suspend fun fetch(shops: List<Shop>): Map<UUID, Double> {
        val owners = shops.mapNotNull { it.owner.uniqueId }.toSet()
        if (owners.isEmpty()) return emptyMap()
        val economy = economyOrNull() ?: return emptyMap()
        // オフラインのプレイヤーの所持金はディスクから読む経済プラグインもあるので、メインスレッドの外で取る
        return withContext(Dispatchers.IO) {
            owners
                .mapNotNull { uuid ->
                    runCatching { uuid to economy.getBalance(Bukkit.getOfflinePlayer(uuid)) }.getOrNull()
                }.toMap()
        }
    }

    private fun economyOrNull(): Economy? {
        // Vault の無いサーバーでは Economy クラスを読み込めないので、先にプラグインの有無を確かめる
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) return null
        return runCatching { get<Economy>() }.getOrNull()
    }
}
