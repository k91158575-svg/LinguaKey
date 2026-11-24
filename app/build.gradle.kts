plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.linguakey"
 compileSdk = 35
 defaultConfig { applicationId = "com.linguakey"; minSdk = 23; targetSdk = 35; versionCode = 1; versionName = "0.1" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation("com.google.mlkit:translate:17.0.3") }