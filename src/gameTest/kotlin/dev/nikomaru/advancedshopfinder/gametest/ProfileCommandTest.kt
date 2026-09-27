package dev.nikomaru.advancedshopfinder.gametest

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import party.morino.fukurou.pause
import party.morino.fukurou.player.KeySym
import kotlin.time.Duration.Companion.seconds

/**
 * `/sf setting use <profile>` で使用中のプロファイルが切り替わることを、`/sf setting list` の表示で確かめる。
 */
@Tag("profile")
@ExtendWith(ShopFinderServer::class)
class ProfileCommandTest {
    @Test
    @DisplayName("sf setting use switches the active profile")
    suspend fun `setting-use-switches-profile`(env: ShopFinderServer) {
        val alice = env.alice
        val mark = env.server.mark()
        val chatMark = alice.mark()

        // プロファイルを作る（作成と同時に GUI が開くので閉じてからチャットを使う）
        alice.sendCommand("sf setting mining")
        pause(2.seconds)
        alice.pressKey(KeySym.ESCAPE)
        pause(1.seconds)

        switchAndCheck(env, "mining")
        switchAndCheck(env, "default")

        // プロファイル名がタブ補完の候補に出ることを目で確認するため、入力途中のチャット欄を撮る
        alice.pressKey(KeySym("t"))
        pause(0.5.seconds)
        alice.typeText("/sf setting use ")
        pause(1.5.seconds)
        alice.screenshot("completion-profiles")
        // 1 回目の Escape は補完候補を閉じるだけなので、もう 1 回押してチャット欄を閉じる
        // （開いたままだと次のテストの入力が書き足されて別のコマンドになる）
        alice.pressKey(KeySym.ESCAPE)
        pause(0.5.seconds)
        alice.pressKey(KeySym.ESCAPE)

        env.server.assertNoLog(ShopFinderServer.COMMAND_ERRORS, after = mark)
        // プロファイルの補完は入力中に非同期で何度も呼ばれるので、読み込みの例外が出ていないことも確かめる
        env.server.assertNoLog(ShopFinderServer.PLUGIN_STACKTRACE, after = mark)
        alice.assertNoChat(ShopFinderServer.CHAT_ERRORS, after = chatMark)
    }

    /** 使用するプロファイルを切り替え、一覧で使用中の印がそのプロファイルに付くことを確かめる。 */
    private suspend fun switchAndCheck(
        env: ShopFinderServer,
        profile: String,
    ) {
        val alice = env.alice
        val before = alice.mark()
        alice.sendCommand("sf setting use $profile")
        alice.awaitChat(Regex("プロファイル '$profile' を使用します"), timeout = 10.seconds, after = before)
        val listed = alice.mark()
        alice.sendCommand("sf setting list")
        alice.awaitChat(Regex("- $profile \\(使用中\\)"), timeout = 10.seconds, after = listed)
        alice.screenshot("list-$profile")
    }
}
