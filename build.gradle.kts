plugins {
    kotlin("multiplatform") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
    id("maven-publish")
}

group = "tl.jake.ktree"
version = "0.0.0-SNAPSHOT"
val entryPackage = "tl.jake.ktree.cli"

object RunTaskConfig {
    val stdin = "".byteInputStream()
    val args = arrayOf("examples/hash-bang.treenotation")
}

repositories {
    mavenCentral()
}

publishing {
    repositories {
        maven {
            mavenCentral()
        }
    }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }

        // TODO: is this needed?
        withJava()

        // Create a JVM jar
        val jvmJar by tasks.getting(org.gradle.jvm.tasks.Jar::class) {
            doFirst {
                manifest {
                    attributes["Main-Class"] = "$entryPackage.MainKt"
                }

                // TODO: is this needed?
                from(configurations.getByName("runtimeClasspath").map { if (it.isDirectory) it else zipTree(it) })
            }
        }

        val runJvmJar by tasks.creating(JavaExec::class) {
            description = "Run the JVM jar main class"
            dependsOn(jvmJar)
            group = "run"
            main = "-jar"
            args(jvmJar.archiveFile.get(), *RunTaskConfig.args)
            standardInput = RunTaskConfig.stdin
        }
    }
    js(IR) {
        useCommonJs()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.binaries {
        executable("ktree") {
            entryPoint = "$entryPackage.main"
            runTask?.run {
                standardInput = RunTaskConfig.stdin
                args(*RunTaskConfig.args)
            }
        }
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
            }
        }
        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val nativeMain by getting
        val nativeTest by getting
    }
}
