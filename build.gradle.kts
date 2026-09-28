plugins {
    application
}

group = "io.github.acm363"
version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

application {
    mainClass = "boardgame.app.Main"
}

tasks.jar {
    archiveFileName = "tile-game.jar"
    manifest {
        attributes("Main-Class" to application.mainClass)
    }
}

tasks.test {
    useJUnitPlatform()
}
