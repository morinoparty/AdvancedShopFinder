package dev.nikomaru.advancedshopfinder.gametest

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import party.morino.fukurou.pause
import party.morino.fukurou.world.BlockPos
import party.morino.fukurou.world.Location
import kotlin.time.Duration.Companion.seconds

/**
 * QuickShop のショップを実際に作り、`/sf search` で見つかることと、ショップの位置が光ること
 * （PacketEvents で送る発光シュルカー）を確かめる。
 */
@Tag("search")
@ExtendWith(ShopFinderServer::class)
class ShopSearchTest {
    @Test
    @DisplayName("sf search finds a QuickShop shop and highlights it")
    suspend fun `search-highlights-shop`(env: ShopFinderServer) {
        val alice = env.alice
        env.server.fixture("diamond-shop") {
            // ダイヤを入れたチェストを置き、Alice にそのチェストを見させる
            setBlock(CHEST, "minecraft:chest")
            command("item replace block ${CHEST.toCommandArgs()} container.0 with minecraft:diamond 64")
            // ショップの作成費用を払えるようにする（経済は EssentialsX）
            command("eco give Alice 1000")
            alice.teleport(Location(0.5, -60.0, 0.5, yaw = 0f, pitch = 0f))
            pause(2.seconds)
        }
        val mark = env.server.mark()
        val chatMark = alice.mark()

        // 見ているチェストを、1 個 10 のダイヤの販売ショップにする
        alice.sendCommand("qs create 10 diamond")
        pause(2.seconds)

        alice.sendCommand("sf search diamond")
        alice.awaitChat(Regex("の検索結果: 1件"), timeout = 10.seconds, after = chatMark)
        // 発光シュルカーは 1 秒表示・0.4 秒非表示を繰り返すので、表示中に撮る
        pause(0.5.seconds)
        alice.screenshot("highlighted-shop")

        env.server.assertNoLog(ShopFinderServer.COMMAND_ERRORS, after = mark)
        env.server.assertNoLog(ShopFinderServer.PLUGIN_STACKTRACE, after = mark)
        alice.assertNoChat(ShopFinderServer.CHAT_ERRORS, after = chatMark)
    }

    private companion object {
        // テレポートの pitch がクライアントの視点に反映されないことがあるので、目の高さ（足元 +1）に置いて水平に見る
        val CHEST = BlockPos(0, -59, 3)
    }
}
