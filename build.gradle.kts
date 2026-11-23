plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

allprojects {
    group = "no.kodastore"
    version = "0.1.0-SNAPSHOT"
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    // CVE patches newer than the Spring Boot 3.5.16 BOM; delete each line once the BOM catches up.
    extra["tomcat.version"] = "10.1.62"
    extra["jackson-bom.version"] = "2.21.8"
    extra["postgresql.version"] = "42.7.15"

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
