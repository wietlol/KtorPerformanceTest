plugins {
    kotlin("multiplatform") version "1.6.20-M1"
}

group = "ktor-performance-test"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktor_version = "1.6.7"
kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-curl:$ktor_version")
            }
        }
        val nativeTest by getting
    }
}
