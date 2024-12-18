import org.gradle.internal.os.OperatingSystem

plugins {
    `java-library`
    `maven-publish`
    idea
//    id("io.papermc.paperweight.userdev") version "1.7.7"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "cn.noy"
version = "1.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
//    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")

    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.15.0")
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

spotless {
    java {
        eclipse()
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.register<Exec>("setupServer") {
    workingDir = file("./devtools")
    if (OperatingSystem.current().isWindows) {
        executable = "cmd"
        args("/c", "setup-server.bat")
    } else {
        executable = "bash"
        args("./setup-server.sh")
    }
    standardOutput = System.out
    errorOutput = System.err
}

tasks.register<Exec>("runServer") {
    dependsOn("setupServer")
    workingDir = file("./server")
    if (OperatingSystem.current().isWindows) {
        executable = "cmd"
        args("/c", "run.bat")
    } else {
        executable = "bash"
        args("./run.sh")
    }
    standardOutput = System.out
    errorOutput = System.err
}

tasks.register<Copy>("copyJarToServerPlugins") {
    dependsOn("setupServer")
    from(tasks.jar)
    into(file("./server/plugins"))
}

tasks.register("buildAndCopy") {
    dependsOn("build")
    finalizedBy("copyJarToServerPlugins")
}

tasks.register("bac") {
    dependsOn("buildAndCopy")
}

tasks.register<Jar>("sourcesJar") {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

tasks.build {
    dependsOn("spotlessApply")
}

tasks.test {
    dependsOn("spotlessApply")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks["sourcesJar"])
            groupId = "cn.noy"
            artifactId = "chest-ui"

            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/NoyException/ChestUI")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }
        }
    }
}