plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.actimate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.actimate"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true  // For KTS, viewBinding is a direct property
    }
}


dependencies {
    implementation("androidx.compose.material:material-icons-extended:1.7.5") // Usa la versione più recente
    implementation("androidx.compose.material3:material3:1.3.1") // Usa la versione di Material 3
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("com.google.android.material:material:1.11.0")   // or 1.12.0-alpha03
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.6.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.google.android.gms:play-services-fitness:21.1.0")
    implementation("com.google.android.gms:play-services-auth:20.6.0")
    implementation("com.airbnb.android:lottie:5.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
    // Add these dependencies to your app-level build.gradle
    implementation(libs.androidx.adapters)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.fragment.ktx) // O l'ultima versione disponibile
        // debugImplementation because LeakCanary should only run in debug builds.
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
    implementation ("com.google.code.gson:gson:2.10.1") // Aggiungi questa dipendenza
    implementation ("androidx.work:work-runtime:2.10.0")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("androidx.compose.ui:ui:1.5.1") // Use the latest version
    implementation ("androidx.compose.foundation:foundation:1.5.1")
    implementation ("org.apache.commons:commons-math3:3.6.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("androidx.compose.ui:ui-viewbinding:1.5.15")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.0")  // or the latest version
    implementation ("androidx.navigation:navigation-ui-ktx:2.7.0")
    implementation ("androidx.compose.material3:material3:<latest_version>")
    implementation ("com.google.android.material:material:1.7.0")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.0-rc1")






    dependencies {
        val room_version = "2.6.1"

        implementation("androidx.room:room-runtime:$room_version")

        // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
        // See Add the KSP plugin to your project
        ksp("androidx.room:room-compiler:$room_version")

        // If this project only uses Java source, use the Java annotationProcessor
        // No additional plugins are necessary
        annotationProcessor("androidx.room:room-compiler:$room_version")

        // optional - Kotlin Extensions and Coroutines support for Room
        implementation("androidx.room:room-ktx:$room_version")

        // optional - RxJava2 support for Room
        implementation("androidx.room:room-rxjava2:$room_version")

        // optional - RxJava3 support for Room
        implementation("androidx.room:room-rxjava3:$room_version")

        // optional - Guava support for Room, including Optional and ListenableFuture
        implementation("androidx.room:room-guava:$room_version")

        // optional - Test helpers
        testImplementation("androidx.room:room-testing:$room_version")

        // optional - Paging 3 Integration
        implementation("androidx.room:room-paging:$room_version")



    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.runtime.livedata)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}