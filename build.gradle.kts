import org.gradle.internal.os.OperatingSystem
import io.papermc.hangarpublishplugin.model.Platforms
import java.io.ByteArrayOutputStream

plugins {
    `java-library`
    `maven-publish`
    idea
//    id("io.papermc.paperweight.userdev") version "1.7.7"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
    id("com.diffplug.spotless") version "6.25.0"
    id("com.gradleup.shadow") version "9.0.0-beta4"
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
    implementation("org.reflections:reflections:0.10.2")
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

tasks.shadowJar {
    dependsOn(tasks.jar)
}

tasks.register<Jar>("sourcesJar") {
    dependsOn(tasks.shadowJar)
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("")
    mergeServiceFiles()
    relocate("org.reflections", "cn.noy.cui.shadow.org.reflections")
}

tasks.build {
    dependsOn(tasks.spotlessApply, tasks.shadowJar)
}

tasks.register("buildAndCopy") {
    dependsOn(tasks.build)
    finalizedBy("copyJarToServerPlugins")
}

tasks.register("bac") {
    dependsOn("buildAndCopy")
}

tasks.test {
    dependsOn(tasks.spotlessApply)
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
            url = uri("https://maven.pkg.github.com/PolyVoxel/ChestUI")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }
        }
    }
}

val versionString: String = version as String
val isRelease: Boolean = !versionString.contains('-')
val suffixedVersion: String = if (isRelease) {
    versionString
} else {
    // Give the version a unique name by using the GitHub Actions run number
    versionString + "+" + System.getenv("GITHUB_RUN_NUMBER")
}

// Helper methods
fun executeGitCommand(vararg command: String): String {
    val byteOut = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", *command)
        standardOutput = byteOut
    }
    return byteOut.toString(Charsets.UTF_8.name()).trim()
}

fun latestCommitMessage(): String {
    return executeGitCommand("log", "-1", "--pretty=%B")
}
// Use the commit description for the changelog
val changelogContent: String = latestCommitMessage()

hangarPublish {
    publications.register("plugin") {
        version.set(suffixedVersion)
        channel.set(if (isRelease) "Release" else "Snapshot")
        changelog.set(changelogContent)
        id.set("ChestUI")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        platforms {
            register(Platforms.PAPER) {
                // Set the JAR file to upload
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })

                // Set platform versions from gradle.properties file
                val versions: List<String> = (property("paperVersion") as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
            }
        }
    }
}