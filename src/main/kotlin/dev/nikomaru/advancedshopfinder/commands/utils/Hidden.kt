package dev.nikomaru.advancedshopfinder.commands.utils

import org.incendo.cloud.CommandManager
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.component.CommandComponent
import org.incendo.cloud.key.CloudKey

/**
 * 付けたコマンドをタブ補完に出さない（実行はできる）。
 *
 * cloud v2 には v1 の `@Hidden` が無いため、[HiddenCommands.install] でコマンドのメタに印を付け、
 * 補完の後処理でその印の付いたコマンドのリテラルを取り除く。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Hidden

object HiddenCommands {
    private val HIDDEN: CloudKey<Boolean> = CloudKey.of("advancedshopfinder:hidden", Boolean::class.javaObjectType)

    /** [Hidden] を読み取るようにし、補完から隠しコマンドを除く。コマンドを parse する前に呼ぶこと。 */
    fun <C> install(annotationParser: AnnotationParser<C>) {
        annotationParser.registerBuilderModifier(Hidden::class.java) { _, builder -> builder.meta(HIDDEN, true) }
        val manager = annotationParser.manager()
        manager.suggestionProcessor(
            manager.suggestionProcessor().then { _, suggestions ->
                val hidden = hiddenLiterals(manager)
                suggestions.filter { it.suggestion() !in hidden }
            },
        )
    }

    /** 隠しコマンドにだけ使われているリテラル（ルートを除く）。表示するコマンドと共有するリテラルは隠さない。 */
    private fun <C> hiddenLiterals(manager: CommandManager<C>): Set<String> {
        val (hidden, visible) = manager.commands().partition { it.commandMeta().getOrDefault(HIDDEN, false) }
        val visibleLiterals = visible.flatMap { it.literals() }.toSet()
        return hidden.flatMap { it.literals() }.filterNot { it in visibleLiterals }.toSet()
    }

    private fun <C> org.incendo.cloud.Command<C>.literals(): List<String> =
        components()
            .drop(1)
            .filter { it.type() == CommandComponent.ComponentType.LITERAL }
            .map { it.name() }
}
