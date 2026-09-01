plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.lxj.mfa"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lxj.mfa"
        minSdk = 26
        targetSdk = 34
        versionCode = 26
        versionName = "1.0.26"
    }

    buildTypes {
        release {
            // 复用 AGP 内置 debug 签名，保证各版本签名一致、可覆盖升级
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

// 产物按版本号命名，去掉 debug/release 字样
// （AGP 8 的新 VariantOutput 已移除 outputFileName，故用 assemble 后的重命名任务实现）
listOf("Debug" to "", "Release" to "-release").forEach { (variant, suffix) ->
    val low = variant.lowercase()
    tasks.register("rename${variant}Apk") {
        doLast {
            val v = android.defaultConfig.versionName ?: "1.0.0"
            val outDir = layout.buildDirectory.dir("outputs/apk/$low").get().asFile
            val src = File(outDir, "app-$low.apk")
            val dst = File(outDir, "LXJ-MFA-v${v}${suffix}.apk")
            if (src.exists()) {
                if (dst.exists()) dst.delete()
                check(src.renameTo(dst)) { "重命名 $src 失败" }
            }
        }
    }
}

afterEvaluate {
    listOf("Debug", "Release").forEach { variant ->
        tasks.named("assemble$variant") { finalizedBy("rename${variant}Apk") }
    }
}

dependencies {
    // AndroidX 基础
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 生物识别（指纹 / 设备凭据）
    implementation("androidx.biometric:biometric:1.1.0")

    // 本地加密存储：Keystore 密钥 + EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.0.0")

    // Room 持久化
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Git 同步（JGit，HTTPS + Token）
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")

    // 扫码（zxing-android-embedded，自带相机扫码 Activity）
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Java 8+ API 脱糖（JGit 依赖部分 Java 8 类）
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
