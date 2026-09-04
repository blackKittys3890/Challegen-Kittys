package io.github.black_Kittys22.challengekittys.Battles.MobForceBattle

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import java.time.Duration
import java.util.*

// ─── Datenklasse für ein Team ────────────────────────────────────────────────
data class MobForceTeam(
    val id: String,
    val displayName: String,
    val color: NamedTextColor,
    val members: MutableSet<UUID> = mutableSetOf(),
    var jokers: Int = 4,
    var currentIndex: Int = 0,
    var mobList: MutableList<EntityType> = mutableListOf(),
    var finished: Boolean = false
)

// ─── Manager ─────────────────────────────────────────────────────────────────
class MobForceBattleManager(private val plugin: Main) {

    val teams = mutableMapOf<String, MobForceTeam>()
    private val soloPlayers = mutableMapOf<UUID, MobForceTeam>()

    var isActive = false
    var timeLimitSeconds: Int = 0

    fun formatTime(seconds: Int): String {
        val s = maxOf(seconds, 0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, sec)
        else String.format("%d:%02d", m, sec)
    }

    // Alle killbaren Mobs
    val allMobs: List<EntityType> = listOf(
        EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
        EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.ENDERMAN,
        EntityType.WITCH, EntityType.BLAZE, EntityType.WITHER_SKELETON,
        EntityType.GHAST, EntityType.MAGMA_CUBE, EntityType.SLIME,
        EntityType.DROWNED, EntityType.HUSK, EntityType.STRAY,
        EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.RAVAGER,
        EntityType.PHANTOM, EntityType.ELDER_GUARDIAN, EntityType.GUARDIAN,
        EntityType.SHULKER, EntityType.ENDERMITE, EntityType.SILVERFISH,
        EntityType.PIGLIN_BRUTE, EntityType.HOGLIN, EntityType.ZOGLIN,
        EntityType.WARDEN, EntityType.ENDER_DRAGON, EntityType.WITHER
    )

    // ─── Team-Verwaltung ──────────────────────────────────────────────────────
    fun createTeam(id: String, displayName: String, color: NamedTextColor): Boolean {
        if (teams.containsKey(id.lowercase())) return false
        teams[id.lowercase()] = MobForceTeam(id.lowercase(), displayName, color)
        return true
    }

    fun deleteTeam(id: String): Boolean = teams.remove(id.lowercase()) != null

    fun addPlayerToTeam(uuid: UUID, teamId: String): Boolean {
        val team = teams[teamId.lowercase()] ?: return false
        removePlayerFromAllTeams(uuid)
        soloPlayers.remove(uuid)
        team.members.add(uuid)
        return true
    }

    fun removePlayerFromAllTeams(uuid: UUID) {
        teams.values.forEach { it.members.remove(uuid) }
    }

    fun getTeamOf(uuid: UUID): MobForceTeam? =
        teams.values.firstOrNull { it.members.contains(uuid) } ?: soloPlayers[uuid]

    fun registerSoloPlayer(uuid: UUID): MobForceTeam =
        soloPlayers.getOrPut(uuid) {
            MobForceTeam(
                id = uuid.toString(),
                displayName = Bukkit.getOfflinePlayer(uuid).name ?: "Unbekannt",
                color = NamedTextColor.WHITE,
                mobList = allMobs.shuffled().toMutableList()
            )
        }

    // ─── Challenge starten – nutzt den bestehenden plugin.timer ──────────────
    fun start(timeLimitSecs: Int = 0) {
        isActive = true
        timeLimitSeconds = timeLimitSecs

        teams.values.forEach { t ->
            t.mobList = allMobs.shuffled().toMutableList()
            t.currentIndex = 0
            t.jokers = 4
            t.finished = false
        }
        soloPlayers.values.forEach { s ->
            s.mobList = allMobs.shuffled().toMutableList()
            s.currentIndex = 0
            s.jokers = 4
            s.finished = false
        }

        // Bestehenden Timer nutzen
        if (timeLimitSecs > 0) {
            plugin.timer.startCountdown(timeLimitSecs)
        } else {
            plugin.timer.paused = false
        }

        broadcastStart(timeLimitSecs)
    }

    fun stop() {
        isActive = false
        plugin.timer.paused = true
        broadcastStop()
    }

    fun reset() {
        isActive = false
        teams.clear()
        soloPlayers.clear()
    }

