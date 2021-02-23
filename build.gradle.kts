import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    `maven-publish`
    maven
}
group = "com.justsoft.nulpschedule"
version = "1.0.6"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jsoup:jsoup:1.13.1")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("api") {
            artifactId = "api"
            groupId = project.group.toString()
            version = project.version.toString()
            from(components["java"])
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xallow-result-return-type"
}

tasks.withType<Test> {
    useJUnitPlatform()
}