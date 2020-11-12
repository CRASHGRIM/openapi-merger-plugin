plugins {
    java
    maven
    `maven-publish`
}

val localRepository: String by project.extra

val mavenCliRuntime: Configuration by configurations.creating {
    isVisible = false
    isCanBeConsumed = false
    isCanBeResolved = true
}

repositories {
    mavenCentral()
}

val mavenVersion = "3.6.3"
val mavenPluginVersion = "3.6.0"
val eclipseAetherVersion = "1.1.0"
val mavenWagonVersion = "3.4.1"
val lombokVersion = "1.18.16"

dependencies {
    compileOnly(group = "org.projectlombok", name = "lombok", version = lombokVersion)
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = lombokVersion)

    implementation(project(":openapi-merger-plugin-models"))
    implementation(group = "org.apache.maven", name = "maven-core", version = mavenVersion)
    implementation(group = "org.apache.maven", name = "maven-plugin-api", version = mavenVersion)
    implementation(group = "org.apache.maven.plugin-tools", name = "maven-plugin-annotations", version = mavenPluginVersion)

    mavenCliRuntime(project(":openapi-merger-plugin-models"))
    mavenCliRuntime(group = "org.apache.maven", name = "maven-embedder", version = mavenVersion)
    mavenCliRuntime(group = "org.apache.maven", name = "maven-compat", version = mavenVersion)
    mavenCliRuntime(group = "org.slf4j", name = "slf4j-simple", version = "1.7.30")
    mavenCliRuntime(group = "org.eclipse.aether", name = "aether-connector-basic", version = eclipseAetherVersion)
    mavenCliRuntime(group = "org.eclipse.aether", name = "aether-transport-wagon", version = eclipseAetherVersion)
    mavenCliRuntime(group = "org.apache.maven.wagon", name = "wagon-http", version = mavenWagonVersion, classifier = "shaded")
    mavenCliRuntime(group = "org.apache.maven.wagon", name = "wagon-provider-api", version = mavenWagonVersion)


    testImplementation("junit", "junit", "4.12")
}

val publish by project(":openapi-merger-plugin-models").tasks.existing

val generatePluginDescriptor by tasks.registering(JavaExec::class) {
    dependsOn(publish)
    val settingsFile = sourceSets["main"].resources.srcDirs.first().path.plus("/settings.xml")
    val javaOutputDir = sourceSets["main"].java.classesDirectory
    val pomFile = "$buildDir/pom.xml"
    val pluginDescriptorFile = javaOutputDir.map {
        it.file("META-INF/maven/plugin.xml")
    }
    inputs.dir(sourceSets["main"].java.outputDir)
    outputs.file(pluginDescriptorFile)

    classpath = mavenCliRuntime
    main = "org.apache.maven.cli.MavenCli"
    systemProperties = mapOf("maven.multiModuleProjectDirectory" to projectDir, "localRepository" to localRepository)
    args = listOf(
            "--errors",
            "--batch-mode",
            "--settings", settingsFile,
            "--file", pomFile,
            "org.apache.maven.plugins:maven-plugin-plugin:$mavenPluginVersion:descriptor"
    )

    doFirst {
        maven.pom {
            packaging = "maven-plugin"
            withXml {
                asNode().appendNode("build").apply {
                    appendNode("directory", "$buildDir")
                    appendNode("outputDirectory", "${javaOutputDir.get()}")
                }
            }
        }.writeTo(pomFile)
        assert(file(pomFile).exists()) {
            "$pomFile was not generated. Check errors"
        }
        logger.info("POM is generated and placed at location $pomFile")
    }
    doLast {
        assert(pluginDescriptorFile.get().asFile.exists()) {
            "${pluginDescriptorFile.get()} was not generated. Check erros"
        }
        logger.info("Plugin descriptor file is generated and placed in ${pluginDescriptorFile.get().asFile.absoluteFile}")
    }
}

val jar by tasks.existing {
    dependsOn(generatePluginDescriptor)
}

publishing {
    publications {
        create<MavenPublication>("maven-plugin") {
            from(components["java"])
        }
    }
}