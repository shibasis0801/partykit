plugins {
    kotlin("multiplatform") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("com.android.library") version "8.4.0"
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
            implementation("io.ktor:ktor-client-core:3.1.0")
            implementation("io.ktor:ktor-client-okhttp:3.1.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")

        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    compileSdk = 33
    namespace = "io.partykit.partysocket"
    defaultConfig {
        minSdk = 21
    }
}
