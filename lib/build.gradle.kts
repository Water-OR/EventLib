plugins {
    `java-library`
    alias(libs.plugins.shadow)
    `maven-publish`
}

group = "net.llvg"
version = property("lib_version") as String
base.archivesName = "EventLib"

java {
    toolchain {
        version = JavaLanguageVersion.of(8)
    }
    
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

val shadeImplementation by configurations.registering

configurations {
    implementation { extendsFrom(shadeImplementation.get()) }
}

dependencies {
    compileOnly(libs.jspecify)
    shadeImplementation(libs.guava)
    shadeImplementation(libs.bundles.asm)
}

testing {
    @Suppress("UnstableApiUsage")
    suites.getByName<JvmTestSuite>("test") {
        useJUnitJupiter()
    }
}

tasks {
    shadowJar {
        archiveClassifier = "relocated"
        configurations = listOf(shadeImplementation).map { it.get() }
        relocate("com.google", "net.llvg.eventlib.lib")
        relocate("org.objectweb", "net.llvg.eventlib.lib")
    }
}

publishing {
    repositories {
        mavenLocal()
    }
    
    publications {
        register<MavenPublication>("maven") {
            artifact(tasks["javadocJar"])
            artifact(tasks["sourcesJar"])
            artifact(tasks.shadowJar)
        }
    }
}