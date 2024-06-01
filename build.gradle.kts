plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
}

group = "org.flmelody"
version = "1.0.2-spring5"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.guava)
    compileOnly(libs.spring.boot.web)
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

tasks.jar {
    enabled = true
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Compiled-Spring-Boot-Version" to libs.versions.spring.boot.version.get()
            )
        )
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "org.flmelody"
            artifactId = "spring-method-parameter-resolver"
            this.version = version
            from(components["java"])

            pom {
                name.set("spring-method-parameter-resolver")
                description.set("Enhanced spring parameter binding extensions")
                url.set("https://github.com/Flmelody/spring-method-parameter-resolver")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("esotericman")
                        name.set("esotericman")
                    }
                }
                scm {
                    connection.set("scm:git:git:github.com/Flmelody/spring-method-parameter-resolver.git")
                    developerConnection.set("scm:git:ssh://github.com/Flmelody/spring-method-parameter-resolver.git")
                    url.set("https://github.com/Flmelody/spring-method-parameter-resolver.git")
                }
            }
        }
    }
    repositories {
        maven {
            name = "OSSRH"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = project.properties["username"] as String?
                password = project.properties["password"] as String?
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as? StandardJavadocDocletOptions)?.addBooleanOption("html5", true)
    }
}