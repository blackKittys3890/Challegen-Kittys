package io.github.black_Kittys22.challengekittys.Challenges

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

class FarbspurChallenge(private val plugin: Main) : Listener {

    var isActive = false

    private val playerTrails = mutableMapOf<UUID, MutableList<Location>>()
    private val playerColors = mutableMapOf<UUID, DyeColor>()
    private val availableColors = DyeColor.values().toMutableList()
    private val lastTrailPosition = mutableMapOf<UUID, Location?>()
    private val placedBlocks = mutableSetOf<Location>()
    private var isMassDeathActive = false
    private var isPaused = false

    init {
        resetColors()
    }

    private fun resetColors() {
        availableColors.clear()
        availableColors.addAll(DyeColor.values())
        availableColors.remove(DyeColor.BLACK)
        availableColors.remove(DyeColor.WHITE)
        availableColors.remove(DyeColor.GRAY)
        availableColors.remove(DyeColor.LIGHT_GRAY)
    }

    fun start() {
        if (isActive) return
        isActive = true
        isPaused = false
        isMassDeathActive = false

        Bukkit.getOnlinePlayers().forEach { player ->
            assignColor(player)
        }

        Bukkit.broadcast(
            Component.text("✦ FARBSPUR-CHALLENGE GESTARTET ✦", NamedTextColor.GOLD, TextDecoration.BOLD)
        )
        Bukkit.broadcast(
            Component.text("Hinterlasse eine farbige Spur! ", NamedTextColor.GRAY)
                .append(Component.text("Kreuze keine Spur!", NamedTextColor.RED, TextDecoration.BOLD))
        )
        Bukkit.broadcast(
            Component.text("Bei einer Kreuzung sterben ALLE!", NamedTextColor.DARK_RED, TextDecoration.BOLD)
        )

        plugin.logger.info("§a[FarbspurChallenge] Gestartet!")
    }

    fun stop() {
        if (!isActive) return
        isActive = false
        isPaused = false
        isMassDeathActive = false

        clearAllTrails()

        playerTrails.clear()
        playerColors.clear()
        lastTrailPosition.clear()
        resetColors()

        Bukkit.broadcast(
            Component.text("✦ FARBSPUR-CHALLENGE BEENDET ✦", NamedTextColor.RED, TextDecoration.BOLD)
        )

        plugin.logger.info("§c[FarbspurChallenge] Gestoppt!")
    }

    fun reset() {
        stop()
        playerTrails.clear()
        playerColors.clear()
        placedBlocks.clear()
        lastTrailPosition.clear()
        resetColors()
    }

    private fun assignColor(player: Player) {
        val color = if (availableColors.isNotEmpty()) {
            val randomColor = availableColors.random()
            availableColors.remove(randomColor)
            randomColor
        } else {
            DyeColor.values().random()
        }

        playerColors[player.uniqueId] = color
        playerTrails[player.uniqueId] = mutableListOf()

        val colorName = when (color) {
            DyeColor.RED -> "Rot"
            DyeColor.BLUE -> "Blau"
            DyeColor.GREEN -> "Grün"
            DyeColor.YELLOW -> "Gelb"
            DyeColor.PURPLE -> "Lila"
            DyeColor.ORANGE -> "Orange"
            DyeColor.PINK -> "Pink"
            DyeColor.CYAN -> "Türkis"
            DyeColor.LIME -> "Hellgrün"
            DyeColor.MAGENTA -> "Magenta"
            else -> color.name
        }

        player.sendMessage(
            Component.text("Deine Spur-Farbe: ", NamedTextColor.GRAY)
                .append(Component.text(colorName, getChatColor(color), TextDecoration.BOLD))
        )
    }

    private fun getChatColor(color: DyeColor): NamedTextColor {
        return when (color) {
            DyeColor.RED -> NamedTextColor.RED
            DyeColor.BLUE -> NamedTextColor.BLUE
            DyeColor.GREEN -> NamedTextColor.GREEN
            DyeColor.YELLOW -> NamedTextColor.YELLOW
            DyeColor.PURPLE -> NamedTextColor.LIGHT_PURPLE
            DyeColor.ORANGE -> NamedTextColor.GOLD
            DyeColor.PINK -> NamedTextColor.LIGHT_PURPLE
            DyeColor.CYAN -> NamedTextColor.AQUA
            DyeColor.LIME -> NamedTextColor.GREEN
            DyeColor.MAGENTA -> NamedTextColor.LIGHT_PURPLE
            DyeColor.WHITE -> NamedTextColor.WHITE
            DyeColor.BLACK -> NamedTextColor.DARK_GRAY
            DyeColor.GRAY -> NamedTextColor.GRAY
            DyeColor.LIGHT_GRAY -> NamedTextColor.GRAY
            DyeColor.BROWN -> NamedTextColor.GOLD
            else -> NamedTextColor.WHITE
        }
    }

