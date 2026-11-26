plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Pure Java, no Android, no transitive dependencies: Code 128 and QR
    // encoding is logic, so it belongs here where it can be tested with a JDK.
    // EAN-13 stays hand-written — see docs/DECISIONS.md.
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed")
    }
}
