package io.github.black_Kittys22.challengekittys.LuegenBattle

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent

class StructureDeathListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onDeath(e: PlayerDeathEvent) {
        // Falls der Spieler im Lügenbattle ist, unterdrücke die Todesnachricht
        if (plugin.structureBattleManager.isPlayerInBattle(e.entity.uniqueId)) {
            e.deathMessage(null)
        }
    }

    @EventHandler
    fun onRespawn(e: PlayerRespawnEvent) {
        // Teleportiere den Spieler zurück zur Struktur
        val loc = plugin.structureBattleManager.getStructureLocation(e.player.uniqueId)
        if (loc != null) {
            e.respawnLocation = loc
        }
    }
}