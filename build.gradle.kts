import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.util.Date

plugins {
	application
	kotlin(Kotlin.jvmId) version Kotlin.version
	kotlin(Kotlin.kaptId) version Kotlin.version
	shadow(Shadow.id) version Shadow.version
}

project.ext {
	set("mainClassName", "io.github.kolod.GroundTruthEditor")
}

group = "io.github.kolod"
version = SimpleDateFormat("yy.M.d").format(Date())


repositories {
	mavenCentral()
}

dependencies {
	implementation(kotlin("stdlib"))
	implementation(Kotlin.stdlibJdk8)
	implementation(Logger.core)
	implementation(Logger.api)
	implementation(Logger.slf4j)
	implementation(Manifests.core)
	implementation(FlatLookAndFeel.core)
	implementation(FlatLookAndFeel.intellij)
	implementation(FlatLookAndFeel.extras)
	implementation(OpenHFT.hashing)
	testImplementation(kotlin("test"))
	testImplementation(platform(Junit.platform))
	testImplementation(Junit.core)
}

application {
	mainClass.set(project.ext["mainClassName"] as String)
}

tasks.test {
	useJUnitPlatform()
	testLogging {
		events("passed", "skipped", "failed")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		jvmTarget = Jvm.version
	}
}

tasks.withType<ShadowJar> {
	manifest {
		attributes["Implementation-Title"] = "GroundTruthEditor"
		attributes["Main-Class"] = project.ext["mainClassName"] as String
		attributes["Author"] = "Oleksandr Kolodkin"
		attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
		attributes["Multi-Release"] = "true"
		attributes["Build-Date"] = SimpleDateFormat("dd/MM/yyyy").format(Date())
	}
}

tasks {
	build {
		dependsOn(shadowJar)
	}
}
