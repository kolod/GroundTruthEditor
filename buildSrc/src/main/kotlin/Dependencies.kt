@file:Suppress("MemberVisibilityCanBePrivate", "unused")

import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

fun PluginDependenciesSpec.shadow(module: String): PluginDependencySpec = id(module)

object Shadow {
	const val version = "7.1.0"
	const val id = "com.github.johnrengelman.shadow"
}
object Jvm {
	const val version = "1.8"
}

object Kotlin {
	const val version = "1.6.0"
	const val stdlibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
	const val jvmId = "jvm"
	const val kaptId = "kapt"
}

object JNA {
	const val version = "5.9.0"
	const val core = "net.java.dev.jna:jna:$version"
	const val platform = "net.java.dev.jna:jna-platform:$version"
}

object Logger {
	const val version = "2.15.0"
	const val core = "org.apache.logging.log4j:log4j-core:$version"
	const val api = "org.apache.logging.log4j:log4j-api:$version"
	const val slf4j = "org.apache.logging.log4j:log4j-slf4j-impl:$version"
}

object Manifests {
	const val version = "1.1"
	const val core = "com.jcabi:jcabi-manifests:$version"
}

object FlatLookAndFeel {
	const val version = "1.6.4"
	const val core = "com.formdev:flatlaf:$version"
	const val intellij = "com.formdev:flatlaf-intellij-themes:$version"
	const val extras = "com.formdev:flatlaf-extras:$version"
}

object SqLite {
	const val version = "3.36.0.3"
	const val core = "org.xerial:sqlite-jdbc:$version"
}

object Exposed {
	const val version = "0.36.2"
	const val core = "org.jetbrains.exposed:exposed-core:$version"
	const val dao = "org.jetbrains.exposed:exposed-dao:$version"
	const val jdbc = "org.jetbrains.exposed:exposed-jdbc:$version"
	const val time = "org.jetbrains.exposed:exposed-java-time:$version"
}

object Tesseract {
	const val version = "4.5.5"
	const val core = "net.sourceforge.tess4j:tess4j:$version"
}

object OpenHFT {
	const val version = "0.15"
	const val hashing = "net.openhft:zero-allocation-hashing:$version"
}

object Junit {
	const val version = "5.8.1"
	const val core = "org.junit.jupiter:junit-jupiter:$version"
	const val platform = "org.junit:junit-bom:$version"
}

object Hunspell {
	const val version = "1.1.1"
	const val core = "com.gitlab.dumonts:hunspell:$version"
}

object Kolod {
	const val group = "io.github.kolod"
	const val appender = "$group:log4j-textarea-appender:1.0"
}
