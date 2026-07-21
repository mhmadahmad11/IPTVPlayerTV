plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.iptvtv.player"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.iptvtv.player"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.leanback:leanback:1.0.0")

    // ExoPlayer (Media3) - يدعم HLS و MPEG-TS وهما الأكثر استخداماً في IPTV
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.3.1")
    implementation("androidx.media3:media3-datasource-rtmp:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // تحميل صور شعارات القنوات
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // تحليل JSON من Xtream Codes API
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines لعمليات الشبكة
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
}
