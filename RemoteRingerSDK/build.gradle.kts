plugins {
    alias(libs.plugins.android.application)
//    id("com.android.library")  // Apply the library plugin directly // Apply the library plugin
//    kotlin("android")

}

//plugins {
//    id("com.android.library")
//    id("org.jetbrains.kotlin.android")
//}

android {
    namespace = "com.remoteringer"
    compileSdk = 35
//
    defaultConfig {
      applicationId = "com.remoteringer"
        minSdk = 26
        targetSdk = 35
       versionCode = 3
      versionName = "3.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
//
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
}

dependencies {

    implementation(libs.appcompat)
    implementation (libs.gson)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


}