    // ─── Wird vom Timer aufgerufen wenn Zeit abläuft ──────────────────────────
    fun onTimeUp() {
        if (!isActive) return
        isActive = false

        Bukkit.broadcast(Component.text("══════════════════════════", NamedTextColor.DARK_RED, TextDecoration.BOLD))
        Bukkit.broadcast(Component.text("  ⏱ MobForceBattle beendet!", NamedTextColor.RED, TextDecoration.BOLD))
        Bukkit.broadcast(Component.text("══════════════════════════", NamedTextColor.DARK_RED, TextDecoration.BOLD))

        val spawn = Bukkit.getWorlds()[0].spawnLocation

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            Bukkit.getOnlinePlayers().forEach { p ->
                p.teleport(spawn)
                p.showTitle(
                    Title.title(
                        Component.text("Zeit abgelaufen!", NamedTextColor.RED, TextDecoration.BOLD),
                        Component.text("Rangliste öffnet sich...", NamedTextColor.GRAY),
                        Title.Times.times(
                            Duration.ofMillis(500),
                            Duration.ofSeconds(3),
                            Duration.ofSeconds(1)
                        )
                    )
                )
            }
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                // Ranking vorbereiten – Spieler öffnen GUI per /mobforce reveal
                MobForceRankingGUI.prepare(plugin, this)
                Bukkit.broadcast(
                    Component.text("[MobForceBattle] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("Nutzt ", NamedTextColor.WHITE))
                        .append(Component.text("/mobforce reveal", NamedTextColor.YELLOW, TextDecoration.BOLD))
                        .append(Component.text(" um die Rangliste zu enthüllen!", NamedTextColor.WHITE))
                )
            }, 40L)
        }, 20L)
    }

    // ─── Mob-Name für Actionbar (wird vom Timer abgefragt) ────────────────────
    fun getMobDisplayForPlayer(uuid: UUID): String? {
        if (!isActive) return null
        val team = getTeamOf(uuid) ?: return null
        if (team.finished) return "✔ Fertig!"
        val mob = currentMob(team) ?: return null
        return mob.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    // ─── Mob / Joker Logik ────────────────────────────────────────────────────
    fun currentMob(team: MobForceTeam): EntityType? {
        if (team.finished || team.currentIndex >= team.mobList.size) return null
        return team.mobList[team.currentIndex]
    }

    fun advanceTeam(team: MobForceTeam) {
        team.currentIndex++
        if (team.currentIndex >= team.mobList.size) {
            team.finished = true
            Bukkit.broadcast(
                Component.text("[MobForceBattle] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .append(Component.text("Team ", NamedTextColor.WHITE))
                    .append(Component.text(team.displayName, team.color, TextDecoration.BOLD))
                    .append(Component.text(" hat alle Mobs besiegt! 🏆", NamedTextColor.YELLOW))
            )
        } else {
            val next = team.mobList[team.currentIndex]
            // Nur Teammitglieder informieren
            val mobName = next.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
            val msg = Component.text("[MobForceBattle] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text("Nächstes Ziel: ", NamedTextColor.GRAY))
                .append(Component.text(mobName, NamedTextColor.RED, TextDecoration.BOLD))

            if (team.members.isNotEmpty()) {
                team.members.mapNotNull { Bukkit.getPlayer(it) }.forEach { it.sendMessage(msg) }
            } else {
                // Solo: direkt kein Broadcast nötig, Actionbar zeigt es
            }
        }
    }

    fun useJoker(team: MobForceTeam): Boolean {
        if (team.jokers <= 0 || team.finished || team.currentIndex >= team.mobList.size) return false
        team.jokers--
        val skipped = team.mobList[team.currentIndex]
        team.currentIndex++
        val skippedName = skipped.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }

        val msg = Component.text("[MobForceBattle] ", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text("Joker! (${team.jokers} übrig) ", NamedTextColor.YELLOW))
            .append(Component.text(skippedName, NamedTextColor.GRAY))
            .append(Component.text(" übersprungen.", NamedTextColor.GRAY))
        Bukkit.broadcast(msg)

        val next = if (team.currentIndex < team.mobList.size) team.mobList[team.currentIndex] else null
        if (next == null) { team.finished = true }
        return true
    }

    // ─── Rangliste ────────────────────────────────────────────────────────────
    fun getRanking(): List<MobForceTeam> {
        val all = teams.values.toList() + soloPlayers.values.toList()
        return all.sortedWith(
            compareByDescending<MobForceTeam> { it.finished }
                .thenByDescending { it.currentIndex }
        )
    }

    // ─── Status ───────────────────────────────────────────────────────────────
    fun buildStatusComponent(): Component {
        var comp = Component.text("══ MobForceBattle Status ══\n", NamedTextColor.GOLD, TextDecoration.BOLD)
        val all = (teams.values.toList() + soloPlayers.values.toList()).sortedByDescending { it.currentIndex }
        if (all.isEmpty()) return comp.append(Component.text("  Keine Teams registriert.", NamedTextColor.GRAY))
        all.forEach { t ->
            val mobName = if (t.finished) "✔ Fertig!"
            else currentMob(t)?.name?.lowercase()?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "?"
            comp = comp
                .append(Component.text("  [", NamedTextColor.DARK_GRAY))
                .append(Component.text(t.displayName, if (t.finished) NamedTextColor.GREEN else t.color, TextDecoration.BOLD))
                .append(Component.text("] Ziel: ", NamedTextColor.GRAY))
                .append(Component.text(mobName, NamedTextColor.RED))
                .append(Component.text("  Joker: ${t.jokers}/4  ${t.currentIndex}/${t.mobList.size}\n", NamedTextColor.GRAY))
        }
        return comp
    }

    private fun broadcastStart(timeLimitSecs: Int) {
        Bukkit.broadcast(Component.text("═══════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
        Bukkit.broadcast(Component.text("  MobForceBattle gestartet!", NamedTextColor.YELLOW, TextDecoration.BOLD))
        if (timeLimitSecs > 0) {
            val h = timeLimitSecs / 3600; val m = (timeLimitSecs % 3600) / 60; val s = timeLimitSecs % 60
            val timeStr = if (h > 0) "${h}h ${m}m" else if (m > 0) "${m}m ${s}s" else "${s}s"
            Bukkit.broadcast(Component.text("  Zeit: $timeStr", NamedTextColor.WHITE))
        }
        Bukkit.broadcast(Component.text("═══════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))

        // Erstes Ziel an alle Teammigleider senden
        teams.values.forEach { team ->
            val first = currentMob(team) ?: return@forEach
            val mobName = first.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
            team.members.mapNotNull { Bukkit.getPlayer(it) }.forEach { p ->
                p.sendMessage(
                    Component.text("[MobForceBattle] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("Euer erstes Ziel: ", NamedTextColor.WHITE))
                        .append(Component.text(mobName, NamedTextColor.RED, TextDecoration.BOLD))
                )
            }
        }
    }

    private fun broadcastStop() {
        Bukkit.broadcast(Component.text("[MobForceBattle] Challenge gestoppt.", NamedTextColor.RED, TextDecoration.BOLD))
    }
}