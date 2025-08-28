plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.mdphotos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mdphotos"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildToolsVersion = "36.0.0"
}


dependencies {

    val cameraxVersion = "1.4.2"

    implementation ("androidx.camera:camera-core:${cameraxVersion}")
    implementation ("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation ("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation ("androidx.camera:camera-video:${cameraxVersion}")
    implementation ("androidx.camera:camera-view:${cameraxVersion}")
    implementation ("androidx.camera:camera-extensions:${cameraxVersion}")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

}