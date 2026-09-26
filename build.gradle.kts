import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.time.Duration

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.resource.factory)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

group = "dev.nikomaru"
val version: String by project

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://central.sonatype.com/repository/maven-snapshots/")
    maven("https://jitpack.io")
    maven("https://plugins.gradle.org/m2/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly(libs.paper.api)

    implementation(libs.bundles.commands)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.bundles.coroutines)

    compileOnly(libs.vault.api)

    compileOnly(libs.protocol.lib)

    compileOnly(libs.quickshop.bukkit)
    compileOnly(libs.quickshop.api)

    implementation(libs.bundles.arrow)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)

    implementation(libs.inventoryframework)

    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mock.bukkit)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.bundles.koin.test)
}

kotlin {
    jvmToolchain(25)
}

detekt {
    // 既存コードのスタイル差異でビルドを止めないため、検出のみ行う
    ignoreFailures = true
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    debug.set(true)
    ignoreFailures.set(true)
    filter {
        include("src/**")
        include("buildSrc/**")
        exclude("**/config/**")
    }
}

tasks {
    compileKotlin {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
        compilerOptions.javaParameters = true
        compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_2_2)
    }
    compileTestKotlin {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
    }
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        // InventoryFramework は各プラグインへシェードして使うため、衝突回避に再配置する
        relocate(
            "com.github.stefvanschie.inventoryframework",
            "dev.nikomaru.advancedshopfinder.libs.inventoryframework",
        )
    }
    runServer {
        minecraftVersion("26.3")
        downloadPlugins {
            modrinth("quickshop-hikari", "6.3.0.3")
            github("dmulloy2", "ProtocolLib", "dev-build", "ProtocolLib.jar")
            url("https://cdn.modrinth.com/data/hXiIvTyT/versions/nY6VN1XH/EssentialsX-2.22.0.jar")
            github("Milkbowl", "Vault", "1.7.3", "Vault.jar")
        }
    }
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
    test {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            events("passed", "skipped", "failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}

testing {
    suites {
        // ゲーム内テストは Minecraft のクライアントと Xvfb が要るため、既定の test（./gradlew build が実行する）とは分ける。
        // 独自の JvmTestSuite は check に含まれないので、build では実行されない
        register<JvmTestSuite>("gameTest") {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(libs.fukurou)
                // JUnit 6 が suspend のテストメソッドを呼ぶには kotlinx-coroutines-core が要る
                implementation(libs.kotlinx.coroutines.core)
                runtimeOnly(libs.junit.platform.launcher)
            }
            targets.configureEach {
                testTask.configure {
                    description = "Runs the in-game tests with fukurou (needs Xvfb, xdotool, xmodmap and Mesa; CI only)"
                    // CI は build ジョブの JAR を -Pfukurou.plugin.advancedshopfinder で渡す。無ければここで shadowJar を作る
                    val prebuilt = providers.gradleProperty("fukurou.plugin.advancedshopfinder")
                    if (!prebuilt.isPresent) dependsOn(tasks.shadowJar)
                    val pluginJar =
                        prebuilt.orElse(tasks.shadowJar.flatMap { it.archiveFile }.map { it.asFile.absolutePath })
                    // -Pfukurou.* をすべてシステムプロパティとして渡す（minecraftVersion, paperChannel, acceptEula, outDir …）
                    val forwarded = providers.gradlePropertiesPrefixedBy("fukurou.")
                    jvmArgumentProviders.add(
                        CommandLineArgumentProvider {
                            forwarded.get().filterKeys { it != "fukurou.plugin.advancedshopfinder" }.map { (k, v) -> "-D$k=$v" } +
                                "-Dfukurou.plugin.advancedshopfinder=${pluginJar.get()}"
                        },
                    )
                    systemProperty("fukurou.outDir.default", layout.buildDirectory.dir("fukurou/out").get().asFile.absolutePath)
                    systemProperty("fukurou.workDir.default", layout.buildDirectory.dir("fukurou/work").get().asFile.absolutePath)
                    // サーバーのリースとメモリ予算は 1 つの JVM を前提にしている
                    maxParallelForks = 1
                    forkEvery = 0
                    maxHeapSize = "512m"
                    // 実機テストは入力が同じでも結果が変わるので毎回実行する
                    outputs.upToDateWhen { false }
                    // CI の timeout-minutes（30）より短くし、強制終了の前に JUnit の XML と result.json を書き終える
                    timeout.set(Duration.ofMinutes(25))
                    testLogging {
                        showStandardStreams = true
                        events("passed", "skipped", "failed")
                        exceptionFormat = TestExceptionFormat.FULL
                    }
                }
            }
        }
    }
}

sourceSets.main {
    resourceFactory {
        bukkitPluginYaml {
            name = rootProject.name
            version = project.version.toString()
            website = "https://github.com/morinoparty/AdvancedShopFinder"
            main = "$group.advancedshopfinder.AdvancedShopFinder"
            apiVersion = "1.20"
            libraries = libs.bundles.coroutines.asString() +
                listOf("org.jetbrains.kotlin:kotlin-stdlib:2.4.20")
            depend = listOf("QuickShop-Hikari", "ProtocolLib")
        }
    }
}

tasks.register("generateTranslate", dev.nikomaru.tasks.GenerateTranslateTask::class)

fun Provider<ExternalModuleDependencyBundle>.asString(): List<String> =
    this.get().map { dependency ->
        "${dependency.group}:${dependency.name}:${dependency.version}"
    }
