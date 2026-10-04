import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
fun String.toBuildConfigString(): String =
    this
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")

val footballApiBaseUrl = localProperties.getProperty(
    "footballApiBaseUrl",
    "https://api.pitchapi.dev/",
)
val pitchApiKey = localProperties.getProperty("footballApiToken").orEmpty()
val releaseStoreFile = localProperties.getProperty("releaseStoreFile").orEmpty()
val releaseStorePassword = localProperties.getProperty("releaseStorePassword").orEmpty()
val releaseKeyAlias = localProperties.getProperty("releaseKeyAlias").orEmpty()
val releaseKeyPassword = localProperties.getProperty("releaseKeyPassword").orEmpty()
val appVersionCode = providers.gradleProperty("appVersionCode")
    .map(String::toInt)
    .getOrElse(1)
val appVersionName = providers.gradleProperty("appVersionName")
    .orElse("1.0.0")
    .get()

android {
    namespace = "com.footballradar.app"
    compileSdk = 36
    val githubReleasesRepository = providers.gradleProperty("githubReleasesRepository").orElse("").get()

    defaultConfig {
        applicationId = "com.footballradar.app"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "PITCH_API_BASE_URL", "\"${footballApiBaseUrl.toBuildConfigString()}\"")
        buildConfigField("String", "PITCH_API_KEY", "\"${pitchApiKey.toBuildConfigString()}\"")
        buildConfigField("String", "GITHUB_RELEASES_REPOSITORY", "\"$githubReleasesRepository\"")
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(releaseStoreFile)
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
            storeType = "JKS"
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

tasks.register<Copy>("copyReleaseApk") {
    dependsOn("assembleRelease")
    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    into(layout.buildDirectory.dir("outputs/apk/release/distribution"))
    rename { "football-radar-v$appVersionName-release.apk" }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
