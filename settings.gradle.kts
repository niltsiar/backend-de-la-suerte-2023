rootProject.name = "luckybackend-2023"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }

    repositories {
        mavenCentral()
    }
}
