package io.github.black_Kittys22.challengekittys.ChainedTogether

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Bat
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

/**
 * ═══════════════════════════════════════════════════════════════
 *  CHAINED TOGETHER CHALLENGE
 * ═══════════════════════════════════════════════════════════════
 *
 *  Wird über das Challenge-GUI gestartet/gestoppt – kein Command.
 *  Bei Join/Leave wird die Kette automatisch neu zusammengestellt
 *  und alle Spieler werden über ihre neue Rolle informiert.
 *
 *  REGELN:
 *  • Nur P1 (Leader) darf sich bewegen.
 *  • Alle anderen werden hinter P1 hergezogen.
 *  • Jeder Spieler hat genau EINE Aufgabe – alles andere ist gesperrt.
 *
 *  Slot 0 – Leader:   Laufen + Angreifen
 *  Slot 1 – Abbauer:  Abbauen + Platzieren
 *  Slot 2 – Händler:  Nur traden (Villager/Wandering Trader)
 *  Slot 3 – Esser:    Nur essen
 *  Slot 4 – Heiler:   Nur Tränke benutzen / Heilen (Items aufheben darf jeder)
 *  Slot 5 – Crafter:  Nur craften
 *  Slot 6 – Manager:  Nur Inventar & Kisten verwalten
 *  Slot 7 – Schütze:  Nur Fernkampf (Bogen/Armbrust)
 *
 *  Items aufheben darf JEDER – kein Spieler ist dafür exklusiv zuständig.
 *  Bei weniger als 8 Spielern werden die Slots gleichmäßig verteilt.
 *  Geteilte Herzen + Hunger immer aktiv.
 *  Geteiltes Inventar optional (isSharedInventory).
 * ═══════════════════════════════════════════════════════════════
 */
class ChainedTogetherChallenge(private val plugin: Main) : Listener {

    var isActive = false
    var isSharedInventory = false

    // Feste Reihenfolge – Index = Position in der Kette
    private val playerOrder = mutableListOf<UUID>()

    // ── Ketten-Parameter ─────────────────────────────────────────────────────
    // MAX_DIST   = maximaler Abstand bevor beide Spieler zur Mitte gezogen werden
    // HARD_DIST  = ab hier sofort-Teleport (z.B. nach Tod / Ladescreen)
    // PULL       = Velocity-Stärke pro Tick wenn Kette gespannt ist
    private val MAX_DIST   = 5.0
    private val HARD_DIST  = 15.0
    private val PULL       = 0.35
    private var taskId     = -1

    private var syncingHealth = false
    private var syncingFood = false

    // ── Slot-Verteilung bei n Spielern ───────────────────────────────────────
    //
    //  Immer: Slot 0 = Leader (P1)
    //  Die restlichen Slots werden so gleichmäßig wie möglich über
    //  die übrigen Spieler verteilt.
    //
    //  Beispiel 3 Spieler → Slots 0, 1, 3  (Leader, Abbauer, Esser)
    //  Beispiel 4 Spieler → Slots 0, 1, 2, 3
    //  Beispiel 5 Spieler → Slots 0, 1, 2, 3, 4  usw.
    //
    // Feste Rollenzuweisung pro Run (überschreibt automatische Verteilung)
    // UUID → Slot-Index, wird in config gespeichert und beim Reset geleert
    val fixedRoles = mutableMapOf<UUID, Int>()

    private val ROLE_DISTRIBUTION = mapOf(
        1 to listOf(0),
        2 to listOf(0, 1),
        3 to listOf(0, 1, 3),
        4 to listOf(0, 1, 2, 3),
        5 to listOf(0, 1, 2, 3, 4),
        6 to listOf(0, 1, 2, 3, 4, 5),
        7 to listOf(0, 1, 2, 3, 4, 5, 6)
    )

    fun getSlot(uuid: UUID): Int {
        // Feste Zuweisung hat IMMER Vorrang — auch vor Position in der Kette
        fixedRoles[uuid]?.let { return it }

        // Keine feste Zuweisung → automatisch nach Position verteilen
        val pos = playerOrder.indexOf(uuid)
        if (pos == -1) return -1
        val total = playerOrder.size.coerceAtMost(8)
        return ROLE_DISTRIBUTION[total]?.getOrNull(pos) ?: pos
    }

