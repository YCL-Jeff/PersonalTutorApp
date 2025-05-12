// build.gradle.kts (Project)
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal() // 確保包含 Gradle Plugin Portal
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.8.0") // 使用穩定版本
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21") // 使用 Kotlin 2.0.21，支援 Compose
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.48") // Hilt 插件
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.0.21-1.0.27") // KSP 插件
        classpath("com.google.gms:google-services:4.4.2") // Google Services 插件
    }
}