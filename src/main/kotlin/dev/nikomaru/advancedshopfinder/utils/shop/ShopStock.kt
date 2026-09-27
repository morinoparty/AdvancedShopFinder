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
     * @property amount ショップの取引単位（shopStackingAmount）での数
     */
    data class Counted(
        val amount: Int,
    ) : ShopStock

    /** ワールドが読み込まれていない・チャンクを読み込めなかったなどで分からない。 */
    data object Unknown : ShopStock

    /** 検索結果に表示するか。在庫切れのショップは [showNoStockShop] が true のときだけ表示する。 */
    fun isVisible(showNoStockShop: Boolean): Boolean =
        when (this) {
            Unlimited, Unknown -> true
            is Counted -> amount > 0 || showNoStockShop
        }

    companion object {
        /**
         * ショップごとの在庫・空き容量を求める。
         *
         * チャンクが読み込まれていないショップは、Paper の非同期チャンク読み込み（メインスレッドを止めない）で
         * チャンクを読み込み、数え終わるまでプラグインのチャンクチケットで読み込んだままにしてから、
         * QuickShop の `remainingStock` / `remainingSpace` で数える。
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
            return withContext(Dispatchers.minecraft) {
                chunks.forEach { it.addPluginChunkTicket(plugin) }
                try {
                    shops.associateWith { count(it, buying) }
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
        ): ShopStock =
            withContext(Dispatchers.minecraft) {
                val location = shop.location
                when {
                    shop.isUnlimited -> Unlimited
                    !location.isWorldLoaded || !location.isChunkLoaded -> Unknown
                    else -> Counted(if (buying) shop.remainingSpace else shop.remainingStock)
                }
            }
    }
}

/** 件数上限を適用する。0 以下は上限なし（GUI の「無制限」は -1）。 */
fun <T> List<T>.limitedTo(limit: Int): List<T> = if (limit > 0) take(limit) else this
