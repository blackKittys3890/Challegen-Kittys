package io.github.black_Kittys22.challengekittys.ChunkChallenge

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import io.papermc.paper.event.entity.EntityMoveEvent

class ChunkListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onPlayerMove(e: PlayerMoveEvent) {
        if (e.from.chunk == e.to.chunk) return
        if (!plugin.isChunkChallengeSelected || plugin.timer.paused) return

        val player = e.player
        val chunk = e.to.chunk
        val key = plugin.makeChunkKey(chunk.x, chunk.z)

        if (plugin.playerActiveChunk[player.uniqueId] == key) return
        // ... (restliche Logik aus deiner Datei)
    }

    @EventHandler
    fun onMobMove(e: EntityMoveEvent) {
        val entity = e.entity
        if (entity is Player) return

        val bossId = entity.uniqueId.toString()
        val playerId = plugin.chunkEntityMap[bossId] ?: return

        // FIX: Cast entfernt
        val player = Bukkit.getPlayer(playerId) ?: return
        val key = plugin.playerActiveChunk[player.uniqueId] ?: return

        if (!plugin.isLocationInChunk(e.to, key)) {
            e.isCancelled = true
            entity.teleport(e.from)
        }
    }

    @EventHandler
    fun onBossKnockback(e: EntityDamageEvent) {
        val entity = e.entity
        if (entity is Player) return

        val bossId = entity.uniqueId.toString()
        val playerId = plugin.chunkEntityMap[bossId] ?: return

        // FIX: Cast entfernt
        val player = Bukkit.getPlayer(playerId) ?: return
        val key = plugin.playerActiveChunk[player.uniqueId] ?: return

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (!plugin.isLocationInChunk(entity.location, key)) {
                val highestY = entity.world.getHighestBlockYAt(entity.location)
                entity.teleport(entity.location.apply { y = highestY.toDouble() + 1 })
            }
        }, 1L)
    }
}