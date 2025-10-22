import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.animalclaasification"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.animalclaasification"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "BAIKE_API_KEY", "\"${properties.getProperty("BAIKE_API_KEY")}\"")
        buildConfigField("String", "ACCESS_TOKEN", "\"${properties.getProperty("ACCESS_TOKEN")}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // PyTorch Android 库
    implementation("org.pytorch:pytorch_android:1.10.0")  // 适用于 Android 的 PyTorch 库

    // Lifecycle 相关依赖，支持 ViewModel 和 LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1") // ViewModel 支持
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1") // LiveData 支持
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1") // LiveCycle 支持

    // retrofit和gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.jsoup:jsoup:1.15.2")

    implementation("org.pytorch:pytorch_android:1.10.0")  // PyTorch Android 库
    implementation("org.pytorch:pytorch_android_torchvision:1.10.0")  // 用于图像处理的库
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.1") // ONNX Runtime

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.android.material:material:1.12.0")

}