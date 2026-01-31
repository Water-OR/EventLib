plugins {
    `java-library`
}

group = "net.llvg"
version = property("lib_version") as String
base.archivesName = "EventLib"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
    
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.jetbrainsAnnontation)
    compileOnly(libs.googleErrorProne)
    compileOnly(libs.jspecify)
    testCompileOnly(libs.jspecify)
    
    compileOnly(libs.lombok)
    testCompileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

testing {
    @Suppress("UnstableApiUsage")
    suites.getByName<JvmTestSuite>("test") {
        useJUnitJupiter()
    }
}