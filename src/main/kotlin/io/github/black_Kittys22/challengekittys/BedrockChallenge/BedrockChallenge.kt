package io.github.black_Kittys22.challengekittys.Challenges

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.DisplaySlot
import java.util.*

class BedrockChallenge(private val plugin: Main) : Listener {
    var isActive = false
    private val startZ = mutableMapOf<UUID, Double>()
    private val maxDist = mutableMapOf<UUID, Double>()
    private val itemLockUntil = mutableMapOf<UUID, Long>()

    init {
        // Erstellt die Config-Sektion, falls sie noch nicht existiert
        if (!plugin.config.contains("bedrock-challenge.spawn-locations")) {
            plugin.config.set("bedrock-challenge.spawn-locations", listOf("world,0,100,0", "world,10,100,0"))
            plugin.saveConfig()
        }
    }

    fun start() {
        isActive = true
        startZ.clear()
        maxDist.clear()
        itemLockUntil.clear()

        val locStrings = plugin.config.getStringList("bedrock-challenge.spawn-locations")
        val players = Bukkit.getOnlinePlayers().toList()

        players.forEachIndexed { index, player ->
            if (index < locStrings.size) {
                val loc = parseLocation(locStrings[index])
                if (loc != null) {
                    val spawnLoc = loc.clone().add(0.5, 1.0, 0.5)

                    // 1. Teleport zum Start
                    player.teleport(spawnLoc)

                    // 2. Spawnpoint setzen (WICHTIG!)
                    player.setRespawnLocation(spawnLoc, true)

                    startZ[player.uniqueId] = loc.z
                    maxDist[player.uniqueId] = 0.0

                    player.sendMessage(Component.text("Challenge gestartet! Dein Spawnpoint wurde gesetzt.", NamedTextColor.GREEN))
                }
            }
        }
    }

    fun tick() {
        if (!isActive || plugin.timer.paused) return

        for (player in Bukkit.getOnlinePlayers()) {
            val sZ = startZ[player.uniqueId] ?: continue
            val currentDist = sZ - player.location.z
            if (currentDist > (maxDist[player.uniqueId] ?: 0.0)) {
                maxDist[player.uniqueId] = currentDist
            }
        }

        updateScoreboard()

        // Item Drop alle 30 Sekunden
        if (plugin.timer.timeSeconds > 0 && plugin.timer.timeSeconds % 10 == 0) {
            dropItemsAtSpawn()
        }
    }

    private fun dropItemsAtSpawn() {
        val locStrings = plugin.config.getStringList("bedrock-challenge.spawn-locations")
        val materials = Material.entries.filter { it.isItem && it.isBlock && !it.isAir }
        val players = Bukkit.getOnlinePlayers().toList()

        locStrings.forEachIndexed { index, locString ->
            val loc = parseLocation(locString) ?: return@forEachIndexed

            if (index < players.size) {
                val player = players[index]
                val now = System.currentTimeMillis()
                val lockTime = itemLockUntil[player.uniqueId] ?: 0L

                if (now > lockTime && !player.isDead && player.isOnline) {
                    val item = ItemStack(materials.random(), 1)
                    loc.world.dropItemNaturally(loc.clone().add(0.5, 1.2, 0.5), item)
                } else if (now <= lockTime) {
                    val remaining = (lockTime - now) / 1000
                    player.sendMessage(Component.text("🚫 Sperre! Noch $remaining Sek. bis zum nächsten Item.", NamedTextColor.RED))
                }
            }
        }
    }

    private fun updateScoreboard() {
        val board = Bukkit.getScoreboardManager().newScoreboard
        val title = Component.text("NORD-REKORD").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)
        val obj = board.registerNewObjective("bedrock", "dummy", title)
        obj.displaySlot = DisplaySlot.SIDEBAR

        val sortedPlayers = Bukkit.getOnlinePlayers()
            .map { p -> Triple(p, (maxDist[p.uniqueId] ?: 0.0).toInt(), ((startZ[p.uniqueId] ?: p.location.z) - p.location.z).toInt()) }
            .sortedWith(compareByDescending<Triple<Player, Int, Int>> { it.second }.thenByDescending { it.third })

        sortedPlayers.forEachIndexed { index, data ->
            val rank = index + 1
            val entryKey = "§r".repeat(rank)
            val team = board.registerNewTeam("rank_$rank")
            team.addEntry(entryKey)

            val line = Component.text("$rank. ").color(NamedTextColor.WHITE)
                .append(Component.text("${data.first.name} "))
                .append(Component.text("${data.second} ").color(NamedTextColor.GRAY))
                .append(Component.text("(${data.third})").color(NamedTextColor.DARK_GRAY))

            team.prefix(line)
            obj.getScore(entryKey).score = sortedPlayers.size - index
        }

        Bukkit.getOnlinePlayers().forEach { it.scoreboard = board }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    fun onDeath(e: PlayerDeathEvent) {
        if (!isActive) return

        val uuid = e.entity.uniqueId
        // Wir setzen die Zeit sofort
        itemLockUntil[uuid] = System.currentTimeMillis() + 60000

        e.entity.sendMessage(Component.text("☠ Todesstrafe: 1 Minute Item-Sperre!", NamedTextColor.RED, TextDecoration.BOLD))
    }

    private fun parseLocation(s: String): Location? {
        val p = s.split(",")
        if (p.size < 4) return null
        val world = Bukkit.getWorld(p[0]) ?: return null
        return Location(world, p[1].toDouble(), p[2].toDouble(), p[3].toDouble())
    }
}