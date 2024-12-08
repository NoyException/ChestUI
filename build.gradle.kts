plugins {
    `java-library`
    `maven-publish`
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

spotless {
    java {
        eclipse()
    }
}

tasks.register<Exec>("setupServer") {
    workingDir = file("./devtools")
    executable = "bash"
    args("./setup-server.sh")
    standardOutput = System.out
    errorOutput = System.err
}

tasks.register<Exec>("runServer") {
    dependsOn("setupServer")
    workingDir = file("./server")
    executable = "bash"
    args("./run.sh")
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