package io.github.black_Kittys22.challengekittys.Battles.LuegenBattle

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.generator.structure.Structure
import java.io.File
import java.util.*

class StructureBattleManager(private val plugin: Main) {

    private val structureLocations = mutableMapOf<UUID, Location>()
    private val playerWorlds = mutableMapOf<UUID, World>()
    private var availableStructures = mutableListOf<Structure>()
    private val playerTruthStatus = mutableMapOf<UUID, Boolean>()
    private var roundActive = false
    private var totalPlayers = 0
    private var structuresPlaced = 0

    init {
        reloadStructures()
    }

    @Suppress("DEPRECATION")
    private fun reloadStructures() {
        availableStructures = Registry.STRUCTURE.toList().toMutableList()
        availableStructures.shuffle()
        println("[DEBUG] Struktur-Liste neu geladen. Anzahl: ${availableStructures.size}")
    }

    private fun isStructureBlock(type: Material): Boolean {
        return when (type) {
            Material.PRISMARINE, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE, Material.SEA_LANTERN,
            Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS,
            Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS,
            Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS,
            Material.CHISELED_STONE_BRICKS, Material.CRACKED_STONE_BRICKS,
            Material.SANDSTONE, Material.CHISELED_SANDSTONE, Material.CUT_SANDSTONE, Material.SMOOTH_SANDSTONE,
            Material.RED_SANDSTONE, Material.CHISELED_RED_SANDSTONE, Material.CUT_RED_SANDSTONE,
            Material.BRICKS, Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS,
            Material.END_STONE_BRICKS, Material.PURPUR_BLOCK, Material.PURPUR_PILLAR,
            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.SPAWNER,
            Material.TERRACOTTA, Material.WHITE_TERRACOTTA, Material.ORANGE_TERRACOTTA, Material.CYAN_TERRACOTTA,
            Material.BLUE_TERRACOTTA, Material.LIGHT_BLUE_TERRACOTTA, Material.YELLOW_TERRACOTTA,
            Material.BOOKSHELF, Material.CRAFTING_TABLE, Material.FURNACE,
            Material.RAIL, Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.ACTIVATOR_RAIL,
            Material.LADDER, Material.TORCH, Material.WALL_TORCH, Material.REDSTONE_TORCH,
            Material.IRON_BARS, Material.GLASS_PANE,
            Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE, Material.DIAMOND_ORE,
            Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
            Material.SPONGE, Material.WET_SPONGE,
            Material.LAVA, Material.MAGMA_BLOCK -> true
            else -> false
        }
    }

    private fun isNaturalBlock(type: Material): Boolean {
        return when (type) {
            Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT, Material.PODZOL, Material.MYCELIUM,
            Material.STONE, Material.DEEPSLATE, Material.BEDROCK,
            Material.WATER,
            Material.GRAVEL, Material.SAND, Material.RED_SAND,
            Material.SANDSTONE, Material.RED_SANDSTONE,
            Material.ANDESITE, Material.DIORITE, Material.GRANITE,
            Material.CALCITE, Material.TUFF, Material.DRIPSTONE_BLOCK,
            Material.SNOW, Material.SNOW_BLOCK, Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE,
            Material.CLAY, Material.TERRACOTTA,
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES,
            Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES, Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES,
            Material.TALL_GRASS, Material.SHORT_GRASS, Material.FERN, Material.LARGE_FERN,
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
            Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP,
            Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY,
            Material.BROWN_MUSHROOM, Material.RED_MUSHROOM,
            Material.SEAGRASS, Material.TALL_SEAGRASS, Material.KELP, Material.KELP_PLANT,
            Material.NETHERRACK, Material.SOUL_SAND, Material.SOUL_SOIL, Material.BASALT, Material.BLACKSTONE,
            Material.WARPED_NYLIUM, Material.CRIMSON_NYLIUM -> true
            else -> false
        }
    }

    fun startRound() {
        val players = Bukkit.getOnlinePlayers().toList()
        val random = Random()

        println("[DEBUG] Starte Runde fuer ${players.size} Spieler.")

        roundActive = true
        totalPlayers = players.size
        structuresPlaced = 0

        playerTruthStatus.clear()
        players.forEach { player ->
            val isTruth = random.nextBoolean()
            playerTruthStatus[player.uniqueId] = isTruth
        }

        Bukkit.broadcast(Component.text("Erstelle individuelle Welten...", NamedTextColor.YELLOW))

        createWorldsSequentially(players.toMutableList(), random, 0)
    }

