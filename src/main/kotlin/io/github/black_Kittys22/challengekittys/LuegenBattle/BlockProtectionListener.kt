package io.github.black_Kittys22.challengekittys.LuegenBattle

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent

class BattleProtectionListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity as? org.bukkit.entity.Player ?: return

        // Verhindere Fall-Schaden in der Battle-Welt
        if (player.world.name == "battle_world" && event.cause == EntityDamageEvent.DamageCause.FALL) {
            event.isCancelled = true
        }

        // Verhindere auch Void-Schaden in Battle-Welt
        if (player.world.name == "battle_world" && event.cause == EntityDamageEvent.DamageCause.VOID) {
            event.isCancelled = true
            // Teleportiere Spieler zurueck zu seiner Struktur
            val structureLoc = plugin.structureBattleManager.getStructureLocation(player.uniqueId)
            if (structureLoc != null) {
                player.teleport(structureLoc)
            }
        }
    }

    @EventHandler
    fun onAdvancement(event: PlayerAdvancementDoneEvent) {
        val player = event.player

        // Blockiere Achievements in der Battle-Welt
        if (player.world.name == "battle_world") {
            // Entferne das Achievement wieder
            val advancement = event.advancement
            val progress = player.getAdvancementProgress(advancement)
            progress.awardedCriteria.forEach { criteria ->
                progress.revokeCriteria(criteria)
            }
        }
    }
}