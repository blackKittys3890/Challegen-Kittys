package io.github.black_Kittys22.challengekittys

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class DeathListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onDeath(e: PlayerDeathEvent) {
        val victim = e.entity

        if (plugin.exemptPlayers.contains(victim.uniqueId)) return
        // FIX: .uniqueId hinzugefügt
        if (plugin.structureBattleManager.isPlayerInBattle(victim.uniqueId)) {
            e.deathMessage(null)
        }

        if (plugin.isDeadSyncActive) {
            for (all in Bukkit.getOnlinePlayers()) {
                if (all.uniqueId != victim.uniqueId) {
                    all.gameMode = GameMode.SPECTATOR
                    all.sendMessage(Component.text("Ein Spieler ist gestorben! Die Challenge ist vorbei.", NamedTextColor.RED))
                }
            }
            plugin.timer.paused = true
            Bukkit.broadcast(
                Component.text("Challenge beendet: ${victim.name} ist gestorben.", NamedTextColor.DARK_RED)
            )
        }
    }

    @EventHandler
    fun onRespawn(e: PlayerRespawnEvent) {
        val player = e.player

        // FIX: .uniqueId hinzugefügt
        val structureLoc = plugin.structureBattleManager.getStructureLocation(player.uniqueId)
        if (structureLoc != null) {
            e.respawnLocation = structureLoc
        }

        if (plugin.isDeadSyncActive) {
            Bukkit.getScheduler().runTask(plugin, Runnable {
                player.gameMode = GameMode.SPECTATOR
            })
        }
    }
}