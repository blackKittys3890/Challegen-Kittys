package io.github.black_Kittys22.challengekittys.DamageInvClear

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import java.time.Duration
import kotlin.math.roundToInt

class DamageListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onDamage(e: EntityDamageEvent) {
        val victim = e.entity as? Player ?: return

        // Challenge Check
        if (plugin.isDamageClearInventoryActive && !plugin.timer.paused) {

            // 1. Berechnung der Herzen (Schaden / 2)
            // Beispiel: 10 Schaden = 5 Herzen
            val heartAmount = (e.finalDamage / 2.0).roundToInt()

            // 2. Symbol des Spielers holen, der den Schaden bekommen hat
            val symbol = plugin.getPlayerSymbol(victim)

            // 3. Title-Komponenten (Adventure API verhindert Warnungen)
            val mainTitle = Component.text(symbol, NamedTextColor.WHITE)
            val subTitle = Component.text("-$heartAmount ❤", NamedTextColor.RED, TextDecoration.BOLD)

            val times = Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1500), Duration.ofMillis(500))
            val title = Title.title(mainTitle, subTitle, times)

            // 4. Globaler Effekt: Alle Spieler betroffen
            for (all in Bukkit.getOnlinePlayers()) {
                // Inventar leeren
                all.inventory.clear()

                // Title anzeigen
                all.showTitle(title)

                // Sound-Feedback
                all.playSound(all.location, Sound.ENTITY_ITEM_BREAK, 1f, 0.6f)
            }

        }
    }
}