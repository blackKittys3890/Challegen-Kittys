package io.github.black_Kittys22.challengekittys.ChunkChallenge

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class PlayerDeathListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        val player = e.entity

        // Entferne den aktiven Chunk, damit beim Respawn alles neu berechnet wird
        if (plugin.playerActiveChunk.containsKey(player.uniqueId)) {
            plugin.playerActiveChunk.remove(player.uniqueId)

            // Border zurücksetzen über Main-Methode
            plugin.resetPlayerBorder(player)
        }
    }
}