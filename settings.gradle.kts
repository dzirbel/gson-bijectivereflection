import java.util.Properties

rootProject.name = "gson-bijectivereflection"

gradle.beforeProject {
    val secretsFile = rootDir.resolve("secrets.properties")
    if (secretsFile.exists()) {
        val secrets = secretsFile.reader().use { reader -> Properties().apply { load(reader) } }
        secrets.forEach { key, value -> project.extra.set(key as String, value) }
        logger.lifecycle("Applied ${secrets.size} properties from $secretsFile")
    }
}