    private fun getConcreteMaterial(color: DyeColor): Material {
        return when (color) {
            DyeColor.WHITE -> Material.WHITE_CONCRETE
            DyeColor.ORANGE -> Material.ORANGE_CONCRETE
            DyeColor.MAGENTA -> Material.MAGENTA_CONCRETE
            DyeColor.LIGHT_BLUE -> Material.LIGHT_BLUE_CONCRETE
            DyeColor.YELLOW -> Material.YELLOW_CONCRETE
            DyeColor.LIME -> Material.LIME_CONCRETE
            DyeColor.PINK -> Material.PINK_CONCRETE
            DyeColor.GRAY -> Material.GRAY_CONCRETE
            DyeColor.LIGHT_GRAY -> Material.LIGHT_GRAY_CONCRETE
            DyeColor.CYAN -> Material.CYAN_CONCRETE
            DyeColor.PURPLE -> Material.PURPLE_CONCRETE
            DyeColor.BLUE -> Material.BLUE_CONCRETE
            DyeColor.BROWN -> Material.BROWN_CONCRETE
            DyeColor.GREEN -> Material.GREEN_CONCRETE
            DyeColor.RED -> Material.RED_CONCRETE
            DyeColor.BLACK -> Material.BLACK_CONCRETE
            else -> Material.WHITE_CONCRETE
        }
    }

    private fun removeTrailBlock(location: Location) {
        if (!placedBlocks.remove(location)) return

        val block = location.block
        if (block.type.name.endsWith("_CONCRETE")) {
            block.type = Material.AIR
        }
    }

    private fun clearAllTrails() {
        placedBlocks.forEach { location ->
            val block = location.block
            if (block.type.name.endsWith("_CONCRETE")) {
                block.type = Material.AIR
            }
        }
        placedBlocks.clear()
        playerTrails.values.forEach { it.clear() }
        lastTrailPosition.clear()
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (!isActive || isPaused) return
        val player = event.player

        val from = event.from
        val to = event.to ?: return

        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return
        }

        val world = to.world ?: return
        if (world.environment != World.Environment.NORMAL) return

        val uuid = player.uniqueId

        if (!playerColors.containsKey(uuid)) {
            assignColor(player)
            playerTrails[uuid] = mutableListOf()
            lastTrailPosition[uuid] = null
        }

        val color = playerColors[uuid] ?: return
        val trail = playerTrails[uuid] ?: return

        val trailLocation = to.clone()
        trailLocation.y = to.blockY - 1.0
        trailLocation.x = trailLocation.blockX + 0.5
        trailLocation.z = trailLocation.blockZ + 0.5

        val lastPos = lastTrailPosition[uuid]
        if (lastPos != null &&
            lastPos.blockX == trailLocation.blockX &&
            lastPos.blockZ == trailLocation.blockZ) {
            return
        }

        if (!isTrailValid(trailLocation, uuid)) {
            handleTrailCrossing(player)
            return
        }

        val block = trailLocation.block
        val material = getConcreteMaterial(color)

        if (block.type != material) {
            block.type = material
            placedBlocks.add(trailLocation.clone())
            trail.add(trailLocation.clone())
            lastTrailPosition[uuid] = trailLocation.clone()
        }
    }

    private fun isTrailValid(location: Location, playerUUID: UUID): Boolean {
        val isBlocked = placedBlocks.any { placed ->
            placed.blockX == location.blockX &&
                    placed.blockY == location.blockY &&
                    placed.blockZ == location.blockZ
        }

        if (isBlocked) {
            return false
        }

        val trail = playerTrails[playerUUID] ?: return true
        val ownTrailBlocked = trail.any { trailLoc ->
            trailLoc.blockX == location.blockX &&
                    trailLoc.blockY == location.blockY &&
                    trailLoc.blockZ == location.blockZ
        }

        if (ownTrailBlocked) {
            return false
        }

        return true
    }

    private fun handleTrailCrossing(player: Player) {
        if (isMassDeathActive) return
        isMassDeathActive = true
        isPaused = true

        Bukkit.broadcast(
            Component.text("✖ " + player.name + " HAT EINE SPUR GEKREUZT! ✖", NamedTextColor.RED, TextDecoration.BOLD)
        )
        Bukkit.broadcast(
            Component.text("ALLE SPIELER STERBEN!", NamedTextColor.DARK_RED, TextDecoration.BOLD)
        )

        // Alle Spieler töten
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            Bukkit.getOnlinePlayers().forEach { target ->
                target.health = 0.0
                target.damage(1000.0)
            }

            // Nach einer kurzen Pause die Challenge fortsetzen
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                clearAllTrails()

                resetColors()
                Bukkit.getOnlinePlayers().forEach { p ->
                    val oldColor = playerColors.remove(p.uniqueId)
                    if (oldColor != null) {
                        availableColors.add(oldColor)
                    }
                    assignColor(p)
                    playerTrails[p.uniqueId] = mutableListOf()
                    lastTrailPosition[p.uniqueId] = null
                }

                isMassDeathActive = false
                isPaused = false

                Bukkit.broadcast(
                    Component.text("✦ FARBSPUR-CHALLENGE NEU GESTARTET ✦", NamedTextColor.GOLD, TextDecoration.BOLD)
                )
                Bukkit.broadcast(
                    Component.text("Alle Spuren wurden gelöscht! Neue Farben verteilt!", NamedTextColor.GRAY)
                )
            }, 40L)
        }, 20L)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!isActive) return

        val uuid = event.player.uniqueId

        val color = playerColors.remove(uuid)
        if (color != null) {
            availableColors.add(color)
        }

        if (!isPaused) {
            val trail = playerTrails.remove(uuid)
            trail?.forEach { removeTrailBlock(it) }
        } else {
            playerTrails.remove(uuid)
        }
        lastTrailPosition.remove(uuid)
    }
}