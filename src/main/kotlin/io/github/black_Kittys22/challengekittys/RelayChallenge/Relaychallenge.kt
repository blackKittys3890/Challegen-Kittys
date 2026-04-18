package io.github.black_Kittys22.challengekittys.RelayChallenge

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.advancement.AdvancementProgress
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

class RelayChallenge(private val plugin: Main) : Listener {

    companion object {
        const val INTERVAL_TICKS = 2400L // 2 Min. (Test) → 6000L für 5 Min. (Live)
        const val HEART_LOSS = 2.0       // 1 Herz = 2.0 HP
    }

    var isActive = false
        private set

    private val order = mutableListOf<UUID>()
    private var currentIndex = 0
    private var mainTask: BukkitTask? = null
    private var ticksLeft = INTERVAL_TICKS

    // ── Public API ───────────────────────────────────────────────────────────

    fun start() {
        if (isActive) return
        val eligible = Bukkit.getOnlinePlayers()
            .filter { !plugin.exemptPlayers.contains(it.uniqueId) }
        if (eligible.size < 2) {
            Bukkit.broadcast(
                Component.text("[Relay] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .append(Component.text("Mindestens 2 Spieler nötig!", NamedTextColor.RED))
            )
            return
        }
        order.clear()
        order.addAll(eligible.map { it.uniqueId }.shuffled())
        currentIndex = 0
        isActive = true
        ticksLeft = INTERVAL_TICKS
        activateCurrentPlayer()
        startTasks()
    }

    fun stop() {
        if (!isActive) return
        isActive = false
        stopTasks()
        for (uuid in order) {
            Bukkit.getPlayer(uuid)?.let { p ->
                if (p.gameMode == GameMode.SPECTATOR) p.spectatorTarget = null
                p.gameMode = GameMode.SURVIVAL
            }
        }
        order.clear()
        Bukkit.broadcast(
            Component.text("[Relay] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text("Challenge gestoppt.", NamedTextColor.RED))
        )
    }

    fun currentActivePlayer(): UUID? =
        if (isActive && order.isNotEmpty()) order[currentIndex] else null

    // ── Tod → alle verlieren ein Herz ────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!isActive) return
        if (plugin.exemptPlayers.contains(event.entity.uniqueId)) return

        // Todes-Nachricht unterdrücken
        event.deathMessage(null)

        // Allen ein Herz abziehen (verzögert, nach dem Respawn-Handling)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            var challengeOver = false
            var killedPlayer: String? = null

            for (uuid in order) {
                val p = Bukkit.getPlayer(uuid) ?: continue
                val maxHp = p.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
                val newMax = (maxHp - HEART_LOSS).coerceAtLeast(0.0)

                p.getAttribute(Attribute.MAX_HEALTH)?.baseValue = newMax
                p.health = p.health.coerceAtMost(newMax)

                if (newMax <= 0.0) {
                    challengeOver = true
                    killedPlayer = p.name
                }
            }

            // Broadcast: ein Herz weg
            Bukkit.broadcast(
                Component.text("❤ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("Alle verlieren ein Herz! ", NamedTextColor.YELLOW))
                    .append(Component.text("(${event.entity.name} ist gestorben)", NamedTextColor.GRAY))
            )

            if (challengeOver) {
                Bukkit.broadcast(
                    Component.text("━━━━━ ", NamedTextColor.DARK_RED)
                        .append(Component.text("RELAY CHALLENGE VORBEI", NamedTextColor.RED, TextDecoration.BOLD))
                        .append(Component.text(" ━━━━━", NamedTextColor.DARK_RED))
                )
                Bukkit.broadcast(
                    Component.text("$killedPlayer hat 0 Herzen – die Challenge ist beendet!", NamedTextColor.RED)
                )
                stop()
                // Alle in Spectator setzen wie Dead-Sync
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    for (p in Bukkit.getOnlinePlayers()) {
                        p.gameMode = GameMode.SPECTATOR
                    }
                })
                plugin.timer.paused = true
            }
        }, 1L)
    }

    // ── Rotation ─────────────────────────────────────────────────────────────

    private fun rotate() {
        if (!isActive || order.isEmpty()) return
        val currentPlayer = Bukkit.getPlayer(order[currentIndex])
        val nextIndex = findNextOnlineIndex()
        if (nextIndex == -1) return
        val nextPlayer = Bukkit.getPlayer(order[nextIndex]) ?: return
        if (currentPlayer != null) transferState(from = currentPlayer, to = nextPlayer)
        currentIndex = nextIndex
        ticksLeft = INTERVAL_TICKS
        // Stiller Wechsel – kein Broadcast, kein Titel
        activateCurrentPlayer()
    }

    /**
     * Aktiver Spieler → SURVIVAL an der aktuellen Position des vorherigen Spielers.
     * Zuschauer       → SPECTATOR + spectatorTarget auf aktiven Spieler.
     *
     * Alles in runTask (+1 Tick) damit wir nie im Event-Kontext sind.
     * spectatorTarget nochmal 2 Ticks später (GameMode muss Client-seitig ankommen).
     */
    private fun activateCurrentPlayer() {
        val activeUUID   = order[currentIndex]
        val activePlayer = Bukkit.getPlayer(activeUUID)

        for ((i, uuid) in order.withIndex()) {
            val p = Bukkit.getPlayer(uuid) ?: continue
            if (i == currentIndex) {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (p.gameMode == GameMode.SPECTATOR) p.spectatorTarget = null
                    p.gameMode = GameMode.SURVIVAL
                })
            } else {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    p.gameMode = GameMode.SPECTATOR
                    Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                        if (p.isOnline && p.gameMode == GameMode.SPECTATOR
                            && activePlayer != null && activePlayer.isOnline) {
                            p.spectatorTarget = activePlayer
                        }
                    }, 2L)
                })
            }
        }

        // Discord: aktiver Spieler wird gedeafened, alle anderen nicht
        plugin.discordVoiceManager.onRotation(activeUUID, order)
    }

    private fun findNextOnlineIndex(): Int {
        val size = order.size
        for (offset in 1 until size) {
            val idx = (currentIndex + offset) % size
            if (Bukkit.getPlayer(order[idx]) != null) return idx
        }
        return -1
    }

    // ── Spielstand-Transfer ──────────────────────────────────────────────────

    private fun transferState(from: Player, to: Player) {
        // Inventar
        val invContents   = copyContents(from.inventory.contents)
        val armorContents = copyContents(from.inventory.armorContents)
        val offHand       = from.inventory.itemInOffHand.clone()
        to.inventory.contents      = invContents
        to.inventory.armorContents = armorContents
        to.inventory.setItemInOffHand(offHand)
        to.updateInventory()

        from.inventory.clear()
        from.inventory.armorContents = arrayOfNulls(4)
        from.inventory.setItemInOffHand(null)
        from.updateInventory()

        // Stats
        val maxHp = to.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        to.health     = from.health.coerceAtMost(maxHp)
        to.foodLevel  = from.foodLevel
        to.saturation = from.saturation
        to.exp        = from.exp
        to.level      = from.level

        // Position: nächster Spieler spawnt genau dort wo der aktive war
        val loc = from.location.clone()
        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (to.isOnline && to.gameMode == GameMode.SURVIVAL) {
                to.teleport(loc)
            } else {
                // Sicherheits-Fallback: kurz warten bis GameMode-Wechsel durch ist
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    if (to.isOnline) to.teleport(loc)
                }, 3L)
            }
        })

        // Advancements
        applyAdvancements(to, collectAdvancements(from))
    }

    // ── Tasks ────────────────────────────────────────────────────────────────

    private fun startTasks() {
        // Kein Countdown-Task mehr – du nutzt deinen eigenen Timer
        mainTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            rotate()
            ticksLeft = INTERVAL_TICKS
        }, INTERVAL_TICKS, INTERVAL_TICKS)
    }

    private fun stopTasks() {
        mainTask?.cancel(); mainTask = null
    }

    // ── Advancements ─────────────────────────────────────────────────────────

    private fun collectAdvancements(player: Player): Map<String, Set<String>> {
        val result = mutableMapOf<String, Set<String>>()
        val iter = Bukkit.advancementIterator()
        while (iter.hasNext()) {
            val adv = iter.next()
            val awarded = player.getAdvancementProgress(adv).awardedCriteria
            if (awarded.isNotEmpty()) result[adv.key.toString()] = awarded.toSet()
        }
        return result
    }

    private fun applyAdvancements(player: Player, snapshot: Map<String, Set<String>>) {
        val iter = Bukkit.advancementIterator()
        while (iter.hasNext()) {
            val adv = iter.next()
            val progress: AdvancementProgress = player.getAdvancementProgress(adv)
            for (criterion in progress.awardedCriteria.toList()) progress.revokeCriteria(criterion)
            snapshot[adv.key.toString()]?.forEach { progress.awardCriteria(it) }
        }
    }

    private fun copyContents(arr: Array<ItemStack?>): Array<ItemStack?> =
        Array(arr.size) { i -> arr[i]?.clone() }
}