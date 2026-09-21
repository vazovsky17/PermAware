import java.io.File
import java.io.StringReader
import java.util.Properties
val sharedTestSources = "src/sharedTest/java"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

val signKeystoreDir: Directory = rootProject.layout.projectDirectory.dir("signKeystore")

private val environmentInputNames = listOf(
    "PERMAWARE_KEYSTORE_PATH",
    "PERMAWARE_KEYSTORE_PASSWORD",
    "PERMAWARE_KEY_ALIAS",
    "PERMAWARE_KEY_PASSWORD",
)
private val propertyInputNames =
    listOf("RELEASE_STORE_FILE", "RELEASE_STORE_PASS", "RELEASE_ALIAS", "RELEASE_KEY_PASS")

private val keyPropertyValues: Map<String, String> =
    providers.fileContents(signKeystoreDir.file("key.properties")).asText.orNull
        ?.let { text -> Properties().apply { load(StringReader(text)) } }
        ?.entries
        ?.mapNotNull { (key, value) ->
            value.toString().trim().takeIf { it.isNotEmpty() }?.let { key.toString() to it }
        }
        ?.toMap()
        .orEmpty()
        .filterKeys { it in propertyInputNames }

private val environmentInputValues: Map<String, String> = environmentInputNames
    .mapNotNull { name ->
        providers.environmentVariable(name).orNull?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { name to it }
    }
    .toMap()

val signingFromEnvironment: Boolean = environmentInputValues.size == environmentInputNames.size
val signingFromProperties: Boolean = keyPropertyValues.size == propertyInputNames.size
val signingIsComplete: Boolean = signingFromEnvironment || signingFromProperties

val signKeystoreFile: File? = keyPropertyValues["RELEASE_STORE_FILE"]?.let { declared ->
    val named = File(declared)
    if (named.isAbsolute) named else signKeystoreDir.file(declared).asFile
}

if (signingFromEnvironment && signingFromProperties) {
    logger.warn(
        "signing: both sources are complete; using the environment. " +
            "signKeystore/key.properties is being ignored for this build.",
    )
}

android {
    namespace = "app.vazovsky.permaware"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.vazovsky.permaware"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "app.vazovsky.permaware.HiltTestRunner"
        vectorDrawables.useSupportLibrary = true

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.generateKotlin", "true")
        }

        fun urlProperty(name: String) = (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
        buildConfigField(
            "String",
            "BOOSTY_URL",
            urlProperty("permaware.boostyUrl")?.let { "\"$it\"" } ?: "null",
        )
        buildConfigField(
            "String",
            "GITHUB_URL",
            urlProperty("permaware.githubUrl")?.let { "\"$it\"" } ?: "null",
        )
        buildConfigField(
            "String",
            "CONTACT_URL",
            urlProperty("permaware.contactUrl")?.let { "\"$it\"" } ?: "null",
        )
    }

    androidResources {
        localeFilters += listOf("ru", "en")
    }

    if (signingIsComplete) {
        signingConfigs.create("release") {
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true

            if (signingFromEnvironment) {
                storeFile = file(environmentInputValues.getValue("PERMAWARE_KEYSTORE_PATH"))
                storePassword = environmentInputValues.getValue("PERMAWARE_KEYSTORE_PASSWORD")
                keyAlias = environmentInputValues.getValue("PERMAWARE_KEY_ALIAS")
                keyPassword = environmentInputValues.getValue("PERMAWARE_KEY_PASSWORD")
            } else {
                storeFile = signKeystoreFile
                storePassword = keyPropertyValues.getValue("RELEASE_STORE_PASS")
                keyAlias = keyPropertyValues.getValue("RELEASE_ALIAS")
                keyPassword = keyPropertyValues.getValue("RELEASE_KEY_PASS")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingIsComplete) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeCompiler {
        stabilityConfigurationFiles.add(
            rootProject.layout.projectDirectory.file("compose_stability.conf"),
        )
        if (project.hasProperty("compose.metrics")) {
            metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
            reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
        }
    }

    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
        getByName("test").java.srcDirs(sharedTestSources)
        getByName("test").kotlin.srcDirs(sharedTestSources)
        getByName("androidTest").java.srcDirs(sharedTestSources)
        getByName("androidTest").kotlin.srcDirs(sharedTestSources)
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkReleaseBuilds = true
        htmlReport = true
        xmlReport = true

        disable += setOf(
            "QueryAllPackagesPermission",
            "ObsoleteSdkInt",
            "InlinedApi",
            "DataExtractionRules",
            "OldTargetApi",
            "GradleDependency",
            "NewerVersionAvailable",
            "AndroidGradlePluginVersion",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp("androidx.hilt:hilt-compiler:${libs.versions.androidxHilt.get()}")

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.room.testing)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.work.testing)
    kspAndroidTest(libs.hilt.compiler)
}

val verifyReleaseSigning by tasks.registering {
    group = "verification"
    description = "Fails when a signing source is half-filled, or names a keystore that is not there."

    val environmentPresent = environmentInputValues.keys.sorted()
    val environmentMissing = environmentInputNames.filterNot { it in environmentInputValues }
    val propertiesPresent = keyPropertyValues.keys.sorted()
    val propertiesMissing = propertyInputNames.filterNot { it in keyPropertyValues }
    val keystorePath = signKeystoreFile?.takeIf { !signingFromEnvironment }?.absolutePath
    val propertiesComplete = signingFromProperties

    doLast {
        check(environmentPresent.isEmpty() || environmentMissing.isEmpty()) {
            "signing is half-configured in the environment: $environmentPresent set, " +
                "$environmentMissing missing. A release build would fall back to the debug key and " +
                "produce something installable under a key anyone can make."
        }
        check(propertiesPresent.isEmpty() || propertiesMissing.isEmpty()) {
            "signing is half-configured in signKeystore/key.properties: $propertiesPresent set, " +
                "$propertiesMissing missing or blank. Fill them in, or delete the file to build " +
                "unsigned — a release must not fall back to the debug key."
        }
        if (propertiesComplete && keystorePath != null) {
            check(File(keystorePath).isFile) {
                "signKeystore/key.properties names a keystore that is not there: $keystorePath. " +
                    "RELEASE_STORE_FILE is resolved inside signKeystore/ unless it is absolute."
            }
        }
    }
}

tasks.matching { it.name.startsWith("assemble") && it.name.contains("Release") }
    .configureEach { dependsOn(verifyReleaseSigning) }
tasks.matching { it.name.startsWith("bundle") && it.name.contains("Release") }
    .configureEach { dependsOn(verifyReleaseSigning) }
