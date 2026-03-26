package io.github.black_Kittys22.challengekittys.ChunkChallenge

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import java.util.*

class EntityDeathListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onBossDeath(e: EntityDeathEvent) {
        val entity = e.entity
        val bossId = entity.uniqueId.toString()

        if (plugin.chunkEntityMap.containsKey(bossId)) {
            val playerId = plugin.chunkEntityMap[bossId] ?: return
            val player = Bukkit.getPlayer(playerId) // KEIN "as UUID" nötig

            if (player != null && player.isOnline) {
                plugin.resetPlayerBorder(player)
                plugin.playerActiveChunk.remove(player.uniqueId)
                player.sendMessage("§aDu hast den Wächter besiegt! Die Grenze wurde aufgehoben.")
            }
            plugin.chunkEntityMap.remove(bossId)
        }
    }
}