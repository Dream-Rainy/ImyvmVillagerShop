import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("fabric-loom")
	id("maven-publish")
	id("org.jetbrains.kotlin.jvm")
	kotlin("plugin.serialization") version "1.8.20"
}

operator fun Project.get(property: String): String {
	return property(property) as String
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

version = project["mod_version"]
group = project["maven_group"]

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	// See https://docs.gradle.org/current/userguide/declaring_repositories.html
	// for more information about repositories.

	mavenCentral()
	// Imyvm Hoki
	maven {
		url = uri("https://maven.victorica.dev")
	}

	maven {
		url = uri("https://maven.pkg.github.com/ImyvmCircle/ImyvmEconomy")
		credentials {
			username = System.getenv("GITHUB_USERNAME")
			password = System.getenv("GITHUB_TOKEN")
		}
	}

	maven {
		url = uri("https://maven.pkg.github.com/ImyvmCircle/HoKi")
		credentials {
			username = System.getenv("GITHUB_USERNAME")
			password = System.getenv("GITHUB_TOKEN")
		}
	}

	maven {
		name = "JP Mods"
		url = uri("https://maven.jpcode.dev")
	}

	// Fabric Permissions Api
	maven {
		url = uri("https://oss.sonatype.org/content/repositories/snapshots")
	}

	maven {
		url = uri("https://maven.nucleoid.xyz")
	}
	mavenLocal()

}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${project["minecraft_version"]}")
	mappings("net.fabricmc:yarn:${project["yarn_mappings"]}:v2")
	modImplementation("net.fabricmc:fabric-loader:${project["loader_version"]}")


	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation("net.fabricmc.fabric-api:fabric-api:${project["fabric_version"]}")
	modImplementation("com.mojang:brigadier:${project["brigadier_version"]}")

	modImplementation("com.imyvm:imyvm-hoki:${project["imyvm_hoki_version"]}")
	modImplementation("com.imyvm:imyvm-economy:${project["imyvm_economy_version"]}")

	modImplementation("net.fabricmc:fabric-language-kotlin:${project["fabric_kotlin_version"]}")
	modImplementation("me.lucko:fabric-permissions-api:${project["fabric_permissions_api_version"]}")
	modImplementation("eu.pb4:sgui:${project["sgui_version"]}")
	implementation("org.jetbrains.exposed:exposed-core:${project["exposed_version"]}")
	implementation("org.jetbrains.exposed:exposed-dao:${project["exposed_version"]}")
	implementation("org.jetbrains.exposed:exposed-jdbc:${project["exposed_version"]}")
	include("org.jetbrains.exposed:exposed-core:${project["exposed_version"]}")
	include("org.jetbrains.exposed:exposed-dao:${project["exposed_version"]}")
	include("org.jetbrains.exposed:exposed-jdbc:${project["exposed_version"]}")
	include("com.mysql:mysql-connector-j:${project["mysql-connector_version"]}")
	implementation("com.zaxxer:HikariCP:${project["HikariCP_version"]}")
	include("com.zaxxer:HikariCP:${project["HikariCP_version"]}")
//	implementation("com.impossibl.pgjdbc-ng", "pgjdbc-ng", "0.8.9")
	include("org.postgresql:postgresql:${project["postgresql_version"]}")
	include("org.xerial:sqlite-jdbc:${project["sqlite-jdbc_version"]}")
	include("com.microsoft.sqlserver:mssql-jdbc:${project["mssql-jdbc_version"]}")
	include("com.oracle.database.jdbc:ojdbc11:${project["ojdbc11_version"]}")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${project["kotlinx-serialization-json_version"]}")
	implementation("com.typesafe:config:${project["typesafe_version"]}")
	// Uncomment the following line to enable the deprecated Fabric API modules. 
	// These are included in the Fabric API production distribution and allow you to update your mod to the latest modules at a later more convenient time.

	// modImplementation "net.fabricmc.fabric-api:fabric-api-deprecated:${project.fabric_version}"
}

tasks.processResources {
	duplicatesStrategy = DuplicatesStrategy.INCLUDE

	inputs.property("version", project.version)

	from(sourceSets["main"].resources.srcDirs) {
		include("fabric.mod.json")
		expand(mutableMapOf("version" to project.version))
	}

	from(sourceSets["main"].resources.srcDirs) {
		exclude("fabric.mod.json")
	}
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.release.set(17)
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		jvmTarget = "17"
	}
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()
}

tasks.jar {
	from("LICENSE")
}

// configure the maven publication
