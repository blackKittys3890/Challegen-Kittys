package io.github.black_Kittys22.challengekittys.ChunkChallenge.effects

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.event.HandlerList

class EffectsChallenge(private val plugin: Main) {
    private val listener = VisualChunkListener(plugin)
    var isActive = false
        private set

    fun start() {
        if (isActive) return
        plugin.server.pluginManager.registerEvents(listener, plugin)
        isActive = true
    }

    fun stop() {
        if (!isActive) return
        // remove all potion effects applied by listener before unregister
        try { listener.cleanupAllEffects() } catch (_: Exception) {}
        HandlerList.unregisterAll(listener)
        isActive = false
    }
}
