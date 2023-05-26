plugins {
    kotlin("jvm") version "1.7.22"
    application
}

application {
    mainClass.set("org.example.SignatureStatusKt")
}

group = "org.example"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation("commons-cli:commons-cli:1.5.0")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-math3:3.6.1")

    implementation("com.docusign:docusign-esign-java:4.3.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    implementation("org.glassfish.jersey.core:jersey-client:3.1.2")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:3.1.2")
    implementation("org.glassfish.jersey.media:jersey-media-multipart:3.1.2")
    implementation("org.apache.oltu.oauth2:org.apache.oltu.oauth2.client:1.0.2")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.73")
}
