plugins {
    // Usamos versiones directas para evitar el error de "libs" no encontrado
    id("com.android.application") version "8.9.2"
    id("org.jetbrains.kotlin.android") version "1.9.0"
    id("com.google.gms.google-services") version "4.4.0"
}

android {
    namespace = "com.tudominio.voicefinance"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tudominio.voicefinance"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // --- SECCIÓN DE FIREBASE ---
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // --- DEPENDENCIAS ANDROID ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- LIBRERÍAS PARA EL DASHBOARD ---
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // --- LIBRERÍA PARA GESTIÓN DE IMÁGENES (GLIDE) ---
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // --- LIBRERÍA DE GRÁFICOS (MPAndroidChart) ---
    // ESTA ES LA QUE NECESITAMOS PARA LAS ESTADÍSTICAS:
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // --- PRUEBAS ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}