    /** Setzt eine feste Rolle für einen Spieler (bleibt bis Reset). */
    fun assignRole(uuid: UUID, slot: Int) {
        fixedRoles[uuid] = slot
        saveFixedRoles()
    }

    /** Entfernt die feste Rollenzuweisung für einen Spieler. */
    fun clearRole(uuid: UUID) {
        fixedRoles.remove(uuid)
        saveFixedRoles()
    }

    /** Entfernt alle festen Rollenzuweisungen (beim Reset). */
    fun resetFixedRoles() {
        fixedRoles.clear()
        saveFixedRoles()
    }

    fun saveFixedRoles() {
        val section = plugin.config
        section.set("chain.fixedRoles", fixedRoles.entries.associate {
            it.key.toString() to it.value
        })
        plugin.saveConfig()
    }

    fun loadFixedRoles() {
        fixedRoles.clear()
        val map = plugin.config.getConfigurationSection("chain.fixedRoles") ?: return
        for (key in map.getKeys(false)) {
            runCatching {
                fixedRoles[java.util.UUID.fromString(key)] = map.getInt(key)
            }
        }
    }

    /**
     * Gibt zurück ob ein Spieler eine bestimmte Aktion (Slot) ausführen darf.
     *
     * Erlaubt wenn:
     *  (a) der Spieler selbst diesen Slot hat, ODER
     *  (b) dieser Slot von keinem Spieler in der Kette besetzt ist
     *      (unbesetzte Rolle → alle dürfen es)
     */
    private fun canDo(uuid: UUID, requiredSlot: Int): Boolean {
        val mySlot = getSlot(uuid)
        if (mySlot == requiredSlot) return true  // eigene Rolle

        // Slot besetzt wenn:
        // (a) ein online Spieler in playerOrder diesen Slot hat, ODER
        // (b) irgendein Spieler eine feste Zuweisung für diesen Slot hat
        val occupiedByOrder = playerOrder.any { getSlot(it) == requiredSlot }
        val occupiedByFixed = fixedRoles.values.any { it == requiredSlot }
        return !(occupiedByOrder || occupiedByFixed)
    }

    // ── Start / Stop (wird vom ChallengeManager aufgerufen) ──────────────────

    fun startChallenge() {
        if (isActive) return
        isActive = true
        loadFixedRoles()

        playerOrder.clear()
        Bukkit.getOnlinePlayers()
            .filter { !plugin.exemptPlayers.contains(it.uniqueId) }
            .map { it.uniqueId }
            .forEach { playerOrder.add(it) }

        startChainTask()
        spawnAllBats()
        broadcastRoles()

        Bukkit.broadcast(
            Component.text("⛓ ", NamedTextColor.GOLD)
                .append(Component.text("Chained Together", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" gestartet!", NamedTextColor.WHITE))
        )
        if (isSharedInventory) {
            Bukkit.broadcast(
                Component.text("  ➤ Geteiltes Inventar: ", NamedTextColor.YELLOW)
                    .append(Component.text("AN", NamedTextColor.GREEN, TextDecoration.BOLD))
            )
        }
    }

