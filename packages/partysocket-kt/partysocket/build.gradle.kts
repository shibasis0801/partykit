plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
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
            api("io.ktor:ktor-client-core:3.1.1")
            api("io.ktor:ktor-serialization-kotlinx-json:3.1.1")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
            api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")
//            implementation("co.touchlab:kermit:2.0.4")

        }
        jvmTest.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.1.1")
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
