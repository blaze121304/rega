plugins {
    kotlin("jvm") version "2.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    main {
        kotlin.srcDirs("src")
    }
    test {
        kotlin.srcDirs("test")
    }
}

tasks.test {
    useJUnitPlatform()
}
