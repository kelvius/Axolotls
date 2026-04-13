---
name: build-deployment
description: Gradle build configuration and Android deployment
---

# Build & Deployment

## Tech Stack
- Kotlin DSL (build.gradle.kts)
- Version Catalog (libs.versions.toml)
- Android Gradle Plugin 8.x
- Kotlin 1.9.x

## Required Patterns

### Version Catalog (libs.versions.toml)

```toml
[versions]
agp = "8.2.0"
kotlin = "1.9.21"
compose-bom = "2024.02.00"
koin = "3.5.3"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "1.12.0" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
koin-android = { group = "io.insert-koin", name = "koin-android", version.ref = "koin" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

### build.gradle.kts (App)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.axolotls"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.axolotls"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
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
        debug {
            isDebuggable = true
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.7"
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui)
    implementation(libs.koin.android)
}
```

## Prohibited Patterns

1. **Never commit API keys** - use `local.properties` and `BuildConfig`
2. **Don't use dynamic versions** (e.g., `1.+.+`) - always use fixed versions in catalog
3. **Avoid `compile`** - use `implementation` or `api`
4. **Don't disable minification** in release builds

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease

# Build with dependencies report
./gradlew dependencies > dependencies.txt

# Clean and rebuild
./gradlew clean assembleDebug
```

## Signing

Add to `local.properties`:
```properties
storeFile=keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Reference in `build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("${project.rootDir}/keystore.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```

## CI/CD

- Use `./gradlew lintDebug` before merging
- Run `./gradlew testDebugUnitTest` in CI
- Use `--stacktrace` for debugging failed builds
