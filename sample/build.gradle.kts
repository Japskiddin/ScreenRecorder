plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
}

android {
  namespace = "io.github.japskiddin.sample"
  compileSdk = libs.versions.androidSdk.compile.get().toInt()
  defaultConfig {
    applicationId = "io.github.japskiddin.screenrecorder.sample"
    minSdk = libs.versions.androidSdk.min.get().toInt()
    targetSdk = libs.versions.androidSdk.target.get().toInt()
    versionCode = 1
    versionName = "1.0.0"
    vectorDrawables {
      useSupportLibrary = true
    }
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    val release by getting {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  packaging {
    jniLibs {
      excludes += listOf(
        "**/kotlin/**",
        "META-INF/androidx.*",
        "META-INF/proguard/androidx-*"
      )
    }
    resources {
      excludes += listOf(
        "/META-INF/*.kotlin_module",
        "**/kotlin/**",
        "**/*.txt",
        "**/*.xml",
        "**/*.properties",
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE",
        "META-INF/LICENSE.txt",
        "META-INF/license.txt",
        "META-INF/NOTICE",
        "META-INF/NOTICE.txt",
        "META-INF/notice.txt",
        "META-INF/ASL2.0",
        "META-INF/*.version",
        "META-INF/androidx.*",
        "META-INF/proguard/androidx-*"
      )
    }
  }

  bundle {
    language {
      enableSplit = false
    }
  }

  buildFeatures {
    viewBinding = true
    buildConfig = true
  }

  lint {
    abortOnError = false
  }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  kotlinOptions {
    jvmTarget = "21"
  }
}

tasks.withType<JavaCompile> {
  val compilerArgs = options.compilerArgs
  compilerArgs.addAll(
    listOf(
      "-Xlint:unchecked",
      "-Xlint:deprecation"
    )
  )
}

dependencies {
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.core)
  implementation(libs.androidx.core.ktx)

  implementation(libs.androidx.navigation.fragment.ktx)
  implementation(libs.androidx.navigation.ui.ktx)

  implementation(libs.material)

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.espresso.core)

  implementation(project(":screenrecorder"))
}
