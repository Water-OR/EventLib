plugins {
    alias(libs.plugins.freefair.lombok)
    `java-library`
}

group = "net.llvg"
version = property("lib_version") as String
base.archivesName = "EventLib"

lombok {
    version = libs.versions.lombok
}

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
}

testing {
    @Suppress("UnstableApiUsage")
    suites.getByName<JvmTestSuite>("test") {
        useJUnitJupiter()
    }
}


tasks {
    withType<Javadoc> {
        options.encoding = "UTF-8"
        options.jFlags("-Dfile.encoding=UTF-8")
        exclude("**/impl/**")
    }
    
    named<Jar>("sourcesJar") {
        val excluded = sourceSets.main.get().java.srcDirs.toTypedArray()
        
        exclude {
            excluded.any { dir -> it.file.startsWith(dir) }
        }
        
        from(delombok.get().outputs)
    }
    
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}