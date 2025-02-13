import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("maven-publish")
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("jacoco")
    id("net.researchgate.release") version "3.0.2"
}

group = properties("pluginGroup")

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

intellijPlatform {
    pluginConfiguration {
        name = properties("pluginName")
        version = properties("version")
        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }
    mavenCentral()
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
    maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
    maven { url = uri("https://plugins.gradle.org/m2/") }
    gradlePluginPortal()
}

dependencies {
    intellijPlatform {
        instrumentationTools()

        if (project.hasProperty("useEap")) {
            println("Using EAP version")
            intellijIdeaUltimate("LATEST-EAP-SNAPSHOT")
        } else {
            println("Using target IDE version from gradle.properties")
            intellijIdeaUltimate(properties("platformVersion"))
        }

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
        properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty).forEach {
            bundledPlugin(it)
        }

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:2.17.2")
    implementation("org.antlr:antlr4-intellij-adaptor:0.1")
    implementation("org.apache.commons:commons-lang3:3.14.0")

    compileOnly("com.google.guava:guava:33.1.0-jre")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

tasks {
    test {
        useJUnitPlatform()
        if (project.hasProperty("useEap")) {
            println("Writing JUnit reports to ${layout.buildDirectory.dir("eap")}")
            reports {
                junitXml.outputLocation = layout.buildDirectory.dir("eap/xml")
                html.outputLocation = layout.buildDirectory.dir("eap/html")
            }
        }
    }

    "beforeReleaseBuild" {
        dependsOn(buildPlugin)
    }

    "afterReleaseBuild" {
        dependsOn(buildPlugin)
    }
}

release {
    failOnCommitNeeded = true
    failOnPublishNeeded = true
    failOnSnapshotDependencies = true
    failOnUnversionedFiles = true
    failOnUpdateNeeded = true
    revertOnFail = true
    preCommitText = ""
    preTagCommitMessage = "Release: pre tag commit: "
    tagCommitMessage = "Release: creating tag: "
    newVersionCommitMessage = "Release: new version commit: "
    tagTemplate = "\${version}"
    versionPropertyFile = "gradle.properties"
    versionProperties = listOf("version")
    snapshotSuffix = "-SNAPSHOT"

    git {
        requireBranch = ""
        pushToBranchPrefix = ""
        commitVersionFileOnly = false
        signTag = false
    }
}


