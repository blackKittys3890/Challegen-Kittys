package io.github.black_Kittys22.challengekittys.ChunkChallenge

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.event.HandlerList

class VisualChunkChallenge(private val plugin: Main) {

    private val chunkListener = ChunkListener(plugin)
    private val entityDeathListener = EntityDeathListener(plugin)

    var isActive = false
        private set

    fun start() {
        if (isActive) return
        val pm = plugin.server.pluginManager
        pm.registerEvents(chunkListener, plugin)
        pm.registerEvents(entityDeathListener, plugin)
        isActive = true
    }

    fun stop() {
        if (!isActive) return
        // unregister specific listener instances
        HandlerList.unregisterAll(chunkListener)
        HandlerList.unregisterAll(entityDeathListener)
        isActive = false
        // cleanup active player states
        // Entferne aktive Borders und Mappings falls nötig
        val toRemove = plugin.chunkEntityMap.keys.toList()
        for (id in toRemove) {
            val playerId = plugin.chunkEntityMap[id]
            if (playerId != null) {
                val player = plugin.server.getPlayer(playerId)
                if (player != null && player.isOnline) {
                    plugin.resetPlayerBorder(player)
                    plugin.playerActiveChunk.remove(player.uniqueId)
                }
            }
        }
        plugin.chunkEntityMap.clear()
    }
}