    fun stopChallenge() {
        if (!isActive) return
        isActive = false

        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId)
            taskId = -1
        }
        removeAllBats()
        playerOrder.clear()

        Bukkit.broadcast(
            Component.text("⛓ Chained Together ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("gestoppt.", NamedTextColor.WHITE))
        )
    }

    // ── Join-Event: Spieler anketten & Rollen neu verteilen ──────────────────

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (!isActive) return
        val player = event.player

        // Exempted Spieler werden nicht angekette
        if (plugin.exemptPlayers.contains(player.uniqueId)) return

        // Ans Ende der Kette hängen
        if (!playerOrder.contains(player.uniqueId)) {
            playerOrder.add(player.uniqueId)
        }

        // Kurz warten bis der Spieler vollständig geladen ist
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            spawnBatFor(player)
            relinkAllLeashes()
            redistributeAndAnnounce(
                reason = Component.text("${player.name} ist der Kette beigetreten!", NamedTextColor.AQUA)
            )
        }, 20L)
    }

    // ── Quit-Event: Spieler entketten & Rollen neu verteilen ─────────────────

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        if (!isActive) return
        val uuid = event.player.uniqueId

        if (!playerOrder.contains(uuid)) return

        val wasLeader = playerOrder.indexOf(uuid) == 0
        playerOrder.remove(uuid)
        removeBatFor(uuid)
        relinkAllLeashes()

        if (playerOrder.isEmpty()) {
            // Niemand mehr da – Challenge pausieren aber aktiv lassen
            Bukkit.broadcast(
                Component.text("⛓ Alle Spieler haben die Kette verlassen. Challenge wartet...", NamedTextColor.YELLOW)
            )
            return
        }

        val leaderPromotedMsg = if (wasLeader) {
            val newLeader = Bukkit.getPlayer(playerOrder[0])
            Component.text("  ➤ Neuer Leader: ", NamedTextColor.GREEN)
                .append(Component.text(newLeader?.name ?: "?", NamedTextColor.WHITE, TextDecoration.BOLD))
        } else null

        redistributeAndAnnounce(
            reason = Component.text("${event.player.name} hat die Kette verlassen.", NamedTextColor.YELLOW),
            extra = leaderPromotedMsg
        )
    }

    // ── Neuverteilung + Broadcast ─────────────────────────────────────────────

    /**
     * Verteilt alle aktuellen Spieler neu auf Rollen und teilt es mit.
     * [reason] = warum neu verteilt wird (Join/Leave-Nachricht)
     * [extra]  = optionale Zusatzzeile (z.B. "Neuer Leader: ...")
     */
    private fun redistributeAndAnnounce(
        reason: Component,
        extra: Component? = null
    ) {
        Bukkit.broadcast(Component.text("─────────────────────────────", NamedTextColor.GOLD))
        Bukkit.broadcast(
            Component.text("  ⛓ Kette neu verteilt  ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(reason)
        )
        extra?.let { Bukkit.broadcast(it) }
        Bukkit.broadcast(Component.text("─────────────────────────────", NamedTextColor.GOLD))

        for ((index, uuid) in playerOrder.withIndex()) {
            val p = Bukkit.getPlayer(uuid) ?: continue
            val slot = getSlot(uuid)
            val role = getRoleInfo(slot)

            // Server-weite Ankündigung
            Bukkit.broadcast(
                Component.text("  ${index + 1}. ", NamedTextColor.GRAY)
                    .append(Component.text(p.name, NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(role.name, role.color, TextDecoration.BOLD))
                    .append(Component.text("  ${role.desc}", NamedTextColor.GRAY))
            )

            // Persönliche Nachricht
            p.sendMessage(
                Component.text("\n⛓ Deine neue Rolle: ", NamedTextColor.GOLD)
                    .append(Component.text(role.name, role.color, TextDecoration.BOLD))
                    .append(Component.text("\n  ${role.desc}\n", NamedTextColor.GRAY))
            )
        }
        Bukkit.broadcast(Component.text("─────────────────────────────", NamedTextColor.GOLD))
    }

    private fun broadcastRoles() {
        redistributeAndAnnounce(
            reason = Component.text("Challenge gestartet!", NamedTextColor.GREEN)
        )
    }

    // ── Ketten-Teleport Task ──────────────────────────────────────────────────

    // ── Ketten-Task ───────────────────────────────────────────────────────────
    //
    //  Jeden Tick: Follower wird zum Anker gezogen sobald Abstand > MAX_DIST.
    //  Nur P1 (Leader) darf aktiv laufen – alle anderen werden mitgezogen.

    private fun startChainTask() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            if (!isActive) return@scheduleSyncRepeatingTask

            updateAllBatPositions()

            for (i in 1 until playerOrder.size) {
                val anchor   = Bukkit.getPlayer(playerOrder[i - 1]) ?: continue
                val follower = Bukkit.getPlayer(playerOrder[i])     ?: continue

                if (follower.world != anchor.world) {
                    follower.teleport(anchor.location)
                    follower.sendMessage(Component.text("⛓ Teleportiert zu ${anchor.name}", NamedTextColor.RED))
                    continue
                }

                val dist = follower.location.distance(anchor.location)

                when {
                    // Sofort-Teleport als letzter Fallback
                    dist > HARD_DIST -> {
                        follower.teleport(anchor.location.clone().apply {
                            yaw   = follower.location.yaw
                            pitch = follower.location.pitch
                        })
                        follower.sendMessage(Component.text("⛓ Mitgezogen!", NamedTextColor.RED))
                    }

                    // Kette gespannt → Follower aktiv zum Anker ziehen
                    dist > MAX_DIST -> {
                        val toAnchor: Vector = anchor.location.toVector()
                            .subtract(follower.location.toVector())
                            .normalize()
                        val overshoot = ((dist - MAX_DIST) / (HARD_DIST - MAX_DIST)).coerceIn(0.0, 1.0)
                        val force = PULL + overshoot * PULL * 2.0
                        follower.velocity = toAnchor.clone().multiply(force)
                    }

                    else -> {}
                }
            }
        }, 1L, 1L)
    }

    // ── Leine: eine Bat pro Spieler ──────────────────────────────────────────
    //
    //  Jeder Spieler bekommt eine unsichtbare Bat die dauerhaft auf seiner
    //  Position sitzt (kein Respawn, nur teleport → keine Lags).
    //  Die Leine läuft von Bat[i] → Bat[i-1], also vom Follower zum Anker.
    //  Das ergibt eine saubere Linie direkt zwischen den Spielern.
    //
    //  playerBats: UUID des Spielers → seine persönliche Bat

    // ── Bat pro Spieler für vanilla Leine ────────────────────────────────────
    //  Eine unsichtbare Bat sitzt über jedem Spieler.
    //  Leine läuft von Bat[i] → Bat[i-1] → sichtbare vanilla-Leine.
    //  Wird jeden Tick neu gesetzt damit sie nie reißt.

    private val playerBats = mutableMapOf<UUID, Bat>()

    private fun spawnAllBats() {
        removeAllBats()
        for (uuid in playerOrder) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            spawnBatFor(player)
        }
        relinkAllLeashes()
    }

    private fun spawnBatFor(player: Player) {
        removeBatFor(player.uniqueId)
        val loc = player.location.clone().add(0.0, 1.4, 0.0)
        val bat = player.world.spawn(loc, Bat::class.java) { b ->
            b.isInvisible    = true
            b.isSilent       = true
            b.isAwake        = true
            b.setAI(false)
            b.isInvulnerable = true
            b.isPersistent   = false
            b.isCollidable   = false
        }
        playerBats[player.uniqueId] = bat
    }

    private fun relinkAllLeashes() {
        for (i in 1 until playerOrder.size) {
            val anchorBat   = playerBats[playerOrder[i - 1]] ?: continue
            val followerBat = playerBats[playerOrder[i]]     ?: continue
            if (anchorBat.isValid && followerBat.isValid) {
                try {
                    if (!followerBat.isLeashed || followerBat.leashHolder != anchorBat) {
                        followerBat.setLeashHolder(anchorBat)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    private fun updateAllBatPositions() {
        for (uuid in playerOrder) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            val bat    = playerBats[uuid]
            if (bat == null || !bat.isValid) {
                spawnBatFor(player)
                relinkAllLeashes()
            } else {
                bat.teleport(player.location.clone().add(0.0, 1.4, 0.0))
            }
        }
        // Jeden Tick neu verknüpfen → Leine reißt nie
        relinkAllLeashes()
    }

    private fun removeBatFor(uuid: UUID) {
        playerBats.remove(uuid)?.let { if (it.isValid) it.remove() }
    }

    private fun removeAllBats() {
        playerBats.values.forEach { if (it.isValid) it.remove() }
        playerBats.clear()
    }

    // ── Geteilte Herzen ───────────────────────────────────────────────────────

    // ── Geteilte Herzen ───────────────────────────────────────────────────────
    //
    //  Wir lauschen auf EntityDamageEvent (alle Schadensquellen: Mobs, Fall,
    //  Feuer, Lava, Explosion, Void, …) und spiegeln nach dem Tick den
    //  niedrigsten HP-Wert auf alle anderen Spieler.

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onAnyDamage(event: EntityDamageEvent) {
        if (!isActive || syncingHealth) return
        val player = event.entity as? Player ?: return
        if (!playerOrder.contains(player.uniqueId)) return

        syncingHealth = true
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            // HP des verwundeten Spielers nach dem Schaden
            val damagedHp = Bukkit.getPlayer(player.uniqueId)?.health ?: run {
                syncingHealth = false; return@Runnable
            }
            for (uuid in playerOrder) {
                Bukkit.getPlayer(uuid)?.let { p ->
                    if (p.uniqueId == player.uniqueId) return@let
                    // Nur nach unten angleichen (Heilung teilen wir nicht hier)
                    if (p.health > damagedHp) {
                        p.health = damagedHp.coerceAtLeast(0.0)
                    }
                }
            }
            syncingHealth = false
        }, 1L)
    }

    // ── Geteilter Hunger ──────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onFoodChange(event: FoodLevelChangeEvent) {
        if (!isActive || syncingFood) return
        val player = event.entity as? Player ?: return
        if (!playerOrder.contains(player.uniqueId)) return

        syncingFood = true
        val newFood = event.foodLevel
        val newSat = player.saturation
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            for (uuid in playerOrder) {
                Bukkit.getPlayer(uuid)?.let { p ->
                    if (p.uniqueId == player.uniqueId) return@let
                    p.foodLevel = newFood
                    p.saturation = newSat
                }
            }
            syncingFood = false
        }, 1L)
    }

    // ── Geteiltes Inventar (optional) ────────────────────────────────────────

    private fun syncInventory(source: Player) {
        if (!isSharedInventory) return
        val contents = source.inventory.contents
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            for (uuid in playerOrder) {
                Bukkit.getPlayer(uuid)?.let { p ->
                    if (p.uniqueId == source.uniqueId) return@let
                    p.inventory.contents = contents.map { it?.clone() }.toTypedArray()
                }
            }
        }, 1L)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInvClickSync(event: InventoryClickEvent) {
        if (!isActive || !isSharedInventory) return
        val player = event.whoClicked as? Player ?: return
        if (!playerOrder.contains(player.uniqueId)) return
        if (event.inventory.type != InventoryType.PLAYER &&
            event.inventory.type != InventoryType.CRAFTING) return
        syncInventory(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDropSync(event: PlayerDropItemEvent) {
        if (!isActive || !isSharedInventory) return
        val player = event.player
        if (!playerOrder.contains(player.uniqueId)) return
        syncInventory(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPickupSync(event: EntityPickupItemEvent) {
        if (!isActive || !isSharedInventory) return
        val player = event.entity as? Player ?: return
        if (!playerOrder.contains(player.uniqueId)) return
        syncInventory(player)
    }

    // ── Bewegung (nur Leader darf laufen) ────────────────────────────────────

    // ── Bewegung: alle dürfen laufen, Kette begrenzt Abstand ─────────────────
    //
    //  Wie im Original Chained Together: jeder kann sich frei bewegen,
    //  aber sobald der Abstand zum Nachbarn > MAX_DIST ist wird die
    //  Bewegung geblockt (Kette hält) + Velocity-Pull zieht ihn zurück.
    //  Zusätzlich: Nicht-Leader werden auch vom Anker mitgezogen wenn
    //  der Leader sich wegbewegt.

    @EventHandler(priority = EventPriority.HIGH)
    fun onMove(event: PlayerMoveEvent) {
        if (!isActive) return
        val player = event.player
        if (plugin.exemptPlayers.contains(player.uniqueId)) return

        val pos = playerOrder.indexOf(player.uniqueId)
        if (pos <= 0) return  // Leader oder nicht in Kette → frei

        val from = event.from
        val to   = event.to ?: return
        if (from.x == to.x && from.z == to.z) return  // Kopfdrehung → OK

        val anchor = Bukkit.getPlayer(playerOrder[pos - 1]) ?: return
        if (to.world != anchor.world) return

        val distTo   = to.distance(anchor.location)
        val distFrom = from.distance(anchor.location)

        // Nur blocken wenn sich der Spieler AKTIV vom Anker wegbewegt
        // und dabei die Grenze überschreitet.
        // Velocity-Pull (Bewegung ZUM Anker hin) immer durchlassen.
        if (distTo > MAX_DIST && distTo > distFrom) {
            event.setTo(from)
        }
    }

    // ── Abbauen & Platzieren (Slot 1, oder unbesetzt) ────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!isActive) return
        val player = event.player
        if (plugin.exemptPlayers.contains(player.uniqueId)) return
        if (canDo(player.uniqueId, 1)) return
        cancelWithBar(event, player, "Nur der Abbauer darf Blöcke abbauen!")
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (!isActive) return
        val player = event.player
        if (plugin.exemptPlayers.contains(player.uniqueId)) return
        if (canDo(player.uniqueId, 1)) return
        cancelWithBar(event, player, "Nur der Abbauer darf Blöcke platzieren!")
    }

    // ── Angreifen (Slot 0 = Leader, Slot 7 = Schütze, oder unbesetzt) ────────

    @EventHandler(priority = EventPriority.HIGH)
    fun onAttack(event: EntityDamageByEntityEvent) {
        if (!isActive) return
        val player = event.damager as? Player ?: return
        if (plugin.exemptPlayers.contains(player.uniqueId)) return
        if (canDo(player.uniqueId, 0) || canDo(player.uniqueId, 7)) return
        event.isCancelled = true
        player.sendMessage(Component.text("⛓ Nur der Leader kann angreifen!", NamedTextColor.RED))
    }

    // ── Traden (Slot 2, oder unbesetzt) ──────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        if (!isActive) return
        val player = event.player
        if (plugin.exemptPlayers.contains(player.uniqueId)) return
        if (getSlot(player.uniqueId) == 0) return

        val type = event.rightClicked.type
        // Alle Boote & Loren über den EntityType-Namen prüfen
        val typeName = type.name.lowercase()
        val isVehicle = typeName.endsWith("_boat") ||
                typeName.endsWith("_raft") ||
                typeName == "minecart" ||
                typeName.endsWith("_minecart")
        if (isVehicle) return

        val isTrader = type == org.bukkit.entity.EntityType.VILLAGER ||
                type == org.bukkit.entity.EntityType.WANDERING_TRADER
        if (!isTrader) {
            event.isCancelled = true
            return
        }
        if (canDo(player.uniqueId, 2)) return
        event.isCancelled = true
        player.sendMessage(Component.text("⛓ Nur der Händler darf traden!", NamedTextColor.RED))
    }

    // ── Essen (Slot 3, oder unbesetzt) ────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    fun onConsume(event: PlayerItemConsumeEvent) {
        if (!isActive) return
        val player = event.player
        if (plugin.exemptPlayers.contains(player.uniqueId)) return
        if (canDo(player.uniqueId, 3)) return
        cancelWithBar(event, player, "Nur der Esser darf essen!")
    }

    // ── Item-Pickup: jeder darf ───────────────────────────────────────────────

    // ── Craften (Slot 5, oder unbesetzt) ──────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    fun onCraft(event: CraftItemEvent) {
        if (!isActive) return
        val player = event.whoClicked as? Player ?: return
        if (plugin.exemptPlayers.contains(player.uniqueId)) return
        if (canDo(player.uniqueId, 5)) return
        event.isCancelled = true
        player.sendMessage(Component.text("⛓ Nur der Crafter darf craften!", NamedTextColor.RED))
    }

    // ── Inventar bedienen (Slot 6, oder unbesetzt + Neben-Berechtigungen) ────

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryClick(event: InventoryClickEvent) {
        if (!isActive) return
        val player = event.whoClicked as? Player ?: return
        if (plugin.exemptPlayers.contains(player.uniqueId)) return
        val slot = getSlot(player.uniqueId)

        // Slot-spezifische Inventar-Berechtigungen
        when {
            slot == 0 -> return
            slot == 1 || canDo(player.uniqueId, 1) -> return
            slot == 2 && event.inventory.type == InventoryType.MERCHANT -> return
            canDo(player.uniqueId, 2) && event.inventory.type == InventoryType.MERCHANT -> return
            slot == 3 && (event.inventory.type == InventoryType.PLAYER ||
                    event.inventory.type == InventoryType.CRAFTING) -> return
            canDo(player.uniqueId, 3) && (event.inventory.type == InventoryType.PLAYER ||
                    event.inventory.type == InventoryType.CRAFTING) -> return
            (slot == 4 || canDo(player.uniqueId, 4)) && (
                    event.inventory.type == InventoryType.FURNACE ||
                            event.inventory.type == InventoryType.BLAST_FURNACE ||
                            event.inventory.type == InventoryType.SMOKER) -> return
            slot == 5 && (event.inventory.type == InventoryType.WORKBENCH ||
                    event.inventory.type == InventoryType.CRAFTING) -> return
            canDo(player.uniqueId, 5) && (event.inventory.type == InventoryType.WORKBENCH ||
                    event.inventory.type == InventoryType.CRAFTING) -> return
            slot == 6 || canDo(player.uniqueId, 6) -> return
        }

        event.isCancelled = true
        player.sendMessage(Component.text("⛓ Nur der Manager darf das Inventar bedienen!", NamedTextColor.RED))
    }

    // ── Welt-Interaktion (Kisten, Hebel, etc.) ───────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        if (!isActive) return
        val player = event.player
        if (plugin.exemptPlayers.contains(player.uniqueId)) return
        val clickedBlock = event.clickedBlock ?: return
        val slot = getSlot(player.uniqueId)

        if (slot == 0) return  // Leader
        if (slot == 1 || canDo(player.uniqueId, 1)) return  // Abbauer

        val foodBlocks = setOf(Material.SWEET_BERRY_BUSH, Material.CAKE)
        if ((slot == 3 || canDo(player.uniqueId, 3)) && clickedBlock.type in foodBlocks) return

        val containers = setOf(
            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
            Material.SHULKER_BOX, Material.ENDER_CHEST, Material.HOPPER,
            Material.DROPPER, Material.DISPENSER
        )
        val furnaces = setOf(
            Material.FURNACE, Material.BLAST_FURNACE,
            Material.SMOKER, Material.BREWING_STAND
        )
        if ((slot == 6 || canDo(player.uniqueId, 6)) && clickedBlock.type in containers) return
        if ((slot == 4 || canDo(player.uniqueId, 4)) && clickedBlock.type in furnaces) return

        // Händler braucht keine Block-Interaktion (läuft über InteractEntity)
        if (slot == 2 || canDo(player.uniqueId, 2)) return

        event.isCancelled = true
        player.sendMessage(Component.text("⛓ Du darfst das nicht!", NamedTextColor.RED))
    }

    private fun cancelWithBar(event: Cancellable, player: Player, msg: String) {
        event.isCancelled = true
        player.sendMessage(Component.text("⛓ $msg", NamedTextColor.RED))
    }

    data class RoleInfo(val name: String, val color: NamedTextColor, val desc: String)

    fun getRoleInfoPublic(slot: Int): RoleInfo = getRoleInfo(slot)

    private fun getRoleInfo(slot: Int): RoleInfo = when (slot) {
        0 -> RoleInfo("Leader",  NamedTextColor.GREEN,        "Laufen + Angreifen")
        1 -> RoleInfo("Abbauer", NamedTextColor.AQUA,         "Abbauen + Platzieren")
        2 -> RoleInfo("Händler", NamedTextColor.YELLOW,       "Nur traden")
        3 -> RoleInfo("Esser",   NamedTextColor.GOLD,         "Nur essen")
        4 -> RoleInfo("Kocher",  NamedTextColor.LIGHT_PURPLE, "Nur Öfen/Smoker/Blast Furnace")
        5 -> RoleInfo("Crafter", NamedTextColor.BLUE,         "Nur craften")
        6 -> RoleInfo("Manager", NamedTextColor.WHITE,        "Nur Inventar & Kisten")
        7 -> RoleInfo("Schütze", NamedTextColor.RED,          "Nur Fernkampf")
        else -> RoleInfo("???",  NamedTextColor.DARK_GRAY,    "Unbekannte Rolle")
    }

    fun getOrderString(): String = playerOrder.mapIndexed { i, uuid ->
        val name = Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString()
        val role = getRoleInfo(getSlot(uuid))
        "  ${i + 1}. $name → ${role.name}"
    }.joinToString("\n")
}