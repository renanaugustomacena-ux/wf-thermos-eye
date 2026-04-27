plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

ksp {
    // Room exports schema JSON snapshots committed to schemas/ in version
    // control. Future schema bumps must add a Migration object validated via
    // MigrationTestHelper; fallbackToDestructiveMigration is the v0.x policy
    // until v1 ships.
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.alexcupsa.wifithermal.core.database"
    compileSdk = 35

    defaultConfig {
        minSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core:model"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.coroutines.android)
}
