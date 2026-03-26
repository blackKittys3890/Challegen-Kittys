package io.github.black_Kittys22.challengekittys

import org.bukkit.entity.Monster
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent

class MobProtectionListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onMobDamage(event: EntityDamageEvent) {
        // 1. Nur ausführen, wenn die Chunk Challenge aktiv ist
        if (!plugin.isChunkChallengeSelected) return

        // 2. Nur für Monster (Skelette, Zombies, Creeper, etc.)
        if (event.entity is Monster) {

            // 3. AUSNAHMEN: Was soll Mobs weiterhin töten?
            // Wenn sie durch Sonne (FIRE_TICK) oder Feuer verbrennen, lassen wir das zu.
            if (event.cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                event.cause == EntityDamageEvent.DamageCause.FIRE ||
                event.cause == EntityDamageEvent.DamageCause.LAVA) {
                return
            }

            // 4. Alle anderen Schadensarten (z.B. Ertrinken, Fallschaden, Erstickung in Blöcken)
            // blockieren wir, damit die Mobs in ihren Chunks "überleben".
            event.isCancelled = true
        }
    }
}