package dev.nikomaru.advancedshopfinder.utils.shop

import com.ghostchu.quickshop.api.shop.Shop
import dev.nikomaru.advancedshopfinder.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.bukkit.Chunk
import org.bukkit.plugin.Plugin
import java.util.UUID
import kotlin.math.floor

/**
 * ショップの在庫（販売ショップ）または空き容量（買取ショップ）。
 *
 * QuickShop-Hikari の `getAllShops()` はチャンクが読み込まれていないショップも返すが、
 * そのショップの `remainingStock` / `remainingSpace` はインベントリを参照できず常に 0 になる。
 * そのため [resolve] は、読み込まれていないショップのチャンクを読み込んでから数える。
 */
sealed interface ShopStock {
    /** アドミンショップなど、在庫・容量の制限が無い。 */
    data object Unlimited : ShopStock

    /**
     * 在庫または空き容量の数。
     *
     * 買取ショップでは、チェストの空き容量とオーナーの所持金で買い取れる数の少ない方になる。
     *
     * @property amount ショップの取引単位（shopStackingAmount）での数
     * @property limitedByMoney 買取ショップで、空き容量よりオーナーの所持金で買い取れる数の方が少ないか
     */
    data class Counted(
        val amount: Int,
        val limitedByMoney: Boolean = false,
    ) : ShopStock

    /** ワールドが読み込まれていない・チャンクを読み込めなかったなどで分からない。 */
    data object Unknown : ShopStock

    /**
     * 検索結果に表示するか。
     *
     * 在庫切れ・空き容量が無いショップは [showNoStockShop] が true のときだけ、
     * オーナーの所持金が足りず 1 回分も買い取れない買取ショップは [showOwnerNotHasEnoughMoneyShop] が true のときだけ表示する。
     */
    fun isVisible(
        showNoStockShop: Boolean,
        showOwnerNotHasEnoughMoneyShop: Boolean = false,
    ): Boolean =
        when (this) {
            Unlimited, Unknown -> true
            is Counted ->
                when {
                    amount > 0 -> true
                    limitedByMoney -> showOwnerNotHasEnoughMoneyShop
                    else -> showNoStockShop
                }
        }

    companion object {
        /**
         * ショップごとの在庫・空き容量を求める。
         *
         * チャンクが読み込まれていないショップは、Paper の非同期チャンク読み込み（メインスレッドを止めない）で
         * チャンクを読み込み、数え終わるまでプラグインのチャンクチケットで読み込んだままにしてから、
         * QuickShop の `remainingStock` / `remainingSpace` で数える。
         * 買取ショップは、Vault で取得したオーナーの所持金で買い取れる数でも制限する。
         *
         * @param buying 買取ショップとして空き容量を求めるか（false なら在庫）
         */
        suspend fun resolve(
            plugin: Plugin,
            shops: List<Shop>,
            buying: Boolean,
        ): Map<Shop, ShopStock> {
            val unloaded =
                withContext(Dispatchers.minecraft) {
                    shops.filter { shop ->
                        val location = shop.location
                        !shop.isUnlimited && location.isWorldLoaded && !location.isChunkLoaded
                    }
                }
            val chunks = loadChunks(unloaded)
            // オフラインのオーナーの所持金はプラグインによってはディスクから読むので、メインスレッドの外で先に取る
            val balances = if (buying) OwnerBalance.fetch(shops.filterNot { it.isUnlimited }) else emptyMap()
            return withContext(Dispatchers.minecraft) {
                chunks.forEach { it.addPluginChunkTicket(plugin) }
                try {
                    shops.associateWith { count(it, buying, balances) }
                } finally {
                    chunks.forEach { it.removePluginChunkTicket(plugin) }
                }
            }
        }

        /** ショップのあるチャンクを重複なく非同期で読み込む。読み込めなかったチャンクは含めない。 */
        private suspend fun loadChunks(shops: List<Shop>): List<Chunk> =
            coroutineScope {
                shops
                    .map { it.location }
                    .distinctBy { Triple(it.world.uid, it.blockX shr 4, it.blockZ shr 4) }
                    .map { location ->
                        async {
                            runCatching {
                                location.world.getChunkAtAsync(location.blockX shr 4, location.blockZ shr 4).await()
                            }.getOrNull()
                        }
                    }.awaitAll()
                    .filterNotNull()
            }

        /**
         * QuickShop の在庫・空き容量の計算はインベントリに触るため、どこから呼ばれても必ずメインスレッドで行う。
         * （Dispatchers.minecraft はメインスレッドから呼ばれた場合はその場で実行するので、切り替えの待ちは無い）
         */
        private suspend fun count(
            shop: Shop,
            buying: Boolean,
            balances: Map<UUID, Double>,
        ): ShopStock =
            withContext(Dispatchers.minecraft) {
                val location = shop.location
                when {
                    shop.isUnlimited -> Unlimited
                    !location.isWorldLoaded || !location.isChunkLoaded -> Unknown
                    !buying -> Counted(shop.remainingStock)
                    else -> buyable(shop.remainingSpace, shop.price, shop.owner.uniqueId?.let { balances[it] })
                }
            }

        /**
         * 買取ショップが買い取れる数。空き容量と、オーナーの所持金で払える回数の少ない方。
         *
         * 所持金が分からない（Vault が無い・オーナーが不明）場合や、価格が 0 以下の場合は空き容量だけで決める。
         *
         * @param space 空き容量（取引単位での数）
         * @param price 1 取引単位あたりの買取価格
         * @param balance オーナーの所持金
         */
        fun buyable(
            space: Int,
            price: Double,
            balance: Double?,
        ): Counted {
            if (balance == null || price <= 0.0) return Counted(space)
            val affordable = floor(balance.coerceAtLeast(0.0) / price).coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()
            return if (affordable < space) Counted(affordable, limitedByMoney = true) else Counted(space)
        }
    }
}

/** 件数上限を適用する。負の値（GUI の「無制限」は -1）は上限なし、0 なら 1 件も表示しない。 */
fun <T> List<T>.limitedTo(limit: Int): List<T> = if (limit < 0) this else take(limit)
