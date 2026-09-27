package dev.nikomaru.advancedshopfinder.utils.shop

import com.ghostchu.quickshop.api.shop.Shop
import com.ghostchu.quickshop.api.shop.cache.ShopInventoryCountCache
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * ショップの在庫（販売ショップ）または空き容量（買取ショップ）。
 *
 * QuickShop-Hikari の `getAllShops()` はチャンクが読み込まれていないショップも返すが、
 * そのショップの `remainingStock` / `remainingSpace` はインベントリを参照できず常に 0 になる。
 * そのため読み込まれていないショップは QuickShop が DB に保存している在庫キャッシュを使い、
 * キャッシュも無ければ [Unknown] として扱う（在庫切れとして除外しない）。
 */
sealed interface ShopStock {
    /** アドミンショップなど、在庫・容量の制限が無い。 */
    data object Unlimited : ShopStock

    /**
     * 在庫または空き容量の数。
     *
     * @property amount ショップの取引単位（shopStackingAmount）での数
     * @property cached チャンクが読み込まれておらず、QuickShop のキャッシュ（前回確認時の値）から取得したか
     */
    data class Counted(
        val amount: Int,
        val cached: Boolean,
    ) : ShopStock

    /** チャンクが読み込まれておらず、キャッシュも無いため分からない。 */
    data object Unknown : ShopStock

    /** 検索結果に表示するか。在庫切れのショップは [showNoStockShop] が true のときだけ表示する。 */
    fun isVisible(showNoStockShop: Boolean): Boolean =
        when (this) {
            Unlimited, Unknown -> true
            is Counted -> amount > 0 || showNoStockShop
        }

    companion object {
        /**
         * ショップの在庫・空き容量を求める。メインスレッドで呼び出すこと。
         *
         * @param buying 買取ショップとして空き容量を求めるか（false なら在庫）
         */
        fun of(
            shop: Shop,
            buying: Boolean,
        ): ShopStock {
            if (shop.isUnlimited) return Unlimited
            val location = shop.location
            if (location.isWorldLoaded && location.isChunkLoaded) {
                return Counted(if (buying) shop.remainingSpace else shop.remainingStock, cached = false)
            }
            val cache = inventoryCountCache(shop) ?: return Unknown
            if (!cache.initialized()) return Unknown
            // 在庫と空き容量は最後に計算した方しか保存されず、もう一方は負の値になる
            val amount = if (buying) cache.space else cache.stock
            return if (amount >= 0) Counted(amount, cached = true) else Unknown
        }

        // ContainerShop#getInventoryCountCache は QuickShop-Hikari 6.3 以降の実装クラスにしか無く、公開 API には無い。
        // 6.2 でも動くよう、リフレクションで探して無ければ null を返す
        private val cacheAccessors = ConcurrentHashMap<Class<*>, Method?>()

        private fun inventoryCountCache(shop: Shop): ShopInventoryCountCache? {
            val accessor =
                cacheAccessors.computeIfAbsent(shop.javaClass) { type ->
                    runCatching { type.getMethod("getInventoryCountCache") }.getOrNull()
                } ?: return null
            return runCatching { accessor.invoke(shop) as? ShopInventoryCountCache }.getOrNull()
        }
    }
}

/** 件数上限を適用する。0 以下は上限なし（GUI の「無制限」は -1）。 */
fun <T> List<T>.limitedTo(limit: Int): List<T> = if (limit > 0) take(limit) else this
