plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20-RC" // 保持不變，如果後續有問題再考慮降級
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.personaltutorapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.personaltutorapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // 指定測試運行器
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }
    composeOptions {
        // 使用與穩定 BOM (2024.05.00) 兼容的版本
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    // 如果還沒有，添加 packagingOptions 以處理重複檔案（測試庫有時需要）
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Jetpack Compose (使用穩定 BOM 版本)
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    // Icons 依賴
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.7")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Coroutines and Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.7.2")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // --- 添加以下測試依賴項 ---
    // *** 添加 Firebase Storage ***
    implementation("com.google.firebase:firebase-storage-ktx") // <<< 添加這一行

    // *** 添加 Coil (圖片加載庫) ***
    implementation("io.coil-kt:coil-compose:2.6.0") // <<< 添加這一行 (檢查最新版本)

    // 本地單元測試 (test source set)
    testImplementation("junit:junit:4.13.2") // JUnit 4 核心庫

    // Android 儀器測試 (androidTest source set)
    androidTestImplementation("junit:junit:4.13.2") // JUnit 4 核心庫
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // AndroidX Test - JUnit 4 擴展
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // AndroidX Test - Espresso UI 測試框架
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00")) // 確保測試使用相同的 Compose BOM
    androidTestImplementation("androidx.compose.ui:ui-test-junit4") // Compose UI 測試 JUnit 4 規則

    // 調試時 Compose UI 測試可能需要
    debugImplementation("androidx.compose.ui:ui-test-manifest")

}
