package io.github.black_Kittys22.challengekittys.AllArmorTrims

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.inventory.meta.ArmorMeta
import java.util.*

enum class TrimDifficulty { EASY, MID, HARD }

/**
 * Team-Sammel-Challenge für Rüstungs-Trims (Schmiedevorlagen).
 *
 * ZÄHLUNG: Eine Kombi zählt, sobald ein nicht-exemptierter Spieler ein
 * getrimmtes Rüstungsteil auf einen Rüstungsständer packt - UND SIE BLEIBT
 * NUR SOLANGE GÜLTIG, WIE SIE DORT AUSGESTELLT BLEIBT. Nimmt jemand das Teil
 * wieder runter, wird der Ständer zerstört, oder wird die Rüstung gegen etwas
 * anderes getauscht, verschwindet die Kombi wieder aus dem Fortschritt.
 * Fortschritt = Momentaufnahme der Welt, kein permanentes Log.
 *
 * Ein einzelner Rüstungsständer kann bis zu 4 Kombis gleichzeitig zählen
 * (Helm + Brust + Beine + Stiefel, je eigene Trim-Kombi).
 *
 * Es wird IMMER der volle, granulare Key gespeichert -
 * Format: "<RÜSTUNGSMATERIAL>_<TEIL>_<TRIMMATERIAL>_<TRIMPATTERN>"
 * z.B. "NETHERITE_HELMET_AMETHYST_DUNE"
 * Die drei Schwierigkeiten leiten ihren jeweiligen Pool + Fortschritt live
 * aus den aktuell ausgestellten Kombis ab - Wechsel der Schwierigkeit
 * verliert nichts, was gerade real in der Welt steht.
 *
 *  - HARD: jede (Rüstungsmaterial+Teil) × Trimmaterial × Trimpattern einzeln
 *  - MID:  nur Trimmaterial × Trimpattern zählt, Rüstungsteil/-material egal
 *  - EASY: eine gewählte Material-Stufe (Standard: Netherite), alle Teile
 *          dieser Stufe × alle Pattern, Trimmaterial-Farbe ist egal
 *          (Sonderfall "TURTLE": nur der Schildkrötenhelm, 1 Teil)
 *
 * EINSCHRÄNKUNG: Für Ständer in entladenen Chunks kann nicht laufend geprüft
 * werden, ob die Rüstung noch dran ist - dafür müssten Chunks künstlich
 * geladen gehalten werden. Die Live-Prüfung greift zuverlässig bei allem,
 * was per Spielerinteraktion passiert (sofort) oder in geladenen Chunks
 * periodisch nachgeprüft wird. Zerstörung eines Ständers durch Explosion
 * o.ä. sollte i.d.R. trotzdem EntityDeathEvent auslösen.
 */
class AllArmorTrims(private val plugin: Main) : Listener {

    var difficulty: TrimDifficulty = TrimDifficulty.EASY
        private set

    var easyTier: String = "NETHERITE"
        private set

    private val armorMaterials = listOf("LEATHER", "CHAINMAIL", "IRON", "GOLDEN", "DIAMOND", "NETHERITE")
    private val pieces = listOf("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS")

    private val trimPatterns = listOf(
        "SENTRY", "DUNE", "COAST", "WILD", "WARD", "EYE", "VEX", "TIDE", "SNOUT",
        "RIB", "SPIRE", "WAYFINDER", "SHAPER", "SILENCE", "RAISER", "HOST", "FLOW", "BOLT"
    )
    private val trimMaterials = listOf(
        "QUARTZ", "IRON", "NETHERITE", "REDSTONE", "COPPER", "GOLD", "EMERALD", "DIAMOND", "LAPIS", "AMETHYST"
    )

    private val armorBaseKeys: List<Pair<String, String>> by lazy {
        val list = mutableListOf<Pair<String, String>>()
        armorMaterials.forEach { mat -> pieces.forEach { piece -> list.add(mat to piece) } }
        list.add("TURTLE" to "HELMET")
        list
    }

