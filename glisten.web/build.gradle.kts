plugins {
    java
    kotlin("jvm") version "1.5.0-RC"
}

group = "org.dice_group"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("junit", "junit", "4.12")
}
