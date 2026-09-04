package io.github.black_Kittys22.challengekittys.ChunkChallenge

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent

class ProtectionListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onBreak(e: BlockBreakEvent) {
        // Schutz für wichtige Blöcke
        val type = e.block.type
        if (type == Material.BEDROCK || type == Material.END_PORTAL_FRAME || type.name.contains("GATEWAY")) {
            e.isCancelled = true
        }
    }

}