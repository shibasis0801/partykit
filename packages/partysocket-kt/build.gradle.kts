plugins {
    kotlin("multiplatform") version "2.1.10" apply false
    kotlin("android") version "2.1.10" apply false
    kotlin("plugin.serialization") version "2.1.10" apply false
    id("com.android.application") version "8.8.0" apply false
    id("com.android.library") version "8.8.0" apply false
    id("org.jetbrains.compose") version "1.8.0-alpha03" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10" apply false
}

buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

allprojects {
    group = "io.partykit"
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven(url = "https://www.jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    }
}
