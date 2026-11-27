// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

val localBuildRoot = rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { stream ->
    java.util.Properties().apply { load(stream) }.getProperty("gminder.buildRoot")
}
localBuildRoot?.takeIf { it.isNotBlank() }?.let { outputRoot ->
    allprojects { layout.buildDirectory.set(file(outputRoot).resolve(name)) }
}
