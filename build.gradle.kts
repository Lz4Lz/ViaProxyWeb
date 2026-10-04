plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.6.1"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven {
        name = "ViaVersion"
        url = uri("https://repo.viaversion.com")
    }
}

dependencies {
    compileOnly("net.raphimc:ViaProxy:3.4.12") {
        isTransitive = false
    }

    implementation("io.javalin:javalin:7.2.3")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.2")
    compileOnly("net.raphimc:MinecraftAuth:5.0.2")
    implementation("net.lenni0451:optconfig:1.1.1")
}


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}


tasks.register<JavaExec>("runViaProxy") {
    description = "Runs ViaProxy"
    dependsOn(tasks.jar)

    mainClass.set("net.raphimc.viaproxy.ViaProxy")
    classpath = sourceSets["main"].compileClasspath + files(tasks.jar.get().archiveFile)
    workingDir = file("run")
    jvmArgs = listOf("-DskipUpdateCheck")
    //args = listOf("config", "viaproxy.yml")

    doFirst {
        val pluginsDir = file("$workingDir/plugins")
        pluginsDir.mkdirs()
        file("$pluginsDir/${project.name}.jar").writeBytes(
            tasks.jar.get().archiveFile.get().asFile.readBytes()
        )
    }

    doLast {
        file("$workingDir/plugins/${project.name}.jar").delete()
        file("$workingDir/logs").deleteRecursively()
    }
}