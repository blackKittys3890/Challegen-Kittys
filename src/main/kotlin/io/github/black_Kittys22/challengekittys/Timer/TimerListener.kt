package io.github.black_Kittys22.challengekittys.Timer

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class TimerListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        // Nur starten, wenn Auto-Start in den Settings aktiv ist
        if (plugin.isTimerAutoStartEnabled) {
            plugin.timer.paused = false
        }
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        // Pausieren, wenn der letzte geht (immer sinnvoll, um Zeitfehler zu vermeiden)
        if (Bukkit.getOnlinePlayers().size <= 1) {
            plugin.timer.paused = true
        }
    }
}