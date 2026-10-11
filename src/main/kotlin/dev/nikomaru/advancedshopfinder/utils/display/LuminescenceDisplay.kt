package dev.nikomaru.advancedshopfinder.utils.display

import com.github.retrooper.packetevents.manager.player.PlayerManager
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import dev.nikomaru.advancedshopfinder.utils.coroutines.async
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Optional
import java.util.UUID
import kotlin.random.Random

/**
 * ショップの位置などに、発光する block_display をパケットだけで表示して目立たせる。
 *
 * 実体のエンティティは作らず、対象のプレイヤーにだけ見える。
 * シュルカーと違い Display エンティティには当たり判定が無いので、プレイヤーの移動やショップのクリックを妨げない。
 */
class LuminescenceDisplay : KoinComponent {
    private val playerManager: PlayerManager by inject()
    private val ids = arrayListOf<Int>()
    private val markers = arrayListOf<Marker>()
    private val target = arrayListOf<Player>()

    private data class Marker(
        val location: Location,
        val blockState: Int,
        val scale: Float,
    )

    /**
     * ショップのブロックに重ねて、同じ見た目のブロックを少し大きく表示する。
     *
     * ブロックを読むので、メインスレッドから呼ぶこと。チャンクが読み込まれていなければチェストとして表示する。
     */
    fun addShop(location: Location) {
        val block = location.toBlockLocation()
        val blockData = if (block.isChunkLoaded) block.block.blockData else Material.CHEST.createBlockData()
        // 本物のブロックと面が重なってちらつかないよう、少しだけ大きくする
        markers.add(Marker(block, blockState(blockData), SHOP_SCALE))
    }

    /** 進む方向を示す小さなブロックを、[location] を中心に表示する。 */
    fun addPointer(location: Location) {
        markers.add(Marker(location.clone(), blockState(Material.SEA_LANTERN.createBlockData()), POINTER_SCALE))
    }

    fun addTarget(player: Player) {
        target.add(player)
    }

    suspend fun display() {
        withContext(Dispatchers.async) {
            markers.forEach { marker ->
                target.forEach {
                    val entityId = Random.nextInt(Int.MAX_VALUE)
                    ids.add(entityId)
                    val spawnPacket =
                        WrapperPlayServerSpawnEntity(
                            entityId,
                            Optional.of(UUID.randomUUID()),
                            EntityTypes.BLOCK_DISPLAY,
                            Vector3d(marker.location.x, marker.location.y, marker.location.z),
                            0f,
                            0f,
                            0f,
                            0,
                            Optional.empty(),
                        )
                    val metadataPacket = WrapperPlayServerEntityMetadata(entityId, metadata(marker))
                    playerManager.sendPacket(it, spawnPacket)
                    playerManager.sendPacket(it, metadataPacket)
                }
            }
        }
    }

    suspend fun stop() {
        withContext(Dispatchers.async) {
            val destroyPacket = WrapperPlayServerDestroyEntities(*ids.toIntArray())
            target.forEach {
                playerManager.sendPacket(it, destroyPacket)
            }
            ids.clear()
        }
    }

    /**
     * block_display のメタデータ。番号は Minecraft 26.3 の Entity / Display / BlockDisplay の定義順。
     *
     * ブロックは原点の角から描かれるので、拡大率の半分だけずらして中心を [Marker.location]（ショップは角）に合わせる。
     */
    private fun metadata(marker: Marker): List<EntityData<*>> {
        // ショップはブロックの角が位置なので中心に合わせて広げ、方向の目印は位置そのものを中心にする
        val offset = if (marker.scale >= 1f) -(marker.scale - 1f) / 2f else -marker.scale / 2f
        return listOf(
            EntityData(INDEX_FLAGS, EntityDataTypes.BYTE, FLAG_GLOWING),
            EntityData(INDEX_TRANSLATION, EntityDataTypes.VECTOR3F, Vector3f(offset, offset, offset)),
            EntityData(INDEX_SCALE, EntityDataTypes.VECTOR3F, Vector3f(marker.scale, marker.scale, marker.scale)),
            EntityData(INDEX_BRIGHTNESS, EntityDataTypes.INT, FULL_BRIGHT),
            EntityData(INDEX_VIEW_RANGE, EntityDataTypes.FLOAT, VIEW_RANGE),
            EntityData(INDEX_BLOCK_STATE, EntityDataTypes.BLOCK_STATE, marker.blockState),
        )
    }

    private fun blockState(blockData: BlockData): Int = SpigotConversionUtil.fromBukkitBlockData(blockData).globalId

    private companion object {
        const val INDEX_FLAGS = 0
        const val INDEX_TRANSLATION = 11
        const val INDEX_SCALE = 12
        const val INDEX_BRIGHTNESS = 16
        const val INDEX_VIEW_RANGE = 17
        const val INDEX_BLOCK_STATE = 23

        /** エンティティのフラグの 0x40 = 発光。 */
        const val FLAG_GLOWING: Byte = 0x40

        /** ブロック光・空の光ともに 15（暗い場所でも見える）。 */
        const val FULL_BRIGHT: Int = (15 shl 4) or (15 shl 20)

        /** 表示距離の倍率（1 = 64 ブロック）。遠くのショップも見えるよう広げる。 */
        const val VIEW_RANGE: Float = 4f

        const val SHOP_SCALE: Float = 1.02f
        const val POINTER_SCALE: Float = 0.5f
    }
}
