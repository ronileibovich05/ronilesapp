plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.ronilesapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ronilesapp"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}



dependencies {
    // Firebase BOM – קובע גרסאות תואמות לכל המודולים
    implementation(platform("com.google.firebase:firebase-bom:32.2.2"))

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.google.firebase:firebase-auth")       // אין גרסה
    implementation("com.google.firebase:firebase-firestore")   // אין גרסה
    implementation("com.google.firebase:firebase-database")    // אין גרסה
    implementation("com.google.firebase:firebase-storage")     // אין גרסה
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
