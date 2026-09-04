package io.github.black_Kittys22.challengekittys

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class ChallengeManager(private val plugin: Main) : Listener {

    private val settingsTitle      = Component.text("Einstellungen", NamedTextColor.DARK_GRAY)
    private val challengeTitle     = Component.text("Challenges", NamedTextColor.GOLD, TextDecoration.BOLD)
    private val luegenTitle        = Component.text("Lügenbattle Modus", NamedTextColor.RED, TextDecoration.BOLD)
    private val chainTitle         = Component.text("Chained Together – Optionen", NamedTextColor.AQUA, TextDecoration.BOLD)
    private val chainRolesTitle    = Component.text("Rollen zuweisen – Spieler", NamedTextColor.AQUA, TextDecoration.BOLD)
    private val chainRolePickTitle = Component.text("Rolle auswählen", NamedTextColor.AQUA, TextDecoration.BOLD)

    private val randommizerTitle = Component.text("Wähle die Randomizer Challenge", NamedTextColor.AQUA, TextDecoration.BOLD)
    private val sammelTitle = Component.text("Sammel-Challenges", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)

    private val uuidKey = NamespacedKey(plugin, "chain_target_uuid")

    fun openChallengeGUI(player: Player) {
        val inv = Bukkit.createInventory(null, 45, challengeTitle)

        val isAnyMBActive = plugin.monsterBattleChallenge.isFarmingPhase || plugin.monsterBattleChallenge.isArenaPhase

        inv.setItem(34, createItem(Material.NETHER_STAR, "Randomizer", plugin.isRandomizerActive || plugin.isMobRandomizerActive ||
                plugin.isCraftingRandomizerActive))

        inv.setItem(0, createItem(Material.CLOCK, "Relay Challenge", plugin.isRelayChallengeActive))
        inv.setItem(10, createItem(Material.GRASS_BLOCK,          "Chunk Challenge",      plugin.isChunkChallengeSelected))
        inv.setItem(12, createItem(Material.ZOMBIE_HEAD,           "Monster Battle",       isAnyMBActive))
        inv.setItem(4,  createItem(Material.CREEPER_HEAD,          "Mob Drop Challenge",   plugin.isMobDropChallengeActive))
        inv.setItem(14, createItem(Material.CHEST,                 "Shared Inventory",     plugin.isSharedInventoryActive))
        inv.setItem(16, createItem(Material.RED_BANNER,            "Lügenbattle",          false))
        inv.setItem(18, createItem(Material.LIGHT_BLUE_STAINED_GLASS, "Chunk Effects",       plugin.isEffectsChunkActive))
        val anySammelActive = plugin.isSharedAdvancementsActive || plugin.isAllItemsChallengeActive ||
                plugin.isAllMobsChallengeActive || plugin.isArmorTrimsChallengeActive
        inv.setItem(6,  createItem(Material.CHEST,                 "Sammel-Challenges",    anySammelActive))
        inv.setItem(20, createItem(Material.FERMENTED_SPIDER_EYE,  "Half Heart",           plugin.isHalfHeartChallengeActive))
        inv.setItem(26, createItem(Material.BEDROCK,               "Bedrock Challenge",    plugin.bedrockChallenge.isActive))
        inv.setItem(28, createItem(Material.REPEATER,              "Infinite Loop",        plugin.isInfiniteLoopActive))
        inv.setItem(2,  createItem(Material.IRON_CHAIN,                 "Chained Together",     plugin.isChainedTogetherActive))
        inv.setItem(8,  createItem(Material.FEATHER,               "Swap Keys",            plugin.isSwapKeysChallengeActive))


        player.openInventory(inv)
    }

    private fun openRandomizerGUI(player: Player) {
        val inv = Bukkit.createInventory(null, 27, randommizerTitle)

        inv.setItem(11, createItem(
            Material.COMPASS,
            "Block Drop Randomizer",
            plugin.isRandomizerActive
        ))

        inv.setItem(13, createItem(
            Material.ZOMBIE_SPAWN_EGG,
            "Mob Randomizer",
            plugin.isMobRandomizerActive
        ))

        inv.setItem(15, createItem(
            Material.CRAFTING_TABLE,
            "Crafting Randomizer",
            plugin.isCraftingRandomizerActive
        ))

        player.openInventory(inv)
    }

    private fun openSammelGUI(player: Player) {
        val inv = Bukkit.createInventory(null, 27, sammelTitle)

        inv.setItem(10, createItem(Material.BEACON,                "All Items",        plugin.isAllItemsChallengeActive))
        inv.setItem(12, createItem(Material.WITHER_SKELETON_SKULL, "All Mobs",         plugin.isAllMobsChallengeActive))
        inv.setItem(14, createItem(Material.BOOKSHELF,             "All Advancements", plugin.isSharedAdvancementsActive))
        inv.setItem(18, createArmorTrimsItem())

        inv.setItem(22, createItem(Material.BARRIER, "Zurück", false))

        player.openInventory(inv)
    }

    fun openSettingsGUI(player: Player) {
        val inv = Bukkit.createInventory(null, 27, settingsTitle)
        inv.setItem(11, createItem(Material.CHEST,          "Keep Inventory", plugin.isKeepInventoryActive))
        inv.setItem(13, createItem(Material.SKELETON_SKULL, "Dead-Sync",      plugin.isDeadSyncActive))
        inv.setItem(15, createItem(Material.DIAMOND_SWORD,  "Damage Clear",   plugin.isDamageClearInventoryActive))
        player.openInventory(inv)
    }

    private fun openLuegenbattleGUI(player: Player) {
        val inv = Bukkit.createInventory(null, 27, luegenTitle)
        inv.setItem(13, createItem(Material.NETHER_STAR, "Start Lügenbattle", false))
        player.openInventory(inv)
    }

    private fun openChainOptionsGUI(player: Player) {
        val inv = Bukkit.createInventory(null, 27, chainTitle)

        val chainActive = plugin.isChainedTogetherActive
        inv.setItem(10, createItem(
            if (chainActive) Material.RED_CONCRETE else Material.LIME_CONCRETE,
            if (chainActive) "Challenge stoppen" else "Challenge starten",
            chainActive
        ))

        inv.setItem(12, createItem(
            Material.ENDER_CHEST,
            "Geteiltes Inventar",
            plugin.chainedTogetherChallenge.isSharedInventory
        ))

        val rolesItem = org.bukkit.inventory.ItemStack(Material.NAME_TAG)
        val rolesMeta = rolesItem.itemMeta!!
        rolesMeta.displayName(
            Component.text("Rollen zuweisen", NamedTextColor.YELLOW, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false)
        )
        val lore = mutableListOf<Component>()
        lore.add(Component.text("Weise Spielern feste Rollen zu.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
        lore.add(Component.text("Bleibt bis zum Reset gespeichert.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
        plugin.chainedTogetherChallenge.fixedRoles.forEach { (uuid, slot) ->
            val name = org.bukkit.Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString()
            val roleName = plugin.chainedTogetherChallenge.getRoleInfoPublic(slot).name
            lore.add(Component.text("  $name → $roleName", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
        }
        if (plugin.chainedTogetherChallenge.fixedRoles.isEmpty()) {
            lore.add(Component.text("  Noch keine festen Zuweisungen.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false))
        }
        rolesMeta.lore(lore)
        rolesItem.itemMeta = rolesMeta
        inv.setItem(14, rolesItem)

        // Alle Zuweisungen löschen
        inv.setItem(16, createItem(Material.BARRIER, "Alle Rollen zurücksetzen", false))

        // Info: aktuelle Ketten-Rollen
        val infoItem = org.bukkit.inventory.ItemStack(Material.PAPER)
        val infoMeta = infoItem.itemMeta!!
        infoMeta.displayName(
            Component.text("Aktuelle Ketten-Rollen", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
        )
        val loreLine = plugin.chainedTogetherChallenge.getOrderString()
            .lines()
            .map { Component.text(it, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false) }
        infoMeta.lore(loreLine)
        infoItem.itemMeta = infoMeta
        inv.setItem(22, infoItem)

        player.openInventory(inv)
    }

    private fun openRolePlayerSelectGUI(admin: Player) {
        val onlinePlayers = Bukkit.getOnlinePlayers().toList()
        val size = (((onlinePlayers.size - 1) / 9) + 1) * 9
        val inv  = Bukkit.createInventory(null, size.coerceIn(9, 54), chainRolesTitle)

        onlinePlayers.forEachIndexed { index, target ->
            val skull = ItemStack(Material.PLAYER_HEAD)
            val meta  = skull.itemMeta as org.bukkit.inventory.meta.SkullMeta
            meta.owningPlayer = target
            val slot     = plugin.chainedTogetherChallenge.getSlot(target.uniqueId)
            val roleName = if (slot >= 0) plugin.chainedTogetherChallenge.getRoleInfoPublic(slot).name else "?"
            val fixed    = plugin.chainedTogetherChallenge.fixedRoles.containsKey(target.uniqueId)
            meta.displayName(
                Component.text(target.name, if (fixed) NamedTextColor.GOLD else NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
            )
            meta.lore(listOf(
                Component.text("Rolle: $roleName", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text(if (fixed) "★ Fest zugewiesen" else "Automatisch", if (fixed) NamedTextColor.GOLD else NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Klicken → Rolle zuweisen", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ))
            // UUID direkt im PDC speichern → kein String-Parsing nötig
            meta.persistentDataContainer.set(uuidKey, PersistentDataType.STRING, target.uniqueId.toString())
            skull.itemMeta = meta
            inv.setItem(index, skull)
        }
        admin.openInventory(inv)
    }

    private fun openRoleSelectGUI(admin: Player, targetUUID: java.util.UUID) {
        val targetName = Bukkit.getOfflinePlayer(targetUUID).name ?: "?"
        val inv = Bukkit.createInventory(null, 27, chainRolePickTitle)

        val roles = listOf(
            Triple(Material.COMPASS,        "Leader",   0),
            Triple(Material.IRON_PICKAXE,   "Abbauer",  1),
            Triple(Material.EMERALD,        "Händler",  2),
            Triple(Material.COOKED_BEEF,    "Esser",    3),
            Triple(Material.POTION,         "Heiler",   4),
            Triple(Material.CRAFTING_TABLE, "Crafter",  5),
            Triple(Material.CHEST,          "Manager",  6),
            Triple(Material.BOW,            "Schütze",  7),
        )

        val currentSlot = plugin.chainedTogetherChallenge.fixedRoles[targetUUID]

        roles.forEachIndexed { index, (mat, name, slot) ->
            val item = ItemStack(mat)
            val meta = item.itemMeta!!
            val isAssigned = currentSlot == slot
            meta.displayName(
                Component.text(name, if (isAssigned) NamedTextColor.GREEN else NamedTextColor.WHITE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false)
            )
            meta.lore(listOf(
                Component.text(if (isAssigned) "✔ Aktuell zugewiesen" else "Klicken zum Zuweisen",
                    if (isAssigned) NamedTextColor.GREEN else NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            ))
            if (isAssigned) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true)
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            }
            // UUID auch hier im PDC speichern damit wir im Click-Handler wissen für wen
            meta.persistentDataContainer.set(uuidKey, PersistentDataType.STRING, targetUUID.toString())
            item.itemMeta = meta
            inv.setItem(index + 10, item)
        }

        // Zuweisung löschen
        val clear = ItemStack(Material.BARRIER)
        val clearMeta = clear.itemMeta!!
        clearMeta.displayName(Component.text("Zuweisung von $targetName entfernen", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
        clearMeta.persistentDataContainer.set(uuidKey, PersistentDataType.STRING, targetUUID.toString())
        clear.itemMeta = clearMeta
        inv.setItem(18, clear)

        admin.openInventory(inv)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = event.view.title()

        // ── Haupt-Challenge-GUI ───────────────────────────────────────────────
        if (title == challengeTitle) {
            event.isCancelled = true

            when (event.slot) {
                34 -> {
                    openRandomizerGUI(player)
                    return
                }

                4 -> {
                    plugin.isMobDropChallengeActive = !plugin.isMobDropChallengeActive
                    player.sendMessage(
                        if (plugin.isMobDropChallengeActive)
                            "§aMob Drop Challenge aktiviert!"
                        else
                            "§cMob Drop Challenge deaktiviert."
                    )
                }

                6 -> {
                    openSammelGUI(player)
                    return
                }

                10 -> {
                    plugin.isChunkChallengeSelected = !plugin.isChunkChallengeSelected
                    player.sendMessage(
                        if (plugin.isChunkChallengeSelected)
                            "§aChunk Challenge aktiviert!"
                        else
                            "§cChunk Challenge deaktiviert."
                    )
                    plugin.saveConfig()
                }

                18 -> {
                    plugin.isEffectsChunkActive = !plugin.isEffectsChunkActive

                    if (plugin.isEffectsChunkActive) {
                        plugin.effectsChallenge.start()
                        player.sendMessage("§aEffekte-Chunk-Challenge aktiviert!")
                    } else {
                        plugin.effectsChallenge.stop()
                        player.sendMessage("§cEffekte-Chunk-Challenge deaktiviert.")
                    }

                    plugin.saveConfig()
                }

                12 -> {
                    if (plugin.monsterBattleChallenge.isFarmingPhase ||
                        plugin.monsterBattleChallenge.isArenaPhase
                    ) {
                        plugin.monsterBattleChallenge.stopChallenge()
                    } else {
                        plugin.monsterBattleChallenge.startChallenge(30)
                    }
                }

                0 -> {
                    if (plugin.isRelayChallengeActive) {
                        plugin.relayChallenge.stop()
                        plugin.isRelayChallengeActive = false
                    } else {
                        plugin.relayChallenge.start()
                        plugin.isRelayChallengeActive =
                            plugin.relayChallenge.isActive
                    }
                    plugin.saveConfig()
                }

                14 -> plugin.isSharedInventoryActive =
                    !plugin.isSharedInventoryActive

                16 -> {
                    openLuegenbattleGUI(player)
                    return
                }

                20 -> {
                    plugin.isHalfHeartChallengeActive =
                        !plugin.isHalfHeartChallengeActive
                    plugin.halfHeartChallenge.applyToAll()
                }

                26 -> {
                    if (!plugin.bedrockChallenge.isActive)
                        plugin.bedrockChallenge.start()
                    else
                        plugin.bedrockChallenge.isActive = false
                }

                28 -> {
                    plugin.isInfiniteLoopActive =
                        !plugin.isInfiniteLoopActive

                    if (!plugin.isInfiniteLoopActive)
                        plugin.infiniteLoopChallenge.stopAllTasks()

                    player.sendMessage(
                        if (plugin.isInfiniteLoopActive)
                            "§aInfinite Loop aktiviert!"
                        else
                            "§cInfinite Loop deaktiviert."
                    )
                }

                2 -> {
                    openChainOptionsGUI(player)
                    return
                }

                8 -> {
                    if (plugin.isSwapKeysChallengeActive) {
                        plugin.swapKeysChallenge.disable()
                        plugin.isSwapKeysChallengeActive = false
                    } else {
                        plugin.swapKeysChallenge.enable()
                        plugin.isSwapKeysChallengeActive = true
                    }
                    plugin.saveConfig()
                }

            }

            openChallengeGUI(player)
        }

        // ── Randomizer GUI ─────────────────────────────────────────────────────
        else if (title == randommizerTitle) {
            event.isCancelled = true

            when (event.slot) {

                11 -> {
                    plugin.isRandomizerActive =
                        !plugin.isRandomizerActive

                    player.sendMessage(
                        if (plugin.isRandomizerActive)
                            "§aBlock Drop Randomizer aktiviert!"
                        else
                            "§cBlock Drop Randomizer deaktiviert!"
                    )
                }

                13 -> {
                    plugin.isMobRandomizerActive =
                        !plugin.isMobRandomizerActive

                    player.sendMessage(
                        if (plugin.isMobRandomizerActive)
                            "§aMob Randomizer aktiviert!"
                        else
                            "§cMob Randomizer deaktiviert!"
                    )
                }

                15 -> {
                    plugin.isCraftingRandomizerActive =
                        !plugin.isCraftingRandomizerActive

                    player.sendMessage(
                        if (plugin.isCraftingRandomizerActive)
                            "§aCrafting Randomizer aktiviert!"
                        else
                            "§cCrafting Randomizer deaktiviert!"
                    )
                }
            }

            plugin.saveConfig()
            openRandomizerGUI(player)
        }

        // ── Sammel-Challenges GUI ────────────────────────────────────────────
        else if (title == sammelTitle) {
            event.isCancelled = true

            when (event.slot) {
                10 -> {
                    plugin.isAllItemsChallengeActive = !plugin.isAllItemsChallengeActive
                    if (plugin.isAllItemsChallengeActive)
                        plugin.allItemsListener.showBar(player)
                    else
                        plugin.allItemsListener.hideBar()
                }

                12 -> {
                    plugin.isAllMobsChallengeActive = !plugin.isAllMobsChallengeActive
                    if (plugin.isAllMobsChallengeActive)
                        plugin.allMobsListener.showBar(player)
                    else
                        plugin.allMobsListener.hideBar()
                }

                14 -> {
                    plugin.isSharedAdvancementsActive = !plugin.isSharedAdvancementsActive
                    player.sendMessage(
                        if (plugin.isSharedAdvancementsActive)
                            "§aAll Advancements aktiviert!"
                        else
                            "§cAll Advancements deaktiviert."
                    )
                }


                18 -> {
                    if (event.isShiftClick) {
                        plugin.allArmorTrims.cycleDifficulty()
                        player.sendMessage("§bArmor-Trims-Schwierigkeit: §f${plugin.allArmorTrims.difficulty.name}")
                    } else {
                        plugin.isArmorTrimsChallengeActive = !plugin.isArmorTrimsChallengeActive
                        if (plugin.isArmorTrimsChallengeActive)
                            plugin.allArmorTrims.showBar(player)
                        else
                            plugin.allArmorTrims.hideBar()
                        player.sendMessage(
                            if (plugin.isArmorTrimsChallengeActive)
                                "§aAll Armor Trims aktiviert!"
                            else
                                "§cAll Armor Trims deaktiviert."
                        )
                    }
                }

                22 -> {
                    openChallengeGUI(player)
                    return
                }
            }

            plugin.saveConfig()
            openSammelGUI(player)
        }

        // ── Einstellungen GUI ──────────────────────────────────────────────────
        else if (title == settingsTitle) {
            event.isCancelled = true

            when (event.slot) {
                11 -> plugin.isKeepInventoryActive =
                    !plugin.isKeepInventoryActive

                13 -> plugin.isDeadSyncActive =
                    !plugin.isDeadSyncActive

                15 -> plugin.isDamageClearInventoryActive =
                    !plugin.isDamageClearInventoryActive
            }

            openSettingsGUI(player)
        }
    }

    private fun createArmorTrimsItem(): ItemStack {
        val active = plugin.isArmorTrimsChallengeActive
        val item = ItemStack(Material.NETHERITE_HELMET)
        val meta = item.itemMeta!!
        val color = if (active) NamedTextColor.GREEN else NamedTextColor.RED
        meta.displayName(Component.text("All Armor Trims", color).decoration(TextDecoration.ITALIC, false))

        val lore = mutableListOf<Component>()
        lore.add(Component.text(if (active) "AKTIVIERT" else "DEAKTIVIERT", color).decoration(TextDecoration.ITALIC, false))
        lore.add(Component.text("Schmiede jede Trim-Kombi am Amboss!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
        lore.add(Component.text("Schwierigkeit: ${plugin.allArmorTrims.difficulty.name}", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
        if (plugin.allArmorTrims.difficulty == io.github.black_Kittys22.challengekittys.AllArmorTrims.TrimDifficulty.EASY) {
            lore.add(Component.text("Easy-Stufe: ${plugin.allArmorTrims.easyTier}", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
        }
        lore.add(Component.text("Fortschritt: ${plugin.allArmorTrims.getProgressString()}", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
        lore.add(Component.text("Klick: Ein/Aus  |  Shift-Klick: Schwierigkeit wechseln", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
        meta.lore(lore)

        if (active) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true)
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
        }
        item.itemMeta = meta
        return item
    }

    private fun createItem(material: Material, name: String, isActive: Boolean): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item

        val color = if (isActive) NamedTextColor.GREEN else NamedTextColor.RED
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false))

        val statusText  = if (isActive) "AKTIVIERT" else "DEAKTIVIERT"
        val statusColor = if (isActive) NamedTextColor.GREEN else NamedTextColor.RED

        val lore = mutableListOf<Component>()
        lore.add(Component.text(statusText, statusColor).decoration(TextDecoration.ITALIC, false))

        when (name) {
            "Relay Challenge" -> {
                lore.add(Component.text("Immer nur 1 Spieler ist aktiv!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Alle anderen schauen per Spectator zu.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Alle 2 Min. wird der Spielstand übergeben.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            }
            "Monster Battle" -> {
                lore.add(Component.text("Linksklick: Start (30m) / Stop", NamedTextColor.GRAY))
            }
            "All Advancements" -> {
                lore.add(Component.text("Schaltet ein Spieler ein Advancement frei,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("erhalten es alle anderen automatisch!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Gilt auch für Datapack-Advancements.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            }
            "Crafting Randomizer" -> {
                lore.add(Component.text("Jedes Rezept ergibt ein zufälliges Item!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Die Rezepte bleiben dauerhaft gleich.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            }
            "All Mobs" -> {
                lore.add(Component.text("Besiege jeden Mob nacheinander,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("um exklusive Trophäen zu sammeln!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            }
            "All Deaths" -> {
                lore.add(Component.text("Stirbt ein Spieler auf eine neue Art,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("zählt das für das ganze Team!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            }
            "Sammel-Challenges" -> {
                lore.add(Component.text("Items, Mobs, Advancements & Deaths", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Klicken für Optionen →", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            }
            "Infinite Loop" -> {
                lore.add(Component.text("Jede Aktion wird unendlich wiederholt:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Abbauen, Platzieren, Schlagen, Schießen", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Sneaken = Loop stoppen", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            }
            "Randomizer" -> {
                lore.add(Component.text("Blöcke abbauen droppt zufällige Items!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Normale Drops werden ersetzt.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            }
            "Mob Randomizer" -> {
                lore.add(Component.text("Jeder Mob wird durch einen zufälligen", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("anderen Mob ersetzt!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Die Zuordnung bleibt dauerhaft fest.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            }
            "Chained Together" -> {
                lore.add(Component.text("Alle Spieler sind aneinander gekettet.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Nur P1 (Leader) darf laufen!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Jeder hat eine eigene Aufgabe.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Klicken für Optionen →", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            }
            "Challenge starten", "Challenge stoppen" -> {
                lore.add(Component.text("Klicken zum Umschalten", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            }
            "Geteiltes Inventar" -> {
                lore.add(Component.text("Alle Spieler teilen sich ein Inventar.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Optional – kann auch deaktiviert bleiben.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            }
            "Swap Keys" -> {
                lore.add(Component.text("Dein W bewegt einen zufälligen anderen Spieler!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Die eigene Vorwärtsbewegung wird blockiert.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Zuordnung wird bei Join/Quit neu gewürfelt.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            }
            "Farbspur Challenge" -> {
                lore.add(Component.text("Hinterlasse eine farbige Spur aus Concrete!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Spuren dürfen sich NICHT kreuzen!", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Bei Kreuzung sterben ALLE Spieler!", NamedTextColor.DARK_RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("Danach werden alle Spuren gelöscht", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("und neue Farben verteilt!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            }
        }

        meta.lore(lore)

        if (isActive) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true)
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
        }

        item.itemMeta = meta
        return item
    }
}