    // composite key "<StandUUID>:<SLOT>" -> voller Trim-Key, nur solange die Rüstung wirklich dort hängt
    private val displayedByStand = mutableMapOf<String, String>()

    private fun completedFull(): List<String> = displayedByStand.values.toList()

    private var currentTarget: String? = null

    private val bossBarName: BossBar = BossBar.bossBar(
        Component.text("Lade Rüstungs-Trims..."),
        1.0f,
        BossBar.Color.PURPLE,
        BossBar.Overlay.PROGRESS
    )

    private var isReady = false

    init {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            loadProgress()
            isReady = true
            if (plugin.isArmorTrimsChallengeActive) refreshBarForAll()
            // Alle 5 Minuten prüfen, ob ausgestellte Kombis (in geladenen Chunks) noch stimmen
            Bukkit.getScheduler().runTaskTimer(plugin, Runnable { reconcileLoadedStands() }, 6000L, 6000L)
        }, 5L)
    }

    // ── Pool-Ableitung je Schwierigkeit ─────────────────────────────────────

    private fun fullKey(armorMat: String, piece: String, trimMat: String, pattern: String) =
        "${armorMat}_${piece}_${trimMat}_${pattern}"

    private fun piecesForTier(tier: String): List<String> =
        if (tier == "TURTLE") listOf("HELMET") else pieces

    private fun poolFor(diff: TrimDifficulty): List<String> = when (diff) {
        TrimDifficulty.HARD -> armorBaseKeys.flatMap { (mat, piece) ->
            trimMaterials.flatMap { tm -> trimPatterns.map { p -> fullKey(mat, piece, tm, p) } }
        }
        TrimDifficulty.MID -> trimMaterials.flatMap { tm -> trimPatterns.map { p -> "${tm}_${p}" } }
        TrimDifficulty.EASY -> piecesForTier(easyTier).flatMap { piece -> trimPatterns.map { p -> "${piece}_${p}" } }
    }

    private fun derive(fullKeyStr: String, diff: TrimDifficulty): String? {
        val parts = fullKeyStr.split("_")
        if (parts.size != 4) return null
        val (armorMat, piece, trimMat, pattern) = parts
        return when (diff) {
            TrimDifficulty.HARD -> fullKeyStr
            TrimDifficulty.MID -> "${trimMat}_${pattern}"
            TrimDifficulty.EASY -> if (armorMat == easyTier) "${piece}_${pattern}" else null
        }
    }

    private fun completedDerivedSet(diff: TrimDifficulty): Set<String> =
        completedFull().mapNotNull { derive(it, diff) }.toSet()

    fun getProgressString(): String {
        val pool = poolFor(difficulty)
        val done = completedDerivedSet(difficulty).count { it in pool }
        return "$done/${pool.size} (${difficulty.name})"
    }

    // ── Schwierigkeit wechseln ───────────────────────────────────────────────

    fun cycleDifficulty() {
        difficulty = when (difficulty) {
            TrimDifficulty.EASY -> TrimDifficulty.MID
            TrimDifficulty.MID -> TrimDifficulty.HARD
            TrimDifficulty.HARD -> TrimDifficulty.EASY
        }
        selectNextTarget()
    }

    fun setEasyTier(material: String) {
        val m = material.uppercase()
        if (m !in armorMaterials && m != "TURTLE") return
        easyTier = m
        if (difficulty == TrimDifficulty.EASY) selectNextTarget()
        saveProgress()
    }

    // ── Erkennung: Rüstung wird auf Ständer gepackt/entfernt ────────────────

    @EventHandler
    fun onArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        if (!plugin.isArmorTrimsChallengeActive || !isReady) return
        val player = event.player
        if (plugin.exemptPlayers.contains(player.uniqueId)) return

        val stand = event.rightClicked
        // Der eigentliche Tausch passiert erst NACH diesem Event (falls nicht
        // gecancelt) - also einen Tick warten, dann den echten Zustand lesen
        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (!stand.isValid) return@Runnable
            updateStandSlots(stand, player)
        })
    }

    @EventHandler
    fun onStandDestroyed(event: EntityDeathEvent) {
        val entity = event.entity
        if (entity !is ArmorStand) return
        removeStandEntries(entity.uniqueId.toString())
    }

    private fun updateStandSlots(stand: ArmorStand, actor: Player) {
        val standId = stand.uniqueId.toString()
        val equipment = stand.equipment ?: return

        val slots = linkedMapOf(
            "HEAD" to (equipment.helmet to "HELMET"),
            "CHEST" to (equipment.chestplate to "CHESTPLATE"),
            "LEGS" to (equipment.leggings to "LEGGINGS"),
            "FEET" to (equipment.boots to "BOOTS")
        )

        var changed = false
        var completedNow: String? = null

        for ((slotName, pair) in slots) {
            val (item, expectedPiece) = pair
            val compositeKey = "$standId:$slotName"

            val meta = item?.itemMeta as? ArmorMeta
            val trim = meta?.trim
            if (item == null || item.type == Material.AIR || trim == null) {
                if (displayedByStand.remove(compositeKey) != null) changed = true
                continue
            }

            val matParts = item.type.name.split("_", limit = 2)
            if (matParts.size != 2 || matParts[1] != expectedPiece) {
                if (displayedByStand.remove(compositeKey) != null) changed = true
                continue
            }

            val full = fullKey(matParts[0], expectedPiece, trim.material.key.key.uppercase(), trim.pattern.key.key.uppercase())

            if (displayedByStand[compositeKey] != full) {
                displayedByStand[compositeKey] = full
                changed = true
                val derived = derive(full, difficulty)
                if (derived != null && derived == currentTarget) completedNow = full
            }
        }

        if (!changed) return

        if (completedNow != null) {
            broadcastSuccess(actor, currentTarget!!)
            selectNextTarget()
            plugin.updateTablist()
        } else {
            refreshBarForAll()
            saveProgress()
        }
    }

    private fun removeStandEntries(standId: String) {
        val prefix = "$standId:"
        val toRemove = displayedByStand.keys.filter { it.startsWith(prefix) }
        if (toRemove.isEmpty()) return
        toRemove.forEach { displayedByStand.remove(it) }
        refreshBarForAll()
        saveProgress()
    }

    /**
     * Sicherheitsnetz: prüft alle aktuell GELADENEN Ständer erneut, ob die
     * getrackte Kombi wirklich noch dranhängt (fängt z.B. Entnahme per
     * Dispenser/Hopper/anderem Plugin ohne PlayerArmorStandManipulateEvent
     * ab). Ständer in entladenen Chunks werden NICHT angefasst, um keine
     * Fehlalarme durch "nicht geladen" statt "wirklich weg" zu erzeugen.
     */
    private fun reconcileLoadedStands() {
        val standIds = displayedByStand.keys.map { it.substringBefore(":") }.distinct()
        var changed = false

        for (idStr in standIds) {
            val uuid = try { UUID.fromString(idStr) } catch (e: IllegalArgumentException) { continue }
            val entity = Bukkit.getEntity(uuid)
            if (entity !is ArmorStand || !entity.isValid) continue // nicht geladen -> nicht anfassen

            val equipment = entity.equipment ?: continue
            val slots = linkedMapOf(
                "HEAD" to (equipment.helmet to "HELMET"),
                "CHEST" to (equipment.chestplate to "CHESTPLATE"),
                "LEGS" to (equipment.leggings to "LEGGINGS"),
                "FEET" to (equipment.boots to "BOOTS")
            )

            for ((slotName, pair) in slots) {
                val compositeKey = "$idStr:$slotName"
                val tracked = displayedByStand[compositeKey] ?: continue
                val (item, expectedPiece) = pair

                val meta = item?.itemMeta as? ArmorMeta
                val trim = meta?.trim
                val stillValid = item != null && item.type != Material.AIR && trim != null && run {
                    val matParts = item.type.name.split("_", limit = 2)
                    matParts.size == 2 && matParts[1] == expectedPiece &&
                            fullKey(matParts[0], expectedPiece, trim.material.key.key.uppercase(), trim.pattern.key.key.uppercase()) == tracked
                }

                if (!stillValid) {
                    displayedByStand.remove(compositeKey)
                    changed = true
                }
            }
        }

        if (changed) {
            refreshBarForAll()
            saveProgress()
        }
    }

    fun selectNextTarget() {
        val pool = poolFor(difficulty)
        val done = completedDerivedSet(difficulty)
        val remaining = pool.filter { it !in done }

        currentTarget = if (remaining.isEmpty()) {
            broadcastChallengeComplete()
            null
        } else {
            remaining.random()
        }

        refreshBarForAll()
        saveProgress()
    }

    private fun refreshBarForAll() {
        val pool = poolFor(difficulty)
        val done = completedDerivedSet(difficulty).count { it in pool }
        val name = currentTarget?.let { formatName(it) } ?: "ALLE TRIMS AUSGESTELLT!"

        bossBarName.name(
            Component.text("", NamedTextColor.WHITE)
                .append(Component.text("⛨ $name", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text("  ($done/${pool.size} · ${difficulty.name})", NamedTextColor.GRAY))
        )
        bossBarName.progress(1.0f)

        Bukkit.getOnlinePlayers().forEach { it.showBossBar(bossBarName) }
    }

    private fun broadcastSuccess(player: Player, key: String) {
        val name = formatName(key)
        val msg = Component.text("", NamedTextColor.LIGHT_PURPLE)
            .append(Component.text(player.name, NamedTextColor.YELLOW))
            .append(Component.text(" hat ausgestellt: ", NamedTextColor.GRAY))
            .append(Component.text(name, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
            .append(Component.text(" – zählt fürs ganze Team, solange es dort hängt!", NamedTextColor.GRAY))
        Bukkit.broadcast(msg)
        Bukkit.getOnlinePlayers().forEach { it.playSound(it.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.2f) }
    }

    private fun broadcastChallengeComplete() {
        Bukkit.broadcast(
            Component.text(
                "!!! ALLE RÜSTUNGS-TRIMS (${difficulty.name}) SIND AUSGESTELLT !!!",
                NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD
            )
        )
        Bukkit.getOnlinePlayers().forEach { it.playSound(it.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f) }
    }

    private fun formatName(key: String): String =
        key.lowercase(Locale.getDefault())
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase(Locale.getDefault()) } }

    // ── Persistenz ────────────────────────────────────────────────────────

    fun saveProgress() {
        val serialized = displayedByStand.entries.map { (composite, full) -> "$composite=$full" }
        plugin.config.set("challenges.allArmorTrims.displayed", serialized)
        plugin.config.set("challenges.allArmorTrims.currentTarget", currentTarget)
        plugin.config.set("challenges.allArmorTrims.difficulty", difficulty.name)
        plugin.config.set("challenges.allArmorTrims.easyTier", easyTier)
        plugin.saveConfig()
    }

    private fun loadProgress() {
        displayedByStand.clear()
        plugin.config.getStringList("challenges.allArmorTrims.displayed").forEach { line ->
            val idx = line.indexOf('=')
            if (idx > 0) {
                displayedByStand[line.substring(0, idx)] = line.substring(idx + 1)
            }
        }

        easyTier = plugin.config.getString("challenges.allArmorTrims.easyTier") ?: "NETHERITE"
        difficulty = try {
            TrimDifficulty.valueOf(plugin.config.getString("challenges.allArmorTrims.difficulty") ?: "EASY")
        } catch (e: Exception) { TrimDifficulty.EASY }

        val savedTarget = plugin.config.getString("challenges.allArmorTrims.currentTarget")
        currentTarget = savedTarget?.takeIf { it in poolFor(difficulty) }

        if (currentTarget == null) {
            selectNextTarget()
        } else {
            refreshBarForAll()
        }
    }

    fun reset() {
        displayedByStand.clear()
        hideBar()
        selectNextTarget()
    }

    fun showBar(player: Player) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { refreshBarForAll() }, 2L)
    }

    fun hideBar() {
        Bukkit.getOnlinePlayers().forEach { it.hideBossBar(bossBarName) }
    }
}