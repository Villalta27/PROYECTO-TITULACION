
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Aplicamos el plugin de Google Services para conectar con Firebase
    id("com.google.gms.google-services")
}

android {
    namespace = "com.tudominio.voicefinance"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tudominio.voicefinance"
        minSdk = 24 // Requisito mínimo para funciones de voz estables
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // --- SECCIÓN DE FIREBASE ---
    // El BoM ayuda a que todas las librerías de Firebase sean compatibles entre sí
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // --- DEPENDENCIAS ANDROID (Catálogo libs.versions.toml) ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material) // Esta permite el Panel Lateral
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // --- LIBRERÍAS PARA EL DASHBOARD (Paso A) ---
    // Estas permiten crear el cuadro de gastos y la lista
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // --- PRUEBAS ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}