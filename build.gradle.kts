plugins {
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.3.2" apply false
    id("com.google.dagger.hilt.android") version "2.56.1" apply false
}

// updateDaemonJvm requires toolchain download repos (foojay) which is broken on Gradle 9.x.
// Android Studio triggers this task to pin the daemon JVM; disable it to avoid the failure.
// The daemon JVM is already pinned via gradle/gradle-daemon-jvm.properties.
tasks.configureEach {
    if (name == "updateDaemonJvm") enabled = false
}
