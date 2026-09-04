package io.github.black_Kittys22.challengekittys.Battles.MobForceBattle

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.time.Duration

/**
 * Schrittweise Enthüllung der Rangliste von hinten nach vorne.
 *
 * Ablauf:
 *  1. /mobforce reveal  → öffnet Platz 3 (letzter) bei allen und animiert Mobs rein
 *  2. /mobforce reveal  → Platz 2
 *  3. /mobforce reveal  → Platz 1 (Sieger)
 *
 * GUI-Layout (54 Slots):
 *  Reihe 0 (0–8):   Info-Zeile – Platz-Symbol | Team-Kopf | Name | Fortschritt | Joker
 *  Reihe 1–3 (9–35): Spawn-Eggs der getöteten Mobs (werden animiert eingeblendet)
 *  Reihe 4 (36–44): frei / Filler
 *  Reihe 5 (45–53): Schließen-Button (Slot 49)
 */
object MobForceRankingGUI : Listener {

    private val TITLE = Component.text("🏆 MobForceBattle – Rangliste", NamedTextColor.GOLD, TextDecoration.BOLD)
    private val TITLE_STR = "MobForceBattle"

    // Welcher Platz als nächstes enthüllt wird (zählt von letztem → 1)
    // -1 = noch nicht gestartet
    private var currentRevealIndex: Int = -1   // Index in ranking-Liste (von hinten)
    private var ranking: List<MobForceTeam> = emptyList()
    private var plugin: Main? = null

    // Alle offenen GUIs (damit wir sie alle gleichzeitig updaten)
    private val openInventories = mutableSetOf<Inventory>()

    // ─── Ranking vorbereiten (nach Challenge-Ende) ────────────────────────────
    fun prepare(p: Main, manager: MobForceBattleManager) {
        plugin = p
        ranking = manager.getRanking()
        currentRevealIndex = ranking.size   // startet hinter dem letzten Platz
        openInventories.clear()
    }

    // ─── GUI für einen Spieler öffnen (zeigt leere Bühne) ────────────────────
    fun open(player: Player) {
        val inv = Bukkit.createInventory(null, 54, TITLE)
        fillFiller(inv)
        setCloseButton(inv)
        openInventories.add(inv)
        player.openInventory(inv)
    }

