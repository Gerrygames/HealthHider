import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("xyz.jpenilla.run-paper") version "2.3.0"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "me.noahvdaa.healthider"
version = "1.0.4"

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    implementation("org.bstats:bstats-bukkit:3.0.0")

    paperDevBundle("1.21-R0.1-SNAPSHOT")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(21)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    shadowJar {
        relocate("org.bstats", "me.noahvdaa.healthhider.libs.bstats")
    }
}

bukkit {
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    main = "me.noahvdaa.healthhider.HealthHider"
    description = "HealthHider allows you to hide the health of other entities, to prevent players from gaining an unfair advantage."
    apiVersion = "1.21"
    authors = listOf("NoahvdAa")
    website = "https://github.com/NoahvdAa/HealthHider"
    permissions.create("healthider.bypass") {
        default = BukkitPluginDescription.Permission.Default.FALSE
    }
}
