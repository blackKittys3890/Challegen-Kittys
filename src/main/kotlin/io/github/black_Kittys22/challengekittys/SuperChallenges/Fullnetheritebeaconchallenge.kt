package io.github.black_Kittys22.challengekittys.SuperChallenges

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAdvancementDoneEvent

class FullNetheriteBeaconChallenge(private val plugin: Main) : Listener {

    var isActive    = false
    var isCompleted = false

    private val beaconatorKey = NamespacedKey.minecraft("nether/create_full_beacon")

    fun reset() {
        isActive    = false
        isCompleted = false
    }

    @EventHandler
    fun onAdvancement(e: PlayerAdvancementDoneEvent) {
        if (e.advancement.key.key != "nether/create_full_beacon") return

        val player = e.player

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val beaconLoc = findBeaconNear(player)
            val isNetherite = beaconLoc != null &&
                    isFullNetheriteBeacon(beaconLoc.blockX, beaconLoc.blockY, beaconLoc.blockZ, beaconLoc.world)

            if (!isNetherite) {
                // Advancement wieder entziehen – Spieler muss es mit Netherite nochmal holen
                val advancement = Bukkit.getAdvancement(beaconatorKey) ?: return@Runnable

                player.sendMessage(
                    Component.text("[Super-Challenge] Beaconator nur mit einer Netherite-Pyramide!", NamedTextColor.RED)
                )
                return@Runnable
            }

            if (isActive && !isCompleted) {
                complete(player)
            }
        }, 2L)
    }

    private fun findBeaconNear(player: Player): Location? {
        val loc   = player.location
        val world = loc.world ?: return null
        var closest: Location? = null
        var closestDist = Double.MAX_VALUE

        for (x in (loc.blockX - 64)..(loc.blockX + 64)) {
            for (y in (loc.blockY - 16)..(loc.blockY + 16)) {
                for (z in (loc.blockZ - 64)..(loc.blockZ + 64)) {
                    if (world.getBlockAt(x, y, z).type == Material.BEACON) {
                        val bLoc = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                        val dist = bLoc.distanceSquared(loc)
                        if (dist < closestDist) {
                            closestDist = dist
                            closest = bLoc
                        }
                    }
                }
            }
        }
        return closest
    }

    private fun isFullNetheriteBeacon(bx: Int, by: Int, bz: Int, world: World): Boolean {
        for (level in 1..4) {
            val y = by - level
            for (x in (bx - level)..(bx + level)) {
                for (z in (bz - level)..(bz + level)) {
                    if (world.getBlockAt(x, y, z).type != Material.NETHERITE_BLOCK) return false
                }
            }
        }
        return true
    }

    private fun complete(player: Player) {
        if (isCompleted) return
        isCompleted = true

        Bukkit.broadcast(Component.text(""))
        Bukkit.broadcast(
            Component.text("★ SUPER-CHALLENGE GESCHAFFT ★", NamedTextColor.GOLD, TextDecoration.BOLD)
        )
        Bukkit.broadcast(
            Component.text("${player.name} hat einen Beacon auf einer vollständigen Netherite-Pyramide gebaut!", NamedTextColor.YELLOW)
        )

        Bukkit.getOnlinePlayers().forEach { p ->
            p.playSound(p.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
        }
    }
}