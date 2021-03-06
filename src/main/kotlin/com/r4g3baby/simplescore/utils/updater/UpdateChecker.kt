package com.r4g3baby.simplescore.utils.updater

import org.bukkit.plugin.Plugin
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.function.BiConsumer

class UpdateChecker(plugin: Plugin, pluginID: Int, consumer: BiConsumer<Boolean, String>) {
    private val _spigotApi = "https://api.spigotmc.org/legacy/update.php?resource=$pluginID"

    init {
        plugin.server.scheduler.runTaskAsynchronously(plugin) {
            try {
                val conn = URL(_spigotApi).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.useCaches = false
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "${plugin.name}/${plugin.description.version}")

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val latestVersion = reader.readText()
                val currentVersion = plugin.description.version

                if (currentVersion.replace(".", "").toInt() < latestVersion.replace(".", "").toInt()) {
                    consumer.accept(true, latestVersion)
                } else consumer.accept(false, latestVersion)

                reader.close()
                conn.disconnect()
            } catch (ignored: Exception) {
                consumer.accept(false, "")
            }
        }
    }
}