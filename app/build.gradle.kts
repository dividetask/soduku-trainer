plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.dividetask.sudokutrainer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dividetask.sudokutrainer"
        minSdk = 26
        targetSdk = 35
        // versionCode: timestamp-based, always increasing across branches.
        // versionName: predicted APK filename (vMAJOR.MINOR.apk = newest committed
        // APK minor + 1), a per-commit letter (a, b, c… for each commit pushed
        // before a new APK lands; resets to 'a' once a new APK is built), and the
        // 4-char hash of the commit Gradle built from. The first two are set by
        // hand; the hash is added at build time because a commit can't contain
        // its own hash.
        versionCode = (System.currentTimeMillis() / 60000).toInt()
        val versionBase = "v2.016 b"
        val commitHash = try {
            val process = Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short=4", "HEAD"))
            process.inputStream.bufferedReader().readText().trim().ifEmpty { "unknown" }
        } catch (_: Exception) { "unknown" }
        versionName = "$versionBase ($commitHash)"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)

    debugImplementation(libs.compose.ui.tooling)
}