    private fun createWorldsSequentially(
        remainingPlayers: MutableList<Player>,
        random: Random,
        delay: Long
    ) {
        if (remainingPlayers.isEmpty()) {
            println("[DEBUG] Alle Welten erstellt.")
            return
        }

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val player = remainingPlayers.removeAt(0)

            if (!player.isOnline) {
                createWorldsSequentially(remainingPlayers, random, 5L)
                return@Runnable
            }

            if (availableStructures.isEmpty()) reloadStructures()

            @Suppress("DEPRECATION")
            val structure = availableStructures.removeAt(0)
            val structureKey = structure.key()
            val structureName = structureKey.value()

            val worldName = "lb${player.name.lowercase()}${System.currentTimeMillis()}"

            println("[DEBUG] Erstelle Welt '$worldName' fuer ${player.name}...")
            player.sendMessage(Component.text("Erstelle deine Welt...", NamedTextColor.YELLOW))

            try {
                val world = WorldCreator(worldName)
                    .environment(World.Environment.NORMAL)
                    .generateStructures(true)
                    .createWorld()

                if (world == null) {
                    println("[DEBUG] FEHLER: Welt konnte nicht erstellt werden!")
                    player.sendMessage(Component.text("Fehler beim Erstellen der Welt!", NamedTextColor.RED))
                    createWorldsSequentially(remainingPlayers, random, 5L)
                    return@Runnable
                }

                val actualWorldName = world.name
                println("[DEBUG] Welt erstellt: $actualWorldName")

                playerWorlds[player.uniqueId] = world

                val spawnLoc = Location(world, 0.5, 200.0, 0.5)
                player.teleport(spawnLoc)

                player.allowFlight = true
                player.isFlying = true

                player.sendMessage(Component.text("Lade Welt...", NamedTextColor.YELLOW))

                // Lade Chunks
                for (chunkX in -15..15) {
                    for (chunkZ in -15..15) {
                        world.getChunkAt(chunkX, chunkZ).load(true)
                    }
                }

                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    processPlayerWorld(player, world, structureName, actualWorldName)
                }, 200L)

            } catch (e: Exception) {
                println("[DEBUG] FEHLER: ${e.message}")
                e.printStackTrace()
                player.sendMessage(Component.text("Fehler!", NamedTextColor.RED))
            }

            createWorldsSequentially(remainingPlayers, random, 40L)

        }, delay)
    }

    private fun processPlayerWorld(player: Player, world: World, structureName: String, actualWorldName: String) {
        player.sendMessage(Component.text("Suche Struktur...", NamedTextColor.GREEN))

        // Verwende /locate structure um die nächste Struktur zu finden
        val structureKey = structureName.lowercase()
        val locateCmd = "execute in $actualWorldName positioned 0 100 0 run locate structure minecraft:$structureKey"

        println("[DEBUG] Führe aus: $locateCmd")

        // Da wir die Koordinaten nicht direkt auslesen können, teleportieren wir den Spieler direkt zur Struktur
        val tpCmd = "execute in $actualWorldName run tp ${player.name} @s"

        // Teleportiere zur nächsten Struktur
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            // Verwende spreadplayers um den Spieler zu einer zufälligen Position zu teleportieren
            // dort sollte mit hoher Wahrscheinlichkeit eine Struktur sein
            val spreadCmd = "execute in $actualWorldName run spreadplayers 0 0 500 2000 false ${player.name}"
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), spreadCmd)

            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                val playerLoc = player.location
                val structureX = playerLoc.blockX
                val structureY = playerLoc.blockY
                val structureZ = playerLoc.blockZ

                println("[DEBUG] Spieler Position: X=$structureX Y=$structureY Z=$structureZ")
                player.sendMessage(Component.text("Position gefunden! Lade Chunks...", NamedTextColor.YELLOW))

                // Lade Chunks um die Position
                val centerChunkX = structureX / 16
                val centerChunkZ = structureZ / 16

                for (chunkX in (centerChunkX - 15)..(centerChunkX + 15)) {
                    for (chunkZ in (centerChunkZ - 15)..(centerChunkZ + 15)) {
                        world.getChunkAt(chunkX, chunkZ).load(true)
                    }
                }

                player.sendMessage(Component.text("Entferne natürliche Blöcke...", NamedTextColor.YELLOW))

                // Entferne Blöcke um die Spieler-Position
                val radius = 100
                var removed = 0

                for (x in (structureX - radius)..(structureX + radius)) {
                    for (z in (structureZ - radius)..(structureZ + radius)) {
                        for (y in world.minHeight..world.maxHeight) {
                            val block = world.getBlockAt(x, y, z)
                            if (isNaturalBlock(block.type)) {
                                block.type = Material.AIR
                                removed++
                            }
                        }
                    }

                    if (x % 10 == 0) {
                        Thread.sleep(1)
                    }
                }

                println("[DEBUG] $removed Blöcke entfernt")
                player.sendMessage(Component.text("Welt bereit!", NamedTextColor.GREEN))

                // Finde höchsten Punkt
                var highestY = structureY
                for (searchX in (structureX - 20)..(structureX + 20)) {
                    for (searchZ in (structureZ - 20)..(structureZ + 20)) {
                        for (y in world.maxHeight downTo world.minHeight) {
                            val block = world.getBlockAt(searchX, y, searchZ)
                            if (block.type != Material.AIR && block.type != Material.CAVE_AIR) {
                                if (y > highestY) {
                                    highestY = y
                                }
                                break
                            }
                        }
                    }
                }

                finishPlayerSetup(player, world, structureX, highestY, structureZ, structureName, actualWorldName)

            }, 100L)

        }, 40L)
    }

    private fun finishPlayerSetup(
        player: Player,
        world: World,
        structureX: Int,
        structureY: Int,
        structureZ: Int,
        structureName: String,
        actualWorldName: String
    ) {
        var highestY = structureY
        for (searchX in (structureX - 20)..(structureX + 20)) {
            for (searchZ in (structureZ - 20)..(structureZ + 20)) {
                for (y in world.maxHeight downTo world.minHeight) {
                    val block = world.getBlockAt(searchX, y, searchZ)
                    if (block.type != Material.AIR && block.type != Material.CAVE_AIR) {
                        if (y > highestY) {
                            highestY = y
                        }
                        break
                    }
                }
            }
        }

        val finalLoc = Location(world, structureX + 0.5, (highestY + 2).toDouble(), structureZ + 0.5)

        finalLoc.block.type = Material.AIR
        finalLoc.clone().add(0.0, 1.0, 0.0).block.type = Material.AIR
        finalLoc.clone().add(0.0, -1.0, 0.0).block.type = Material.GLASS

        player.teleport(finalLoc)
        structureLocations[player.uniqueId] = finalLoc

        player.sendMessage(
            Component.text("Deine Struktur: ", NamedTextColor.GREEN)
                .append(Component.text(structureName, NamedTextColor.GOLD))
        )

        val isTruth = playerTruthStatus[player.uniqueId] ?: false
        if (isTruth) {
            player.sendMessage(
                Component.text("Du musst die ", NamedTextColor.GRAY)
                    .append(Component.text("WAHRHEIT", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(" sagen!", NamedTextColor.GRAY))
            )
        } else {
            player.sendMessage(
                Component.text("Du musst ", NamedTextColor.GRAY)
                    .append(Component.text("LUEGEN", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text("!", NamedTextColor.GRAY))
            )
        }

        println("[DEBUG] ${player.name} fertig")

        structuresPlaced++
        println("[DEBUG] Strukturen platziert: $structuresPlaced / $totalPlayers")

        if (structuresPlaced >= totalPlayers) {
            val battleTimeSeconds = 300
            plugin.timer.startCountdown(battleTimeSeconds)

            Bukkit.broadcast(Component.text("=".repeat(50), NamedTextColor.GOLD))
            Bukkit.broadcast(
                Component.text("Timer gestartet: ", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .append(Component.text("${battleTimeSeconds / 60} Minuten", NamedTextColor.YELLOW, TextDecoration.BOLD))
            )
            Bukkit.broadcast(Component.text("Viel Erfolg!", NamedTextColor.GREEN))
            Bukkit.broadcast(Component.text("=".repeat(50), NamedTextColor.GOLD))
        }
    }

    fun stopAndTeleportToZero() {
        if (!roundActive) return

        roundActive = false
        plugin.timer.paused = true

        val world = Bukkit.getWorld("world") ?: return
        val spawnLoc = Location(world, 0.5, world.getHighestBlockYAt(0, 0) + 1.0, 0.5)

        Bukkit.getOnlinePlayers().forEach { player ->
            player.teleport(spawnLoc)
            player.allowFlight = false
            player.isFlying = false
            player.sendMessage(Component.text("Luegenbattle beendet!", NamedTextColor.GOLD))
        }

        playerWorlds.forEach { (uuid, world) ->
            Bukkit.unloadWorld(world, false)
            File(Bukkit.getWorldContainer(), world.name).deleteRecursively()
        }

        structureLocations.clear()
        playerWorlds.clear()

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            Bukkit.broadcast(Component.text("=".repeat(50), NamedTextColor.GOLD))
            Bukkit.broadcast(Component.text("ABSTIMMUNG", NamedTextColor.GOLD, TextDecoration.BOLD))
            Bukkit.broadcast(Component.text("Wer hat gelogen?", NamedTextColor.YELLOW))
            Bukkit.broadcast(Component.text("=".repeat(50), NamedTextColor.GOLD))
        }, 40L)
    }

    fun isRoundActive(): Boolean = roundActive

    fun revealTruth() {
        Bukkit.broadcast(Component.text("=".repeat(50), NamedTextColor.DARK_RED))
        Bukkit.broadcast(Component.text("AUFLOESUNG", NamedTextColor.DARK_RED, TextDecoration.BOLD))
        Bukkit.broadcast(Component.text("=".repeat(50), NamedTextColor.DARK_RED))

        playerTruthStatus.forEach { (uuid, isTruth) ->
            val player = Bukkit.getPlayer(uuid) ?: return@forEach
            if (isTruth) {
                Bukkit.broadcast(
                    Component.text(player.name, NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text(" sagte die WAHRHEIT", NamedTextColor.GRAY))
                )
            } else {
                Bukkit.broadcast(
                    Component.text(player.name, NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text(" hat GELOGEN", NamedTextColor.GRAY))
                )
            }
        }

        playerTruthStatus.clear()
    }

    fun isPlayerInBattle(playerUuid: UUID): Boolean = structureLocations.containsKey(playerUuid)
    fun getStructureLocation(playerUuid: UUID): Location? = structureLocations[playerUuid]
}