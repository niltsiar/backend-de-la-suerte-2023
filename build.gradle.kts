@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    application
    id(libs.plugins.kotlin.jvm.pluginId)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ktor)
}

application {
    mainClass by "dev.niltsiar.luckybackend.MainKt"
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "${JavaVersion.VERSION_11}"
        }
    }
}

dependencies {
    implementation(libs.bundles.ktor.server)
    implementation(libs.logback.classic)
    implementation(libs.bundles.arrow)
    implementation(libs.bundles.suspendapp)
    implementation("org.fusesource.jansi:jansi:2.4.0")

}
