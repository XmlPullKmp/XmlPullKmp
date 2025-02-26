plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.test.resources.plugin)
    id("maven-publish")
}

group = "io.github.xmlpullkmp"
version = "1.0.0"

kotlin {
    jvmToolchain(17)

    jvm {
        withJava()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    mingwX64()
    macosX64()
    macosArm64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.fleeksoft.io.core)
                api(libs.fleeksoft.io.io)
                api(libs.fleeksoft.io.charset.ext)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.test.resources)
            }
        }

        jvmTest.dependencies {
            implementation(libs.junit)
            implementation(libs.junit.params)
        }
    }
}

tasks.withType<Test> {
    // check if target is jvm
    if (name.contains("jvm")) {
        maxHeapSize = "4g"
        forkEvery = 1
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        useJUnitPlatform() // Enable JUnit 5
    }
}






