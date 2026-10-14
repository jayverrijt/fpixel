plugins {
    `java-library`
}

group = "net.fpixel"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.181-stable")
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.named<Jar>("jar") {
    archiveFileName.set("FPixelCore.jar")
    destinationDirectory.set(file("${rootDir}/../server/plugins"))
}
