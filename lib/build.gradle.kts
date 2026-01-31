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
    compileOnly(libs.bundles.annontations)
    testCompileOnly(libs.bundles.annontations)
    
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