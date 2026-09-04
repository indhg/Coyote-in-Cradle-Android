import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

/** 版本号：阶段标注（如 0.1.0-m1.2），同时写进 APK 文件名 */
val appVersion = "1.1.7"

/** debug 构建时间戳：yyyyMMdd-HHmm，用于 versionNameSuffix 与 APK 文件名 */
val buildTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))

android {
    namespace = "com.indhg.aiforcoyote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.indhg.aiforcoyote"
        minSdk = 26
        targetSdk = 34
        versionCode = 9
        versionName = appVersion
    }

    buildTypes {
        debug {
            versionNameSuffix = "-dev.$buildTimestamp"
        }
        release {
            isMinifyEnabled = false
            // 自用侧载：release 复用 debug 签名（换正式签名时在此处替换）
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// APK 文件名：
// debug:   Coyote-in-Cradle-1.1.7-dev.<ts>-debug.apk
// release: Coyote-in-Cradle-1.1.7-release.apk
androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            val fileName = if (variant.buildType == "debug") {
                "Coyote-in-Cradle-$appVersion-dev.$buildTimestamp-debug.apk"
            } else {
                "Coyote-in-Cradle-$appVersion-${variant.name}.apk"
            }
            (output as com.android.build.api.variant.impl.VariantOutputImpl)
                .outputFileName.set(fileName)
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
}
