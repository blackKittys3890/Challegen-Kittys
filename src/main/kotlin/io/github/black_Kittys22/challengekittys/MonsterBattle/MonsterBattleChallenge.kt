package io.github.black_Kittys22.challengekittys.MonsterBattle

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.scheduler.BukkitTask
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class MonsterBattleChallenge(private val plugin: Main) : Listener {

    private val killedMonsters = mutableMapOf<UUID, MutableList<EntityType>>()
    private val playerEnemiesToKill = mutableMapOf<UUID, MutableSet<UUID>>()
    private val playerPendingWaves = mutableMapOf<UUID, MutableList<EntityType>>()
    private val playerTimes = mutableMapOf<UUID, Int>()
    private val placedBlocks = mutableListOf<Location>()
    private val playerQueue = mutableListOf<UUID>()
    private var currentPlayer: UUID? = null

    var isFarmingPhase = false
    var isArenaPhase = false
    private var countdownTask: BukkitTask? = null
    private val entitiesPerWave = 5

    fun startChallenge(durationMinutes: Int) {
        this.reset()
        this.isFarmingPhase = true
        plugin.timer.startCountdown(durationMinutes * 60)

        Bukkit.broadcast(Component.text("═════════════════════════", NamedTextColor.GOLD))
        Bukkit.broadcast(Component.text("MONSTER BATTLE GESTARTET", NamedTextColor.RED))
        Bukkit.broadcast(Component.text("Tötet alles! Die Kills spawnen beim Gegner!", NamedTextColor.YELLOW))
        Bukkit.broadcast(Component.text("═════════════════════════", NamedTextColor.GOLD))

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (!isFarmingPhase) {
                countdownTask?.cancel()
                return@Runnable
            }
            if (plugin.timer.timeSeconds <= 0) {
                isFarmingPhase = false
                countdownTask?.cancel()
                startShowdown()
            }
        }, 20L, 20L)
    }

    private fun startShowdown() {
        isArenaPhase = true
        playerQueue.clear()
        playerQueue.addAll(Bukkit.getOnlinePlayers().map { it.uniqueId })
        playerTimes.clear()

        Bukkit.getOnlinePlayers().forEach { it.gameMode = GameMode.SPECTATOR }
        nextPlayerTurn()
    }

    private fun nextPlayerTurn() {
        cleanUpArena()

        if (playerQueue.isEmpty()) {
            showFinalResults()
            stopChallenge()
            return
        }

        val nextId = playerQueue.removeAt(0)
        currentPlayer = nextId
        val player = Bukkit.getPlayer(nextId) ?: run { nextPlayerTurn(); return }

        plugin.timer.maxTime = 0
        plugin.timer.timeSeconds = 0
        plugin.timer.paused = false

        // --- FIXED CONFIG LOADING ---
        val worldName = plugin.config.getString("monsterbattle.arena_world_name") ?: "world"
        val world = Bukkit.getWorld(worldName) ?: Bukkit.getWorlds()[0]

        val coordString = plugin.config.getString("monsterbattle.spawn_coords") ?: "0,100,0"
        val parts = coordString.split(",")
        val spawnLoc = if (parts.size >= 3) {
            Location(world, parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble())
        } else {
            world.spawnLocation
        }

        player.gameMode = GameMode.SURVIVAL
        player.teleport(spawnLoc)

        val opponentMobs = mutableListOf<EntityType>()
        killedMonsters.forEach { (uuid, mobs) ->
            if (uuid != player.uniqueId) opponentMobs.addAll(mobs)
        }
        playerPendingWaves[player.uniqueId] = opponentMobs

        Bukkit.broadcast(Component.text("Kampf beginnt: ", NamedTextColor.WHITE)
            .append(Component.text(player.name, NamedTextColor.GOLD)))

        spawnNextWave(player)
    }

    private fun spawnNextWave(player: Player) {
        if (!isArenaPhase || currentPlayer != player.uniqueId) return
        val pending = playerPendingWaves[player.uniqueId] ?: return

        if (pending.isEmpty()) {
            val finalTime = plugin.timer.timeSeconds
            playerTimes[player.uniqueId] = finalTime
            plugin.timer.paused = true

            Bukkit.broadcast(Component.text("${player.name} fertig! Zeit: ${finalTime}s", NamedTextColor.GREEN))
            player.gameMode = GameMode.SPECTATOR
            Bukkit.getScheduler().runTaskLater(plugin, Runnable { nextPlayerTurn() }, 60L)
            return
        }

        val center = player.location
        val radius = 8.0
        val currentWave = pending.take(entitiesPerWave)
        repeat(currentWave.size) { pending.removeAt(0) }

        currentWave.forEachIndexed { index, type ->
            val angle = 2 * Math.PI * index / currentWave.size
            val spawnLoc = center.clone().add(radius * cos(angle), 1.0, radius * sin(angle))
            val entity = center.world.spawnEntity(spawnLoc, type)
            if (entity is Mob) entity.target = player
            playerEnemiesToKill.getOrPut(player.uniqueId) { mutableSetOf() }.add(entity.uniqueId)
        }
    }

    private fun showFinalResults() {
        Bukkit.broadcast(Component.text("═══ ERGEBNISSE ═══", NamedTextColor.GOLD))
        playerTimes.toList().sortedBy { it.second }.forEachIndexed { index, pair ->
            val pName = Bukkit.getPlayer(pair.first)?.name ?: "Unbekannt"
            Bukkit.broadcast(Component.text("${index + 1}. $pName: ${pair.second}s", NamedTextColor.YELLOW))
        }
        Bukkit.broadcast(Component.text("══════════════════", NamedTextColor.GOLD))
    }

    @EventHandler
    fun onMobContract(event: EntityDeathEvent) {
        if (!isFarmingPhase || plugin.timer.paused) return
        val killer = event.entity.killer ?: return
        if (event.entity !is Player) {
            addKilledMonster(killer.uniqueId, event.entity.type)
        }
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        if (!isArenaPhase) return
        val ownerId = currentPlayer ?: return
        val enemies = playerEnemiesToKill[ownerId] ?: return
        if (enemies.contains(event.entity.uniqueId)) {
            enemies.remove(event.entity.uniqueId)
            if (enemies.isEmpty()) Bukkit.getPlayer(ownerId)?.let { spawnNextWave(it) }
        }
    }

    // --- TOD IST JETZT EGAL ---
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!isArenaPhase || event.entity.uniqueId != currentPlayer) return

        // Wir lassen den Timer laufen!
        val player = event.entity

        // Wir teleportieren ihn nach dem Respawn einfach zurück
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val worldName = plugin.config.getString("monsterbattle.arena_world_name") ?: "world"
            val coordString = plugin.config.getString("monsterbattle.spawn_coords") ?: "0,100,0"
            val parts = coordString.split(",")
            val loc = Location(Bukkit.getWorld(worldName), parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble())

            player.spigot().respawn() // Auto-Respawn (falls Spigot) oder einfach teleport
            player.teleport(loc)
            player.sendMessage(Component.text("Beeil dich! Die Zeit läuft weiter!", NamedTextColor.RED))
        }, 1L)
    }

    fun stopChallenge() {
        isFarmingPhase = false
        isArenaPhase = false
        countdownTask?.cancel()
        currentPlayer = null
        plugin.timer.paused = true
        cleanUpArena()
        Bukkit.getOnlinePlayers().forEach { it.gameMode = GameMode.SURVIVAL }
    }

    private fun cleanUpArena() {
        placedBlocks.forEach { it.block.type = Material.AIR }
        placedBlocks.clear()
        val worldName = plugin.config.getString("monsterbattle.arena_world_name") ?: "world"
        val world = Bukkit.getWorld(worldName) ?: return
        world.entities.forEach { if (it is Item || it is ExperienceOrb || (it is Mob && it !is Player)) it.remove() }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (!isArenaPhase) return
        if (event.block.world.name == plugin.config.getString("monsterbattle.arena_world_name")) {
            placedBlocks.add(event.block.location)
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!isArenaPhase) return
        if (event.block.world.name == plugin.config.getString("monsterbattle.arena_world_name")) {
            event.isCancelled = true
        }
    }

    fun addKilledMonster(player: UUID, type: EntityType) = killedMonsters.getOrPut(player) { mutableListOf() }.add(type)
    fun getKilledMonsterCount(player: UUID): Int = killedMonsters[player]?.size ?: 0

    fun reset() {
        killedMonsters.clear()
        playerEnemiesToKill.clear()
        playerPendingWaves.clear()
        playerTimes.clear()
        placedBlocks.clear()
        isFarmingPhase = false
        isArenaPhase = false
        countdownTask?.cancel()
    }
}