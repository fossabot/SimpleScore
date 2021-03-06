package com.r4g3baby.simplescore.scoreboard

import com.r4g3baby.simplescore.SimpleScore
import com.r4g3baby.simplescore.scoreboard.handlers.BukkitScoreboard
import com.r4g3baby.simplescore.scoreboard.handlers.ProtocolScoreboard
import com.r4g3baby.simplescore.scoreboard.handlers.ScoreboardHandler
import com.r4g3baby.simplescore.scoreboard.listeners.PlayersListener
import com.r4g3baby.simplescore.scoreboard.models.Scoreboard
import com.r4g3baby.simplescore.scoreboard.placeholders.ScoreboardExpansion
import com.r4g3baby.simplescore.scoreboard.tasks.ScoreboardRunnable
import org.bukkit.World
import org.bukkit.entity.Player
import java.io.*
import java.util.*
import java.util.logging.Level
import kotlin.collections.HashSet

class ScoreboardManager(private val plugin: SimpleScore) {
    private val _disabledDataFile = File(plugin.dataFolder, "data${File.separator}scoreboards")

    private val disabledScoreboards = HashSet<UUID>()
    private val scoreboardHandler: ScoreboardHandler

    init {
        plugin.server.pluginManager.registerEvents(PlayersListener(plugin), plugin)

        if (plugin.config.saveScoreboards) {
            plugin.logger.info("Loading disabled scoreboards...")

            try {
                if (_disabledDataFile.exists()) {
                    FileInputStream(_disabledDataFile).use { fis ->
                        ObjectInputStream(fis).use { ois ->
                            val content = ois.readObject()
                            if (content is ArrayList<*>) {
                                disabledScoreboards.addAll(content.filterIsInstance<UUID>())
                            }
                        }
                    }
                }

                plugin.logger.info("Disabled scoreboards loaded.")
            } catch (ex: IOException) {
                plugin.logger.log(Level.WARNING, "Error while loading disabled scoreboards", ex)
            }
        }

        if (plugin.placeholderAPI) {
            ScoreboardExpansion(plugin).register()
        }

        scoreboardHandler = if (!plugin.server.pluginManager.isPluginEnabled("ProtocolLib")) {
            BukkitScoreboard(plugin)
        } else ProtocolScoreboard()

        ScoreboardRunnable(plugin).runTaskTimerAsynchronously(plugin, 20L, 1L)
    }

    fun reload() {
        if (!plugin.config.saveScoreboards) {
            disabledScoreboards.clear()
        }
    }

    fun disable() {
        if (plugin.config.saveScoreboards) {
            plugin.logger.info("Saving disabled scoreboards...")

            try {
                if (!_disabledDataFile.parentFile.exists()) {
                    _disabledDataFile.parentFile.mkdirs()
                }
                if (!_disabledDataFile.exists()) {
                    _disabledDataFile.createNewFile()
                }

                FileOutputStream(_disabledDataFile).use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(disabledScoreboards)
                    }
                }

                plugin.logger.info("Disabled scoreboards saved.")
            } catch (ex: IOException) {
                plugin.logger.log(Level.WARNING, "Error while saving disabled scoreboards", ex)
            }
        }
    }

    fun createScoreboard(player: Player) {
        if (!disabledScoreboards.contains(player.uniqueId)) {
            scoreboardHandler.createScoreboard(player)
        }
    }

    fun removeScoreboard(player: Player) {
        scoreboardHandler.removeScoreboard(player)
    }

    fun clearScoreboard(player: Player) {
        if (!disabledScoreboards.contains(player.uniqueId)) {
            scoreboardHandler.clearScoreboard(player)
        }
    }

    fun updateScoreboard(title: String, scores: Map<Int, String>, player: Player) {
        if (!disabledScoreboards.contains(player.uniqueId)) {
            scoreboardHandler.updateScoreboard(title, scores, player)
        }
    }

    fun toggleScoreboard(player: Player): Boolean {
        return if (disabledScoreboards.contains(player.uniqueId)) {
            disabledScoreboards.remove(player.uniqueId)
            scoreboardHandler.createScoreboard(player)
            false
        } else {
            disabledScoreboards.add(player.uniqueId)
            scoreboardHandler.removeScoreboard(player)
            true
        }
    }

    fun isScoreboardDisabled(player: Player): Boolean {
        return disabledScoreboards.contains(player.uniqueId)
    }

    fun getWorldScoreboards(world: World): List<Scoreboard> {
        return mutableListOf<Scoreboard>().also { list ->
            plugin.config.worlds.forEach { (predicate, scoreboards) ->
                if (predicate.test(world.name)) {
                    scoreboards.forEach {
                        val scoreboard = plugin.config.scoreboards[it]
                        if (scoreboard != null) {
                            list.add(scoreboard)
                        }
                    }
                }
            }
        }.toList()
    }

    fun getScoreboard(scoreboard: String): Scoreboard? {
        return plugin.config.scoreboards[scoreboard.toLowerCase()]
    }

    fun getScoreboards(): List<Scoreboard> {
        return plugin.config.scoreboards.values.toList()
    }
}