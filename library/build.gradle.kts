import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.test.resources.plugin)
    alias(libs.plugins.maven.publish)
    signing
}

group = "io.github.xmlpullkmp"
version = "1.0.0"

android {
    namespace = "io.github.xmlpullkmp"
    defaultConfig {
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        publishLibraryVariants("release")
    }
    jvm()
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

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    coordinates(
        groupId = group.toString(),
        artifactId = "xmlpullkmp",
        version = version.toString()
    )

    println(SonatypeHost.DEFAULT.toString())

    pom {
        name.set("XmlPullKmp")
        description.set("Kotlin Multiplatform implementation of XmlPullParser.")
        inceptionYear.set("2025")
        url.set("https://github.com/XmlPullKmp/XmlPullKmp")

        licenses {
            license {
                name.set("Apache-2.0 license")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }

        developers {
            developer {
                id.set("matekar")
                name.set("Marcin Kiżewski")
                email.set("kiewskimarcin@gmail.com")
            }

            developer {
                id.set("lemcoder")
                name.set("Mikołaj Lemański")
                email.set("lemanski.dev@gmail.com")
            }
        }

        scm {
            url.set("https://github.com/XmlPullKmp/XmlPullKmp")
        }
    }
}

// Sign with default plugin
signing {
    useInMemoryPgpKeys(
        System.getenv("PRIVATE_GPG_KEY"),
        System.getenv("PRIVATE_GPG_KEY_PASSWORD")
    )
    sign(publishing.publications)

    // Temporary workaround, see https://github.com/gradle/gradle/issues/26091#issuecomment-1722947958
    tasks.withType<AbstractPublishToMaven>().configureEach {
        val signingTasks = tasks.withType<Sign>()
        mustRunAfter(signingTasks)
    }
}

// Disable iOS testing
afterEvaluate {
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        tasks.getByName("compileTestKotlinIosArm64").enabled = false
        tasks.getByName("compileTestKotlinIosX64").enabled = false
        tasks.getByName("compileTestKotlinIosSimulatorArm64").enabled = false
        tasks.getByName("compileTestKotlinMacosArm64").enabled = false
        tasks.getByName("compileTestKotlinMacosX64").enabled = false
    }
}