    // ─── Nächsten Platz enthüllen ─────────────────────────────────────────────
    fun revealNext(p: Main) {
        if (ranking.isEmpty()) return
        if (currentRevealIndex <= 0) {
            Bukkit.broadcast(
                Component.text("[MobForceBattle] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .append(Component.text("Alle Plätze wurden bereits enthüllt!", NamedTextColor.RED))
            )
            return
        }

        currentRevealIndex--
        val team = ranking[currentRevealIndex]
        val place = currentRevealIndex + 1

        // Announce im Chat
        val placeSymbol = placeSymbol(place)
        val placeColor = placeColor(place)
        Bukkit.broadcast(
            Component.text("══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD)
        )
        Bukkit.broadcast(
            Component.text("  $placeSymbol Platz $place: ", placeColor, TextDecoration.BOLD)
                .append(Component.text(team.displayName, team.color, TextDecoration.BOLD))
                .append(Component.text("  (${team.currentIndex} Mobs)", NamedTextColor.GRAY))
        )
        Bukkit.broadcast(
            Component.text("══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD)
        )

        // GUI für alle öffnen die noch keins haben, dann animieren
        Bukkit.getOnlinePlayers().forEach { player ->
            val alreadyOpen = player.openInventory.topInventory in openInventories
            if (!alreadyOpen) {
                open(player)
            }
        }

        // Kurze Pause dann Animation starten
        Bukkit.getScheduler().runTaskLater(p, Runnable {
            animateReveal(p, team, place)
        }, 10L)
    }

    // ─── Animation: Info-Zeile setzen, dann Mobs einzeln einblenden ──────────
    private fun animateReveal(p: Main, team: MobForceTeam, place: Int) {
        // Alle offenen Inventare aktualisieren
        openInventories.removeIf { inv ->
            // Inventar noch aktiv?
            Bukkit.getOnlinePlayers().none { it.openInventory.topInventory == inv }
        }

        // Schritt 1: Info-Zeile sofort setzen
        openInventories.forEach { inv ->
            fillFiller(inv)   // reset
            setInfoRow(inv, team, place)
            setCloseButton(inv)
        }

        // Schritt 2: Mobs animiert einblenden (alle 3 Ticks ein Mob)
        val killedMobs = team.mobList.subList(0, team.currentIndex)  // nur getötete
        var taskIndex = 0

        val task = object : Runnable {
            var mobIndex = 0
            override fun run() {
                if (mobIndex >= killedMobs.size) return

                val mob = killedMobs[mobIndex]
                val slot = MOB_SLOTS.getOrNull(mobIndex) ?: return

                val egg = makeSpawnEgg(mob, mobIndex + 1)
                openInventories.forEach { inv -> inv.setItem(slot, egg) }

                // Sound für alle die das GUI haben
                Bukkit.getOnlinePlayers()
                    .filter { it.openInventory.topInventory in openInventories }
                    .forEach { it.playSound(it.location, Sound.UI_BUTTON_CLICK, 0.5f, 1.2f) }

                mobIndex++
                if (mobIndex < killedMobs.size) {
                    Bukkit.getScheduler().runTaskLater(p, this, 3L)
                } else {
                    // Fertig – Fanfare für Platz 1
                    if (place == 1) {
                        Bukkit.getOnlinePlayers()
                            .filter { it.openInventory.topInventory in openInventories }
                            .forEach {
                                it.playSound(it.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
                                it.showTitle(
                                    Title.title(
                                        Component.text("🏆 ${team.displayName}", team.color, TextDecoration.BOLD),
                                        Component.text("Gewinner!", NamedTextColor.YELLOW),
                                        Title.Times.times(
                                            Duration.ofMillis(300),
                                            Duration.ofSeconds(3),
                                            Duration.ofSeconds(1)
                                        )
                                    )
                                )
                            }
                    }
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(p, task, 5L)
    }

    // ─── Info-Zeile (Reihe 0: Platz | Kopf | Name | Fortschritt | Joker) ─────
    private fun setInfoRow(inv: Inventory, team: MobForceTeam, place: Int) {
        val placeColor = placeColor(place)
        val symbol = placeSymbol(place)

        // Slot 0: Platz-Item
        val placeItem = ItemStack(when (place) {
            1 -> Material.GOLD_BLOCK
            2 -> Material.IRON_BLOCK
            3 -> Material.COPPER_BLOCK
            else -> Material.STONE
        }).also {
            val meta = it.itemMeta
            meta.displayName(Component.text("$symbol Platz $place", placeColor, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false))
            it.itemMeta = meta
        }
        inv.setItem(0, placeItem)

        // Slot 2: Spielerkopf / Team-Kopf
        val headItem: ItemStack = if (team.members.size == 1) {
            val skull = ItemStack(Material.PLAYER_HEAD)
            val skullMeta = skull.itemMeta as SkullMeta
            skullMeta.owningPlayer = Bukkit.getOfflinePlayer(team.members.first())
            skullMeta.displayName(Component.text(team.displayName, team.color, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false))
            skull.itemMeta = skullMeta
            skull
        } else {
            ItemStack(Material.ZOMBIE_HEAD).also {
                val meta = it.itemMeta
                meta.displayName(Component.text(team.displayName, team.color, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false))
                it.itemMeta = meta
            }
        }
        inv.setItem(2, headItem)

        // Slot 4: Fortschritt
        val progressItem = ItemStack(Material.PAPER).also {
            val meta = it.itemMeta
            meta.displayName(Component.text("Fortschritt", NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false))
            meta.lore(listOf(
                Component.text("${team.currentIndex} / ${team.mobList.size} Mobs getötet",
                    if (team.finished) NamedTextColor.GREEN else NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
            ))
            it.itemMeta = meta
        }
        inv.setItem(4, progressItem)

        // Slot 6: Joker
        val jokerItem = ItemStack(Material.NETHER_STAR).also {
            val meta = it.itemMeta
            meta.displayName(Component.text("Joker", NamedTextColor.YELLOW, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false))
            meta.lore(listOf(
                Component.text("${team.jokers}/4 übrig", NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
            ))
            it.itemMeta = meta
        }
        inv.setItem(6, jokerItem)

        // Slot 8: Mitglieder (nur bei echten Teams)
        if (team.members.size > 1) {
            val memberItem = ItemStack(Material.PLAYER_HEAD).also {
                val meta = it.itemMeta
                meta.displayName(Component.text("Mitglieder", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false))
                meta.lore(team.members.mapNotNull { uuid ->
                    Bukkit.getOfflinePlayer(uuid).name?.let { name ->
                        Component.text("  ▸ $name", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                    }
                })
                it.itemMeta = meta
            }
            inv.setItem(8, memberItem)
        }
    }

    // ─── Spawn-Egg für einen getöteten Mob ───────────────────────────────────
    private fun makeSpawnEgg(entityType: EntityType, index: Int): ItemStack {
        val eggMaterial = spawnEggMaterial(entityType) ?: Material.ZOMBIE_SPAWN_EGG
        return ItemStack(eggMaterial).also {
            val meta = it.itemMeta
            val mobName = entityType.name.lowercase().replace("_", " ").replaceFirstChar { c -> c.uppercase() }
            meta.displayName(Component.text("#$index $mobName", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false))
            it.itemMeta = meta
        }
    }

    // ─── Slots für Mob-Spawn-Eggs (Reihen 1–3, 27 Slots) ────────────────────
    private val MOB_SLOTS = (9..35).toList()

    // ─── Filler ───────────────────────────────────────────────────────────────
    private fun fillFiller(inv: Inventory) {
        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE).also {
            val meta = it.itemMeta
            meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false))
            it.itemMeta = meta
        }
        for (i in 0 until 54) inv.setItem(i, filler)
    }

    private fun setCloseButton(inv: Inventory) {
        inv.setItem(49, ItemStack(Material.BARRIER).also {
            val meta = it.itemMeta
            meta.displayName(Component.text("✖ Schließen", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false))
            it.itemMeta = meta
        })
    }

    // ─── EntityType → Spawn-Egg Material ──────────────────────────────────────
    private fun spawnEggMaterial(type: EntityType): Material? = when (type) {
        EntityType.ZOMBIE          -> Material.ZOMBIE_SPAWN_EGG
        EntityType.SKELETON        -> Material.SKELETON_SPAWN_EGG
        EntityType.CREEPER         -> Material.CREEPER_SPAWN_EGG
        EntityType.SPIDER          -> Material.SPIDER_SPAWN_EGG
        EntityType.CAVE_SPIDER     -> Material.CAVE_SPIDER_SPAWN_EGG
        EntityType.ENDERMAN        -> Material.ENDERMAN_SPAWN_EGG
        EntityType.WITCH           -> Material.WITCH_SPAWN_EGG
        EntityType.BLAZE           -> Material.BLAZE_SPAWN_EGG
        EntityType.WITHER_SKELETON -> Material.WITHER_SKELETON_SPAWN_EGG
        EntityType.GHAST           -> Material.GHAST_SPAWN_EGG
        EntityType.MAGMA_CUBE      -> Material.MAGMA_CUBE_SPAWN_EGG
        EntityType.SLIME           -> Material.SLIME_SPAWN_EGG
        EntityType.DROWNED         -> Material.DROWNED_SPAWN_EGG
        EntityType.HUSK            -> Material.HUSK_SPAWN_EGG
        EntityType.STRAY           -> Material.STRAY_SPAWN_EGG
        EntityType.PILLAGER        -> Material.PILLAGER_SPAWN_EGG
        EntityType.VINDICATOR      -> Material.VINDICATOR_SPAWN_EGG
        EntityType.RAVAGER         -> Material.RAVAGER_SPAWN_EGG
        EntityType.PHANTOM         -> Material.PHANTOM_SPAWN_EGG
        EntityType.ELDER_GUARDIAN  -> Material.ELDER_GUARDIAN_SPAWN_EGG
        EntityType.GUARDIAN        -> Material.GUARDIAN_SPAWN_EGG
        EntityType.SHULKER         -> Material.SHULKER_SPAWN_EGG
        EntityType.ENDERMITE       -> Material.ENDERMITE_SPAWN_EGG
        EntityType.SILVERFISH      -> Material.SILVERFISH_SPAWN_EGG
        EntityType.PIGLIN_BRUTE    -> Material.PIGLIN_BRUTE_SPAWN_EGG
        EntityType.HOGLIN          -> Material.HOGLIN_SPAWN_EGG
        EntityType.ZOGLIN          -> Material.ZOGLIN_SPAWN_EGG
        EntityType.WARDEN          -> Material.WARDEN_SPAWN_EGG
        else                       -> null  // Ender Dragon & Wither haben kein Spawn-Egg
    }

    // ─── Hilfsfunktionen ─────────────────────────────────────────────────────
    private fun placeSymbol(place: Int) = when (place) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#$place" }
    private fun placeColor(place: Int) = when (place) {
        1 -> NamedTextColor.GOLD; 2 -> NamedTextColor.GRAY; 3 -> NamedTextColor.RED; else -> NamedTextColor.WHITE
    }

    // ─── Click-Handler ────────────────────────────────────────────────────────
    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        val titleStr = PlainTextComponentSerializer
            .plainText().serialize(e.view.title())
        if (!titleStr.contains(TITLE_STR)) return
        e.isCancelled = true
        if (e.rawSlot == 49) (e.whoClicked as? Player)?.closeInventory()
    }
}