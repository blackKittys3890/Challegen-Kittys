package io.github.black_Kittys22.challengekittys

import org.bukkit.entity.Monster
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent

class MobProtectionListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onMobDamage(event: EntityDamageEvent) {
        if (!plugin.isChunkChallengeSelected) return
        if (event.entity !is Monster) return

        val mobId = event.entity.uniqueId.toString()

        // Challenge-Mobs (in chunkEntityMap) bekommen KEINEN Schutz – die soll der Spieler töten!
        if (plugin.chunkEntityMap.containsKey(mobId)) return

        // Normale Mobs: Spieler-Schaden weiterhin erlauben
        if (event.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
            event.cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ||
            event.cause == EntityDamageEvent.DamageCause.PROJECTILE ||
            event.cause == EntityDamageEvent.DamageCause.MAGIC ||
            event.cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
            event.cause == EntityDamageEvent.DamageCause.FIRE ||
            event.cause == EntityDamageEvent.DamageCause.LAVA) {
            return
        }

        // Alles andere (Fallschaden, Ertrinken, Erstickung) blockieren
        event.isCancelled = true
    }
}