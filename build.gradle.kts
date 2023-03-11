@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    application
    id(libs.plugins.kotlin.jvm.pluginId)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ktor)
}

application {
    mainClass by "dev.niltsiar.luckybackend,ApplicationKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.ktor.server)
    implementation(libs.logback.classic)
}
