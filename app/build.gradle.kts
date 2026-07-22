import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

/**
 * Release signing configuration setup.
 *
 * Loads signing credentials from `key.properties` located in the root project directory.
 * If `key.properties` is missing, release builds fall back to unsigned mode with a warning log.
 */
val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists()

/**
 * Application Semantic Versioning (SemVer 2.0.0).
 *
 * - MAJOR: Breaking API or architecture changes.
 * - MINOR: Backward-compatible new features.
 * - PATCH: Backward-compatible bug fixes.
 */
val versionMajor = 0
val versionMinor = 1
val versionPatch = 0
val versionPreRelease = "Alpha01"
val appVersionName = "$versionMajor.$versionMinor.$versionPatch-$versionPreRelease"

plugins {
    // Android Application plugin
    alias(libs.plugins.android.application)

    // Kotlin Android support plugin
    alias(libs.plugins.kotlin.android)

    // Kotlin Compose compiler plugin (Kotlin 2.x+)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.appcustodian.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.appcustodian.app"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Enables code obfuscation, shrinking, and resource optimization for production builds
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Attach signing configuration if key.properties exists
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "⚠️ No key.properties found at project root — release build " +
                            "will be UNSIGNED. Play Protect and most devices will refuse " +
                            "to install an unsigned release APK."
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        // Enables Jetpack Compose UI toolchain
        compose = true
    }
}

kotlin {
    // Modern Kotlin compiler options configuration for JVM bytecode target
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    // Core AndroidX and Lifecycle dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Jetpack Compose dependencies
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Unit and Instrumentation Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debugging and Developer Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}