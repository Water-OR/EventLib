plugins {
    alias(libs.plugins.freefair.lombok)
    `java-library`
}

val libVersion = run<_, String> {
    fun git(vararg args: String): String? = runCatching {
        val process = ProcessBuilder("git", *args)
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .start()
        
        val output = process.inputStream.bufferedReader().readText().trim()
        if (process.waitFor() != 0 || output.isEmpty()) null else output
    }.getOrNull()
    
    var result = git("describe", "--tags", "--exact-match")
    if (result != null) return@run result.removePrefix("v")
    
    result = git("rev-parse", "--short", "HEAD")
    if (result == null) return@run "unknown"
    
    "$result-SNAPSHOT"
}

group = "net.llvg"
version = libVersion
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