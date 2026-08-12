plugins {
    java
}

group = "de.nations"
version = "1.0.0"

description = "Blue lives system for Paper 1.21.11 using the Minecraft Nations resource pack heart glyphs."

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

jar {
    archiveBaseName.set("NationHearts")
}
