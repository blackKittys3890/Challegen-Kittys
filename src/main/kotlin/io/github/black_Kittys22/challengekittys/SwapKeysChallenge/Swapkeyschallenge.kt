package io.github.black_Kittys22.challengekittys.Challenges

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInputEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

class SwapKeysChallenge(private val plugin: Main) : Listener {

    var isActive = false

    // Kontrolleur-UUID → Ziel-UUID
    private val controlMap = mutableMapOf<UUID, UUID>()

    // Aktueller Input-Zustand pro Spieler
    private data class InputState(
        val forward:  Boolean = false,
        val backward: Boolean = false,
        val left:     Boolean = false,
        val right:    Boolean = false,
        val jump:     Boolean = false,
        val sneak:    Boolean = false,
        val sprint:   Boolean = false,
    )
    private val inputState = mutableMapOf<UUID, InputState>()

    // Spieler die gerade per Teleport bewegt werden → MoveEvent ignorieren
    private val beingMoved = mutableSetOf<UUID>()

    // Laufender Tick-Task
    private var tickTask: Int = -1

    // ── Zuordnung auslosen ────────────────────────────────────────────────────

    fun shuffleAssignments() {
        controlMap.clear()
        val players = Bukkit.getOnlinePlayers().map { it.uniqueId }.toMutableList()
        if (players.size < 2) return

        val targets = players.toMutableList()
        do { targets.shuffle() } while (players.indices.any { players[it] == targets[it] })
        players.forEachIndexed { i, uuid -> controlMap[uuid] = targets[i] }

        for ((ctrlId, targetId) in controlMap) {
            val ctrl   = Bukkit.getPlayer(ctrlId)   ?: continue
            val target = Bukkit.getPlayer(targetId) ?: continue
            ctrl.sendMessage(
                Component.text("[SwapKeys] ", NamedTextColor.GOLD)
                    .append(Component.text("Du kontrollierst: ", NamedTextColor.YELLOW))
                    .append(Component.text(target.name, NamedTextColor.AQUA))
            )
        }
    }

    // ── Input tracken (korrekte Paper-API Methodennamen) ──────────────────────

    @EventHandler
    fun onInput(e: PlayerInputEvent) {
        if (!isActive) return
        val id  = e.player.uniqueId
        val inp = e.input
        inputState[id] = InputState(
            forward  = inp.isForward,
            backward = inp.isBackward,
            left     = inp.isLeft,
            right    = inp.isRight,
            jump     = inp.isJump,
            sneak    = inp.isSneak,
            sprint   = inp.isSprint,
        )
    }

    // ── Kontrolleur-Bewegung + Sprung blockieren ─────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onMove(e: PlayerMoveEvent) {
        if (!isActive) return
        val player = e.player
        val id = player.uniqueId
        if (beingMoved.contains(id)) return

        // Nur eingreifen wenn dieser Spieler ein Kontrolleur ist
        if (!controlMap.containsKey(id)) return

        val state = inputState[id]

        // Sprung abbrechen: Y-Velocity auf 0 setzen sobald Spieler abhebt
        if (e.to.y > e.from.y && player.isOnGround.not()) {
            val vel = player.velocity
            if (vel.y > 0) {
                player.velocity = vel.clone().setY(0.0)
            }
        }

        val anyMovement = state != null && (state.forward || state.backward || state.left || state.right)
        if (!anyMovement) return
        if (!e.hasChangedPosition()) return

        // XZ einfrieren, Y auf from-Höhe zurücksetzen (kein Springen)
        val blocked = e.from.clone()
        blocked.yaw   = e.to.yaw
        blocked.pitch = e.to.pitch
        e.setTo(blocked)
    }

    // ── Tick-Loop: Ziel-Spieler steuern ───────────────────────────────────────

    private fun startTickTask() {
        tickTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, Runnable {
            for ((ctrlId, targetId) in controlMap) {
                Bukkit.getPlayer(ctrlId)   ?: continue
                val target = Bukkit.getPlayer(targetId) ?: continue
                val state  = inputState[ctrlId] ?: continue

                val loc    = target.location
                val yawRad = Math.toRadians(loc.yaw.toDouble())
                val fwdX   = -Math.sin(yawRad)
                val fwdZ   =  Math.cos(yawRad)
                val rightX =  Math.cos(yawRad)
                val rightZ =  Math.sin(yawRad)

                val speed = if (state.sprint) 0.28 else 0.18

                var dx = 0.0
                var dz = 0.0
                if (state.forward)  { dx += fwdX * speed;   dz += fwdZ * speed   }
                if (state.backward) { dx -= fwdX * speed;   dz -= fwdZ * speed   }
                if (state.right)    { dx += rightX * speed; dz += rightZ * speed }
                if (state.left)     { dx -= rightX * speed; dz -= rightZ * speed }

                // Sneak
                if (state.sneak != target.isSneaking) target.isSneaking = state.sneak

                // Sprint
                if (state.sprint != target.isSprinting) target.isSprinting = state.sprint

                // Sprung
                if (state.jump && target.isOnGround) {
                    target.velocity = target.velocity.clone().setY(0.42)
                }

                if (dx == 0.0 && dz == 0.0) continue

                val dest = loc.clone().add(dx, 0.0, dz)
                dest.yaw   = loc.yaw
                dest.pitch = loc.pitch
                dest.y     = getGroundY(dest) ?: loc.y

                beingMoved.add(targetId)
                target.teleport(dest)
                beingMoved.remove(targetId)
            }
        }, 0L, 1L)
    }

    private fun stopTickTask() {
        if (tickTask != -1) {
            Bukkit.getScheduler().cancelTask(tickTask)
            tickTask = -1
        }
    }

    // ── Sichere Y-Koordinate ──────────────────────────────────────────────────

    private fun getGroundY(loc: Location): Double? {
        val world = loc.world ?: return null
        for (dy in listOf(0, -1, 1, -2)) {
            val check = loc.clone().add(0.0, dy.toDouble(), 0.0)
            val block = world.getBlockAt(check)
            val above = world.getBlockAt(check.clone().add(0.0, 1.0, 0.0))
            if (!block.isPassable && above.isPassable) return check.y + 1.0
        }
        return null
    }

    // ── Join / Quit ───────────────────────────────────────────────────────────

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        if (!isActive) return
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { shuffleAssignments() }, 20L)
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        if (!isActive) return
        val id = e.player.uniqueId
        controlMap.remove(id)
        controlMap.entries.removeIf { it.value == id }
        inputState.remove(id)
        beingMoved.remove(id)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { shuffleAssignments() }, 5L)
    }

    // ── An- / Abschalten ─────────────────────────────────────────────────────

    fun enable() {
        isActive = true
        shuffleAssignments()
        startTickTask()
        Bukkit.broadcast(
            Component.text("[SwapKeys] ", NamedTextColor.GOLD)
                .append(Component.text("Challenge aktiviert! Deine Tasten steuern jemand anderen.", NamedTextColor.GREEN))
        )
    }

    fun disable() {
        isActive = false
        stopTickTask()
        controlMap.clear()
        inputState.clear()
        beingMoved.clear()
        Bukkit.getOnlinePlayers().forEach {
            it.isSneaking  = false
            it.isSprinting = false
        }
        Bukkit.broadcast(
            Component.text("[SwapKeys] ", NamedTextColor.GOLD)
                .append(Component.text("Challenge deaktiviert.", NamedTextColor.RED))
        )
    }

    fun reset() = disable()
}