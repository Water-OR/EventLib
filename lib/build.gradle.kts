plugins {
    alias(libs.plugins.freefair.lombok)
    `java-library`
}

val libVersion: Provider<String> = run {
    fun git(vararg args: String): Provider<out String> {
        val result = providers.exec {
            executable("git")
            args(*args)
            isIgnoreExitValue = true
        }
        
        @Suppress("UnstableApiUsage")
        return result.result
          .filter { it.exitValue == 0 }
          .flatMap { result.standardOutput.asText }
    }
    
    val tag = git("describe", "--tags", "--exact-match")
      .map { it.trim().removePrefix("v") }
    val hash = git("rev-parse", "--short", "HEAD")
      .map { it.trim() }
    val dirty = git("status", "--porcelain")
      .map { it.trim().isNotEmpty() }
    
    tag.orElse(
        hash
          .zip(dirty.orElse(false)) { it, dirty ->
              if (dirty) "$it-modified" else it
          }
          .map { "$it-SNAPSHOT" }
          .orElse("unknown")
    )
}

group = "net.llvg"
version = libVersion.get()
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