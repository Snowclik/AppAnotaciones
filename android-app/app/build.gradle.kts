import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

fun String?.toBuildConfigLiteral(): String {
    val escaped = (this ?: "").trim()
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$escaped\""
}

val appVersionName = "2.5.1"

android {
    namespace = "com.appnotas.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.appnotas.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = appVersionName

        val tursoUrl = localProperties.getProperty("TURSO_DATABASE_URL").toBuildConfigLiteral()
        val tursoToken = localProperties.getProperty("TURSO_AUTH_TOKEN").toBuildConfigLiteral()
        buildConfigField("String", "TURSO_DATABASE_URL", tursoUrl)
        buildConfigField("String", "TURSO_AUTH_TOKEN", tursoToken)

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

base {
    archivesName.set("AppNotas-v$appVersionName")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.google.material)
    implementation(libs.androidx.biometric)
    implementation("net.java.dev.jna:jna:5.17.0@aar")
}
