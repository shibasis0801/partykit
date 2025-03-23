plugins {
    kotlin("multiplatform") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("org.jetbrains.kotlinx.atomicfu") version "0.27.0"
    id("com.android.library") version "8.8.0"

}

group = "io.partykit"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
}

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

    sourceSets {
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:3.1.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")
//            implementation("co.touchlab:kermit:2.0.4")

        }
        jvmTest.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.1.1")
            implementation(kotlin("test"))
        }
    }
}

android {
    compileSdk = 34
    namespace = "io.partykit.partysocket"
    defaultConfig {
        minSdk = 21
    }
}
