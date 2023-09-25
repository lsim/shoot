import io.github.oshai.kotlinlogging.KotlinLogging
import org.ini4j.Ini
import org.ini4j.IniPreferences
import java.io.File
import java.util.prefs.Preferences

class ShootPreferences(val testing: Boolean = false) {
    private val logger = KotlinLogging.logger {}

    val preferencesFile = File("shoot.ini")
    val ini: Ini
    val preferences: Preferences
    init {
        if (!preferencesFile.exists() && !testing) {
            preferencesFile.createNewFile()
        }
        logger.info { "Loading preferences from ${preferencesFile.absolutePath}" }
        ini = if (!testing) Ini(preferencesFile) else Ini()
        preferences = IniPreferences(ini).node("shoot")
    }

    val user get() = System.getProperty("user.name")
    val hostname get() = System.getenv("COMPUTERNAME") ?: System.getenv("HOSTNAME") ?: "unknown"

    operator fun get(key: String, def: String): String {
        return preferences[key, def]
    }

    operator fun set(key: String, value: String) {
        preferences.put(key, value)
        preferences.sync()
        ini.store()
    }
}
