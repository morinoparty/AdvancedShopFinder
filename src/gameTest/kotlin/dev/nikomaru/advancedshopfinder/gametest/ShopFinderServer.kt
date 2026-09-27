package dev.nikomaru.advancedshopfinder.gametest

import party.morino.fukurou.FukurouConfig
import party.morino.fukurou.junit.GameServerExtension
import party.morino.fukurou.plugin.PluginSource
import party.morino.fukurou.server.Isolation
import party.morino.fukurou.server.ServerSpec
import party.morino.fukurou.server.ServerType
import party.morino.fukurou.server.paper.Paper
import party.morino.fukurou.server.paper.PaperChannel
import kotlin.time.Duration.Companion.seconds

/**
 * AdvancedShopFinder と依存プラグイン（runServer と同じ構成）を入れたサーバー。
 *
 * `@ExtendWith(ShopFinderServer::class)` を付けたテストクラスはすべてこの 1 台のサーバーを共有する。
 */
class ShopFinderServer : GameServerExtension() {
    // /sf のコマンドを実行するため OP にする
    val alice by player("Alice", op = true)

    /**
     * 起動するサーバーの種類。
     *
     * CI は -Pfukurou.minecraftVersion / -Pfukurou.paperChannel を渡す。省略時は 26.2 の alpha までを使う。
     */
    override fun type(config: FukurouConfig): ServerType =
        Paper.fromProperties(config, defaultVersion = "26.2", defaultChannel = PaperChannel.Alpha)

    override fun ServerSpec.configure() {
        // result の id は paper-<version>-shop-finder になる
        label = "shop-finder"
        plugins {
            // gameTest タスクが shadowJar の成果物のパスを fukurou.plugin.advancedshopfinder に渡す
            underTest(PluginSource.systemProperty("advancedshopfinder"))
            // plugin.yml の depend。dmulloy2 の Jenkins は 403 を返すため GitHub の開発版リリースから取得する
            dependency(PluginSource.githubRelease("dmulloy2/ProtocolLib", tag = "dev-build", asset = "ProtocolLib.jar"))
            dependency(PluginSource.url(QUICKSHOP_URL))
            // QuickShop-Hikari の経済プロバイダ
            dependency(PluginSource.githubRelease("MilkBowl/Vault", tag = "1.7.3", asset = "Vault.jar"))
            dependency(PluginSource.url(ESSENTIALS_URL))
        }
        // ブロックは使わないので、リセットはプレイヤーの状態だけにする。
        // 参加直後はクライアントがチャンクの描画で忙しく、チャットの入力を取りこぼすので長めに待つ
        isolation = Isolation.Reset(arena = null, settle = 5.seconds)
    }

    companion object {
        const val QUICKSHOP_URL = "https://cdn.modrinth.com/data/ijC5dDkD/versions/OxlW1jL5/QuickShop-Hikari-6.3.0.3.jar"
        const val ESSENTIALS_URL = "https://cdn.modrinth.com/data/hXiIvTyT/versions/nY6VN1XH/EssentialsX-2.22.0.jar"

        /** コマンドの処理中に例外が出たときのログ（cloud の "Exception executing command handler" など）。 */
        val COMMAND_ERRORS = Regex("(Exception executing command handler|ArrayIndexOutOfBoundsException)")

        /** コマンドを受け付けなかったときにチャットへ出る文言（cloud の構文エラーと Minecraft の不明なコマンド）。 */
        val CHAT_ERRORS = Regex("(Invalid command syntax|Unknown or incomplete command|Unknown command)")
    }
}
