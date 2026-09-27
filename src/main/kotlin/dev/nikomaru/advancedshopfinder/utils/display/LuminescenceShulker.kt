package dev.nikomaru.advancedshopfinder.utils.display

import com.github.retrooper.packetevents.manager.player.PlayerManager
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import dev.nikomaru.advancedshopfinder.utils.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Optional
import java.util.UUID
import kotlin.random.Random

/**
 * ショップの位置に、発光（0x40）・透明（0x20）のシュルカーをパケットだけで表示して目立たせる。
 *
 * 実体のエンティティは作らず、対象のプレイヤーにだけ見える。
 */
class LuminescenceShulker : KoinComponent {
    private val playerManager: PlayerManager by inject()
    private val ids = arrayListOf<Int>()
    private val blocks = arrayListOf<Location>()
    private val target = arrayListOf<Player>()

    fun addBlock(location: Location) {
        blocks.add(location)
    }

    fun addTarget(player: Player) {
        target.add(player)
    }

    suspend fun display() {
        withContext(Dispatchers.async) {
            blocks.forEach { location ->
                target.forEach {
                    val entityId = Random.nextInt(Int.MAX_VALUE)
                    ids.add(entityId)
                    val spawnPacket =
                        WrapperPlayServerSpawnEntity(
                            entityId,
                            Optional.of(UUID.randomUUID()),
                            EntityTypes.SHULKER,
                            Vector3d(location.x, location.y, location.z),
                            0f,
                            0f,
                            0f,
                            0,
                            Optional.empty(),
                        )
                    // インデックス 0 はエンティティのフラグ。0x40 = 発光、0x20 = 透明
                    val metadataPacket =
                        WrapperPlayServerEntityMetadata(
                            entityId,
                            listOf<EntityData<*>>(EntityData(0, EntityDataTypes.BYTE, ENTITY_FLAGS)),
                        )
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

    private companion object {
        const val ENTITY_FLAGS: Byte = 0x60
    }
}
