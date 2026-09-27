package dev.nikomaru.advancedshopfinder.gametest

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import party.morino.fukurou.pause
import party.morino.fukurou.player.KeySym
import kotlin.time.Duration.Companion.seconds

/**
 * `/sf setting` で検索設定 GUI が開くことを、実際のクライアントで確かめる。
 *
 * メソッド名がそのまま fukurou のテスト id になる。
 */
@Tag("gui")
@ExtendWith(ShopFinderServer::class)
class SettingGuiTest {
    @Test
    @DisplayName("sf setting opens the find option GUI without errors")
    suspend fun `setting-gui-opens`(env: ShopFinderServer) {
        val mark = env.server.mark()
        env.alice.sendCommand("sf setting")
        // コマンドは非同期で処理され、GUI はメインスレッドで開くので少し待つ
        pause(2.seconds)
        env.alice.screenshot("setting-gui")
        // InventoryFramework 0.12.1 のバグではここで ArrayIndexOutOfBoundsException が出ていた
        env.server.assertNoLog(ShopFinderServer.COMMAND_ERRORS, after = mark)
        // 次のテストがチャットを使えるよう GUI を閉じる
        env.alice.pressKey(KeySym.ESCAPE)
    }
}
