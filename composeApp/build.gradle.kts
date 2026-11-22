import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.application")
}

val appVersion = (findProperty("appVersion") as String?) ?: "1.0.0"
val appVersionCode = ((findProperty("appVersionCode") as String?) ?: "1").toInt()
fun prop(name: String, env: String): String =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: (findProperty(name) as String)

// ---------- Генерация AppConfig.kt (адреса серверов, версия) ----------
val generatedDir = layout.buildDirectory.dir("generated/appconfig/kotlin")
val generateAppConfig by tasks.registering {
    val kz = prop("serverKz", "SERVER_KZ")
    val ru = prop("serverRu", "SERVER_RU")
    inputs.property("kz", kz); inputs.property("ru", ru); inputs.property("v", appVersion)
    outputs.dir(generatedDir)
    doLast {
        val f = generatedDir.get().file("com/fhdvideobox/app/AppConfig.kt").asFile
        f.parentFile.mkdirs()
        f.writeText(
            """
            |package com.fhdvideobox.app
            |
            |/** Сгенерировано Gradle — не редактировать вручную. */
            |object AppConfig {
            |    const val VERSION = "$appVersion"
            |    const val SERVER_KZ = "$kz"
            |    const val SERVER_RU = "$ru"
            |}
            """.trimMargin()
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    sourceSets {
        val desktopMain by getting

        commonMain {
            kotlin.srcDir(generateAppConfig)
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

                implementation("io.ktor:ktor-client-core:3.0.1")
                implementation("io.ktor:ktor-client-content-negotiation:3.0.1")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")

                implementation("io.coil-kt.coil3:coil-compose:3.0.4")
            }
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.9.3")
            implementation("androidx.core:core-splashscreen:1.0.1")
            implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
            implementation("io.ktor:ktor-client-okhttp:3.0.1")
            implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
            implementation("androidx.media3:media3-exoplayer:1.4.1")
            implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
            implementation("androidx.media3:media3-ui:1.4.1")
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
            implementation("io.ktor:ktor-client-okhttp:3.0.1")
            implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
            implementation("uk.co.caprica:vlcj:4.8.3")
        }
    }
}

android {
    namespace = "com.fhdvideobox.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fhdvideobox.app"
        minSdk = 21          // Android 5.0 — старые ТВ-приставки тоже
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersion
    }

    signingConfigs {
        create("release") {
            val ks = System.getenv("ANDROID_KEYSTORE_FILE")
            if (ks != null && file(ks).exists()) {
                storeFile = file(ks)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            } else {
                // Нет секретов — подписываем debug-ключом, чтобы APK всё равно ставился
                val dbg = getByName("debug")
                storeFile = dbg.storeFile; storePassword = dbg.storePassword
                keyAlias = dbg.keyAlias; keyPassword = dbg.keyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources.excludes += setOf("META-INF/{AL2.0,LGPL2.1}", "META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties")
    }
}

compose.desktop {
    application {
        mainClass = "com.fhdvideobox.app.MainKt"
        jvmArgs += listOf("-Xmx768m", "-Dfile.encoding=UTF-8")

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "FHDVideobox"
            packageVersion = appVersion
            description = "FHD Videobox — онлайн-кинотеатр"
            vendor = "FHD Videobox"
            // Папка с libVLC для Windows кладётся сюда в CI: resources/windows/vlc
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            modules("java.naming", "java.net.http", "jdk.crypto.ec", "jdk.unsupported", "java.management", "jdk.charsets")

            windows {
                iconFile.set(project.file("icons/icon.ico"))
                menu = true
                shortcut = true
                dirChooser = true
                perUserInstall = false
                menuGroup = "FHD Videobox"
                upgradeUuid = "7d3c1b52-6c1f-4f0a-9d7e-2f6a1f0b9e11"
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
                packageName = "fhdvideobox"
                debMaintainer = "support@fhdvideobox.example"
                appCategory = "AudioVideo"
                menuGroup = "AudioVideo;Video;Player"
                shortcut = true
            }
        }
    }
}
