package io.github.black_Kittys22.challengekittys.Challenges

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class HalfHeartChallenge(private val plugin: Main) : Listener {

    fun applyToAll() {
        val active = plugin.isHalfHeartChallengeActive
        Bukkit.getOnlinePlayers().forEach { player ->
            val attribute = player.getAttribute(Attribute.MAX_HEALTH)
            if (active) {
                attribute?.baseValue = 1.0 // 1.0 entspricht einem halben Herz
                player.health = 1.0
            } else {
                attribute?.baseValue = 20.0 // Standard: 10 Herzen
            }
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (plugin.isHalfHeartChallengeActive) {
            val player = event.player
            player.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 1.0
            player.health = 1.0
        }
    }
}