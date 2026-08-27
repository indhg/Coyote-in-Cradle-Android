plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

/** 版本号：阶段标注（如 0.1.0-m1.2），同时写进 APK 文件名 */
val appVersion = "0.1.0-m3.1"

android {
    namespace = "com.indhg.aiforcoyote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.indhg.aiforcoyote"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = appVersion
    }

    buildTypes {
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

// APK 文件名带版本号：Coyote-in-Cradle-<versionName>-<variant>.apk
androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            (output as com.android.build.api.variant.impl.VariantOutputImpl)
                .outputFileName.set("Coyote-in-Cradle-$appVersion-${variant.name}.apk")
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
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
