import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
  alias(libs.plugins.room3)
}

// Release signing is read from keystore.properties (kept out of VCS) or, on CI, from env vars.
val keystoreProperties =
  Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
  }

fun signingValue(key: String, env: String): String? =
  keystoreProperties.getProperty(key) ?: System.getenv(env)

android {
  namespace = "com.lentikr.reminder"
  compileSdk = 37

  defaultConfig {
    // Separate from the 1.x app (com.lentikr.reminder), so the two install side by side.
    applicationId = "app.lentikr.reminder"
    minSdk = 29
    targetSdk = 37
    versionCode = 21
    versionName = "2.0.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    val storeFilePath = signingValue("storeFile", "REMINDER_STORE_FILE")
    if (storeFilePath != null) {
      create("release") {
        storeFile = rootProject.file(storeFilePath)
        storePassword = signingValue("storePassword", "REMINDER_STORE_PASSWORD")
        keyAlias = signingValue("keyAlias", "REMINDER_KEY_ALIAS")
        keyPassword = signingValue("keyPassword", "REMINDER_KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    debug {
      // Lets a debug build sit next to the release build on the same device.
      applicationIdSuffix = ".debug"
    }
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release")
    }
  }

  // One APK for arm64 phones plus a universal one for everything else (emulators, older devices).
  splits {
    abi {
      isEnable = true
      reset()
      include("arm64-v8a")
      isUniversalApk = true
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
    aidl = false
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
  compilerOptions {
    optIn.addAll(
      "androidx.compose.material3.ExperimentalMaterial3Api",
      "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
      "androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
    )
    // Opt in to the upcoming default: annotations on constructor properties (e.g. @DrawableRes,
    // @ApplicationContext) go to both the parameter and the field. See KT-73255.
    freeCompilerArgs.add("-Xannotation-default-target=param-property")
  }
}

room3 {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material3.adaptive.navigation3)
  debugImplementation(libs.androidx.compose.ui.tooling)

  // Navigation 3
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // DI
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

  // Persistence
  implementation(libs.androidx.room3.runtime)
  ksp(libs.androidx.room3.compiler)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.serialization.json)

  // Chinese lunar calendar
  implementation(libs.tyme)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.kotlinx.coroutines.test)
}
