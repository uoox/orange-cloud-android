import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// 官方 OAuth Client（PKCE 公开客户端，非机密；与 iOS OAuthConfig.swift 同值）。
// oss 自编译者在 local.properties 覆盖 OAUTH_CLIENT_ID 并自建回调，官方 Client 不向第三方构建开放。
val officialOAuthClientId = "eae9090b8f240e6dd54d9926a55d56ce"
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun oauthClientId(default: String): String =
    localProps.getProperty("OAUTH_CLIENT_ID")
        ?: providers.gradleProperty("OAUTH_CLIENT_ID").orNull
        ?: default

// 发布签名参数：依次读 local.properties → 应用专属环境变量 ORANGE_CLOUD_<KEY> → 通用 <KEY>（均不入库）。
// 用 ORANGE_CLOUD_ 前缀的应用专属变量（放 ~/.zshrc）避免与本机其它 Android 项目的通用 RELEASE_* 串用。
// 缺失时 release 保持未签名，不影响他人构建（构建仍成功，只是产物需自行签名）。
fun signingProp(key: String): String? =
    localProps.getProperty(key)
        ?: System.getenv("ORANGE_CLOUD_$key")
        ?: System.getenv(key)
val releaseStoreFile: String? = signingProp("RELEASE_STORE_FILE")

android {
    namespace = "jiamin.chen.orangecloud"
    compileSdk = 36

    defaultConfig {
        applicationId = "jiamin.chen.orangecloud"
        minSdk = 31
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // OAuth 回调（Web 后端 302 跳回的自定义 scheme）
        manifestPlaceholders["oauthScheme"] = "orangecloud"
        manifestPlaceholders["oauthHost"] = "oauth"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_OSS", "false")
            buildConfigField("String", "OAUTH_CLIENT_ID", "\"${oauthClientId(officialOAuthClientId)}\"")
        }
        create("oss") {
            dimension = "distribution"
            applicationIdSuffix = ".oss"
            versionNameSuffix = "-oss"
            buildConfigField("boolean", "IS_OSS", "true")
            // oss 默认不带官方 Client；自编译者用 local.properties 填
            buildConfigField("String", "OAUTH_CLIENT_ID", "\"${oauthClientId("")}\"")
        }
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = signingProp("RELEASE_STORE_PASSWORD")
                keyAlias = signingProp("RELEASE_KEY_ALIAS")
                keyPassword = signingProp("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 提供签名凭据时启用发布签名（local.properties / 环境变量），否则产物未签名。
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.adaptive.navigation)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // 网络
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // 持久化（Token 走 Keystore + DataStore，不用 EncryptedSharedPreferences）
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // 平台特色
    implementation(libs.androidx.browser)        // Custom Tabs（OAuth）
    "playImplementation"(libs.billing.ktx)        // Play Billing 仅 play 风味
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coil.compose)

    // 测试
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
