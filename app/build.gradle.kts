plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.bancodelechematerna"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.bancodelechematerna"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            // Misma clave que debug: asi la release se instala encima de la version que ya
            // tiene el telefono sin desinstalar, y no se pierde la base de datos.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    lint {
        // El lint de las release pide material-icons-core-desktop:1.7.8, que no esta
        // publicado, y hace fallar el build. Se puede seguir corriendo a mano con :app:lint.
        checkReleaseBuilds = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}