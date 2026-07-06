import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

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
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://plugins.gradle.org/m2/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())

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
        minecraftVersion("1.21.10")
        downloadPlugins {
            modrinth("quickshop-hikari", "6.2.0.11")
            github("dmulloy2", "ProtocolLib", "5.4.0", "ProtocolLib.jar")
            github("EssentialsX", "Essentials", "2.21.2", "EssentialsX-2.21.2.jar")
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

sourceSets.main {
    resourceFactory {
        bukkitPluginYaml {
            name = rootProject.name
            version = project.version.toString()
            website = "https://github.com/Nlkomaru/AdvancedShopFinder"
            main = "$group.advancedshopfinder.AdvancedShopFinder"
            apiVersion = "1.20"
            libraries = libs.bundles.coroutines.asString()
            depend = listOf("QuickShop-Hikari", "ProtocolLib", "Vault")
        }
    }
}

tasks.register("generateTranslate", dev.nikomaru.tasks.GenerateTranslateTask::class)

fun Provider<ExternalModuleDependencyBundle>.asString(): List<String> =
    this.get().map { dependency ->
        "${dependency.group}:${dependency.name}:${dependency.version}"
    }
