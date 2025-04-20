
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

version = "1.0.0"

kotlin {
    androidTarget()
    jvm()
    js(IR) {
        browser()
        nodejs()
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            api(project(":partysocket"))
            implementation(compose.runtime)
            implementation(compose.foundation)

        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    compileSdk = 35
    namespace = "io.partykit.partysocket"
    defaultConfig {
        minSdk = 21
    }
}
