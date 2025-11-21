import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
}

// Helper function to safely load local.properties
fun getLocalProperty(key: String): String {
    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")

    if (localPropertiesFile.exists()) {
        try {
            properties.load(FileInputStream(localPropertiesFile))
        } catch (e: Exception) {
            println("Warning: Could not load local.properties")
        }
    }

    return properties.getProperty(key) ?: ""
}

android {
    namespace = "com.example.doirag"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.doirag"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- INJECT SECRETS INTO BUILDCONFIG ---
        // 1. Read values and TRIM whitespace
        val supabaseUrl = getLocalProperty("SUPABASE_URL").trim()
        val supabaseKey = getLocalProperty("SUPABASE_ANON_KEY").trim()

        // 2. Wrap in escaped quotes for Java String syntax: "value"
        //    If value is missing, default to empty string: ""
        val urlValue = if (supabaseUrl.isNotEmpty()) "\"$supabaseUrl\"" else "\"\""
        val keyValue = if (supabaseKey.isNotEmpty()) "\"$supabaseKey\"" else "\"\""

        // 3. Inject fields
        buildConfigField("String", "SUPABASE_URL", urlValue)
        buildConfigField("String", "SUPABASE_ANON_KEY", keyValue)
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")

    // Network
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // Core Android
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Material Design 3
    implementation("com.google.android.material:material:1.11.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}