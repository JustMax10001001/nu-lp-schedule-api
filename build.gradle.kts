plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    `maven-publish`
}
group = "com.justsoft.nulpschedule"
version = "1.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jsoup:jsoup:1.16.1")

    testImplementation(platform("org.junit:junit-bom:5.7.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
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

tasks.withType<Test> {
    useJUnitPlatform()
}