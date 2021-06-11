plugins {
    java
    kotlin("jvm") version "1.5.0-RC"
}


group = "org.dice_group"
version = "1.0-SNAPSHOT"

description = "Glisten Core library"


repositories {
    mavenCentral()
}


dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("junit", "junit", "4.13.1")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson", "gson", "2.8.7")
}

