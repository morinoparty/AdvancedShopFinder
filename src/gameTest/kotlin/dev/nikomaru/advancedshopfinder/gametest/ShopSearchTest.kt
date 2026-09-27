package dev.nikomaru.advancedshopfinder.gametest

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import party.morino.fukurou.log.LogMark
import party.morino.fukurou.pause
import party.morino.fukurou.world.BlockPos
import party.morino.fukurou.world.Location
import kotlin.time.Duration.Companion.seconds

/**
 * QuickShop のショップを実際に作り、`/sf search` で見つかることを確かめる。
 */
@Tag("search")
@ExtendWith(ShopFinderServer::class)
class ShopSearchTest {
    /** 近くのショップが見つかり、その位置が光る（PacketEvents で送る発光シュルカー）。 */
    @Test
    @DisplayName("sf search finds a QuickShop shop and highlights it")
    suspend fun `search-highlights-shop`(env: ShopFinderServer) {
        val alice = env.alice
        val mark = env.server.mark()
        val chatMark = alice.mark()
        createShop(env, NEAR_CHEST, "diamond")

        alice.sendCommand("sf search diamond")
        alice.awaitChat(Regex("の検索結果: 1件"), timeout = 10.seconds, after = chatMark)
        // 発光シュルカーは 1 秒表示・0.4 秒非表示を繰り返すので、表示中に撮る
        pause(0.5.seconds)
        alice.screenshot("highlighted-shop")

        assertNoErrors(env, mark, chatMark)
    }

    /**
     * チャンクが読み込まれていない遠くのショップも、チャンクを読み込んで在庫を数え、結果に出る。
     *
     * QuickShop はチャンクが読み込まれていないショップの在庫を 0 と返すので、以前は在庫切れとして消えていた。
     */
    @Test
    @DisplayName("sf search counts the stock of a shop in an unloaded chunk")
    suspend fun `search-finds-shop-in-unloaded-chunk`(env: ShopFinderServer) {
        val alice = env.alice
        val mark = env.server.mark()
        val chatMark = alice.mark()
        createShop(env, FAR_CHEST, "emerald")

        // 原点に戻り、ショップのチャンクが解放されるまで待つ
        alice.teleport(Location(0.5, -60.0, 0.5))
        var unloaded = false
        repeat(30) {
            if (!unloaded) {
                pause(1.seconds)
                val response = env.server.command("execute in minecraft:overworld if loaded ${FAR_CHEST.toCommandArgs()}").text.orEmpty()
                unloaded = response.contains("failed", ignoreCase = true)
            }
        }
        assertTrue(unloaded, "the shop chunk at $FAR_CHEST was not unloaded")

        alice.sendCommand("sf search emerald")
        alice.awaitChat(Regex("の検索結果: 1件"), timeout = 10.seconds, after = chatMark)
        alice.awaitChat(Regex("在庫: 64 \\* 1個"), timeout = 10.seconds, after = chatMark)
        alice.screenshot("unloaded-shop-result")

        assertNoErrors(env, mark, chatMark)
    }

    /**
     * [chest] にチェストを置いて Alice に見させ、1 個 10 の [item] の販売ショップにして 64 個入れる。
     */
    private suspend fun createShop(
        env: ShopFinderServer,
        chest: BlockPos,
        item: String,
    ) {
        val alice = env.alice
        env.server.fixture("$item-shop") {
            setBlock(chest, "minecraft:chest")
            // ショップの作成費用を払えるようにする（経済は EssentialsX）
            command("eco give Alice 1000")
            // テレポートの pitch がクライアントの視点に反映されないことがあるので、チェストは目の高さ（足元 +1）に置いて水平に見る
            alice.teleport(Location(chest.x + 0.5, chest.y - 1.0, chest.z - 2.5, yaw = 0f, pitch = 0f))
            pause(3.seconds)
        }
        alice.sendCommand("qs create 10 $item")
        pause(2.seconds)
        // 在庫はショップを作った後に入れる（作成前に入れると在庫切れのショップになった）
        // EssentialsX が item コマンドを上書きしている（プレイヤー専用）ので、バニラの minecraft:item を明示する
        env.server.command(
            "execute in minecraft:overworld run minecraft:item replace block ${chest.toCommandArgs()} container.0 with minecraft:$item 64",
        )
        pause(1.seconds)
        // 在庫が本当に入ったかをチェストの NBT で確かめる
        val items = env.server.command("execute in minecraft:overworld run data get block ${chest.toCommandArgs()} Items").text.orEmpty()
        assertTrue(items.contains("minecraft:$item"), "chest has no $item: $items")
    }

    private fun assertNoErrors(
        env: ShopFinderServer,
        mark: LogMark,
        chatMark: LogMark,
    ) {
        env.server.assertNoLog(ShopFinderServer.COMMAND_ERRORS, after = mark)
        env.server.assertNoLog(ShopFinderServer.PLUGIN_STACKTRACE, after = mark)
        env.alice.assertNoChat(ShopFinderServer.CHAT_ERRORS, after = chatMark)
    }

    private companion object {
        val NEAR_CHEST = BlockPos(0, -59, 3)

        // 描画距離・シミュレーション距離より十分遠い場所
        val FAR_CHEST = BlockPos(2000, -59, 2003)
    }
}
