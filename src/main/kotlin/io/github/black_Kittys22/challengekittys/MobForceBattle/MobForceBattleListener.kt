package io.github.black_Kittys22.challengekittys.MobForceBattle

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

class MobForceBattleListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onEntityDeath(e: EntityDeathEvent) {
        val manager = plugin.mobForceBattleManager
        if (!manager.isActive) return

        val killer = e.entity.killer ?: return
        val killedType = e.entityType

        val team = manager.getTeamOf(killer.uniqueId)
            ?: manager.registerSoloPlayer(killer.uniqueId).also { solo ->
                // erstes Ziel anzeigen wenn erstmalig registriert
                val first = manager.currentMob(solo) ?: return
                killer.sendMessage(
                    Component.text("[MobForceBattle] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("Dein erstes Ziel: ", NamedTextColor.WHITE))
                        .append(Component.text(first.name.lowercase().replace("_", " ")
                            .replaceFirstChar { it.uppercase() }, NamedTextColor.RED, TextDecoration.BOLD))
                )
                return
            }

        if (team.finished) return

        val target = manager.currentMob(team) ?: return

        if (killedType != target) {
            // Falscher Mob – kurze Info nur an den Killer
            killer.sendMessage(
                Component.text("[MobForceBattle] ", NamedTextColor.GOLD)
                    .append(Component.text("Das ist nicht euer Ziel! Tötet: ", NamedTextColor.RED))
                    .append(Component.text(target.name.lowercase().replace("_", " ")
                        .replaceFirstChar { it.uppercase() }, NamedTextColor.YELLOW, TextDecoration.BOLD))
            )
            return
        }

        // Richtiger Mob getötet!
        manager.advanceTeam(team)
    }
}