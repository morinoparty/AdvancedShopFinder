package dev.nikomaru.advancedshopfinder.commands

import com.ghostchu.quickshop.api.QuickShopAPI
import com.ghostchu.quickshop.api.shop.Shop
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.advancedshopfinder.AdvancedShopFinder
import dev.nikomaru.advancedshopfinder.commands.utils.Hidden
import dev.nikomaru.advancedshopfinder.utils.coroutines.minecraft
import dev.nikomaru.advancedshopfinder.utils.display.LuminescenceDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 検索結果の「[ここまで行く]」から実行され、ショップの位置まで発光するブロック（block_display）で案内する。
 *
 * 検索結果のクリックから使う前提のため、[Hidden] で補完には出さない。
 */
@Command("advancedshopfinder|asf|shopfinder|sf")
object TraceCommand : KoinComponent {
    private const val GUIDE_SECONDS: Int = 30
    private const val REFRESH_MILLIS: Long = 1000
    private const val REFRESH_COUNT: Int = (GUIDE_SECONDS * 1000 / REFRESH_MILLIS).toInt()

    /** この距離まで近づいたら到着として案内を終える。 */
    private const val ARRIVAL_DISTANCE: Double = 3.0

    /** ショップがこれより遠いときは、ショップの方向を示す目印も出す。 */
    private const val POINTER_MIN_DISTANCE: Double = 24.0

    /** 方向を示す目印を出す、プレイヤーの目からの距離。近すぎると視界を塞ぐので離して置く。 */
    private const val POINTER_DISTANCE: Double = 16.0

    private val quickShop: QuickShopAPI by inject()
    private val plugin: AdvancedShopFinder by inject()

    /** プレイヤーごとの案内。新しい案内を始めたら前の案内は止める。 */
    private val guides = ConcurrentHashMap<UUID, Job>()

    @Hidden
    @Command("trace <shopId>")
    @CommandDescription("ショップまで案内します")
    suspend fun trace(
        sender: CommandSender,
        @Argument("shopId") shopId: Long,
    ) {
        if (sender !is Player) {
            sender.sendRichMessage("<red>このコマンドはプレイヤーのみ実行できます。")
            return
        }
        val shop = withContext(Dispatchers.minecraft) { findShop(shopId) }
        if (shop == null) {
            sender.sendRichMessage("<red>ショップが見つかりません。削除された可能性があります。")
            return
        }
        val target = shop.location.toBlockLocation()
        if (target.world != sender.world) {
            sender.sendRichMessage("<red>ショップは別のワールド（${target.world.name}）にあります。")
            return
        }

        guides.remove(sender.uniqueId)?.cancel()
        sender.sendRichMessage(
            "<green>ショップ（x:${target.blockX} y:${target.blockY} z:${target.blockZ}）まで ${GUIDE_SECONDS} 秒間案内します。",
        )
        val job = plugin.launch { guide(sender, target) }
        guides[sender.uniqueId] = job
        job.invokeOnCompletion { guides.remove(sender.uniqueId, job) }
    }

    private fun findShop(shopId: Long): Shop? =
        quickShop.shopManager.getShop(shopId)
            ?: quickShop.shopManager.allShops.firstOrNull { it.shopId == shopId }

    /** [GUIDE_SECONDS] 秒間、ショップの位置（と遠い場合は進む方向）に発光するブロックを表示し続ける。 */
    private suspend fun guide(
        player: Player,
        target: Location,
    ) {
        val center = target.clone().add(0.5, 0.5, 0.5)
        var shown: LuminescenceDisplay? = null
        try {
            repeat(REFRESH_COUNT) {
                val eye = withContext(Dispatchers.minecraft) { if (player.isOnline) player.eyeLocation else null } ?: return
                // 別のワールド（ディメンション）に移ったら、同じ座標で今のワールドに表示しないよう案内を終える
                if (eye.world != target.world) {
                    player.sendRichMessage("<yellow>別のワールドに移動したため、案内を終了しました。")
                    return
                }
                val distance = eye.distance(center)
                if (distance <= ARRIVAL_DISTANCE) {
                    player.sendRichMessage("<green>ショップに到着しました。")
                    return
                }

                val next = LuminescenceDisplay()
                next.addTarget(player)
                // ショップのブロックを読むので、メインスレッドで追加する
                withContext(Dispatchers.minecraft) { next.addShop(target) }
                if (distance > POINTER_MIN_DISTANCE) {
                    val direction = center.toVector().subtract(eye.toVector()).normalize()
                    next.addPointer(eye.clone().add(direction.multiply(POINTER_DISTANCE)))
                }
                // 新しい位置を表示してから前回分を消し、表示が途切れないようにする
                next.display()
                shown?.stop()
                shown = next
                delay(REFRESH_MILLIS)
            }
            player.sendRichMessage("<gray>案内を終了しました。")
        } finally {
            // 別の案内に切り替わってキャンセルされた場合も、表示中のブロックは必ず消す
            withContext(NonCancellable) { shown?.stop() }
        }
    }
}
