package io.github.black_Kittys22.challengekittys.ChunkChallenge

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import java.util.Random

class ChunkListener(private val plugin: Main) : Listener {

    private val spawnableMobs = listOf(
        EntityType.CHICKEN,
        EntityType.COW,
        EntityType.SHEEP,
        EntityType.PIG,
        EntityType.ZOMBIE,
        EntityType.SKELETON,
        EntityType.CREEPER,
        EntityType.SPIDER,
        EntityType.CAVE_SPIDER,
        EntityType.ENDERMAN,
        EntityType.WITCH,
        EntityType.BLAZE,
        EntityType.GHAST,
        EntityType.RAVAGER,
        EntityType.ELDER_GUARDIAN,
        EntityType.EVOKER,
        EntityType.WARDEN,
        EntityType.WITHER
    )

    private val replaceMaterials = Material.entries.filter { mat ->
        mat.isBlock &&
                mat.isSolid &&
                !mat.name.contains("AIR") &&
                !mat.name.contains("WATER") &&
                !mat.name.contains("LAVA") &&
                !mat.name.contains("FIRE") &&
                !mat.name.contains("PORTAL") &&
                !mat.name.contains("VOID") &&
                !mat.name.contains("SPAWNER") &&
                !mat.name.contains("COMMAND") &&
                !mat.name.contains("STRUCTURE") &&
                !mat.name.contains("BARRIER") &&
                !mat.name.contains("LIGHT") &&
                mat != Material.BEDROCK
    }

    private val replacedChunks = mutableSetOf<String>()

    @EventHandler
    fun onPlayerMove(e: PlayerMoveEvent) {
        if (e.from.chunk == e.to.chunk) return
        if (!plugin.isChunkChallengeSelected || plugin.timer.paused) return

        val player = e.player
        val chunk = e.to.chunk
        val key = plugin.makeChunkKey(chunk.x, chunk.z)

        if (plugin.playerActiveChunk[player.uniqueId] == key) return

        // remove old mob for player
        val oldEntry = plugin.chunkEntityMap.entries.find { it.value == player.uniqueId }
        if (oldEntry != null) {
            val oldMob = Bukkit.getEntity(java.util.UUID.fromString(oldEntry.key))
            oldMob?.remove()
            plugin.chunkEntityMap.remove(oldEntry.key)
        }

        plugin.playerActiveChunk[player.uniqueId] = key

        val chunkCenterX = (chunk.x * 16 + 8).toDouble()
        val chunkCenterZ = (chunk.z * 16 + 8).toDouble()
        val border = Bukkit.createWorldBorder()
        border.setCenter(chunkCenterX, chunkCenterZ)
        border.size = 16.0
        border.warningDistance = 0
        border.warningTime = 0
        player.worldBorder = border

        if (!replacedChunks.contains(key)) {
            replacedChunks.add(key)

            val chunkRandom = Random(chunk.world.seed xor (chunk.x.toLong() shl 32) xor chunk.z.toLong())
            val material = replaceMaterials[chunkRandom.nextInt(replaceMaterials.size)]

            Bukkit.getScheduler().runTask(plugin, Runnable {
                val minY = chunk.world.minHeight
                val maxY = chunk.world.maxHeight

                for (x in 0 until 16) {
                    for (z in 0 until 16) {
                        for (absY in minY until maxY) {
                            val block = chunk.getBlock(x, absY, z)
                            if (block.type.isSolid) {
                                block.type = material
                            }
                        }
                    }
                }
                player.sendMessage("§7Dieser Chunk besteht jetzt aus: §e${material.name}")
            })
        }

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val spawnY = chunk.world.getHighestBlockYAt(
                chunk.x * 16 + 8,
                chunk.z * 16 + 8
            ).toDouble() + 1

            val spawnLoc = org.bukkit.Location(
                chunk.world,
                chunkCenterX,
                spawnY,
                chunkCenterZ
            )

            val mobType = spawnableMobs.random()
            val mob = chunk.world.spawnEntity(spawnLoc, mobType)
            plugin.chunkEntityMap[mob.uniqueId.toString()] = player.uniqueId
            player.sendMessage("§6Ein §e${mobType.name} §6bewacht diesen Chunk! Besiege ihn, um weiterzukommen.")
        }, 40L)
    }

    @EventHandler
    fun onMobMove(e: EntityMoveEvent) {
        val entity = e.entity
        if (entity is org.bukkit.entity.Player) return

        val bossId = entity.uniqueId.toString()
        val playerId = plugin.chunkEntityMap[bossId] ?: return

        val player = Bukkit.getPlayer(playerId) ?: return
        val key = plugin.playerActiveChunk[player.uniqueId] ?: return

        if (!plugin.isLocationInChunk(e.to, key)) {
            e.isCancelled = true
            entity.teleport(e.from)
        }
    }

    @EventHandler
    fun onBossKnockback(e: EntityDamageEvent) {
        val entity = e.entity
        if (entity is org.bukkit.entity.Player) return

        val bossId = entity.uniqueId.toString()
        val playerId = plugin.chunkEntityMap[bossId] ?: return

        val player = Bukkit.getPlayer(playerId) ?: return
        val key = plugin.playerActiveChunk[player.uniqueId] ?: return

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (!plugin.isLocationInChunk(entity.location, key)) {
                val highestY = entity.world.getHighestBlockYAt(entity.location)
                entity.teleport(entity.location.apply { y = highestY.toDouble() + 1 })
            }
        }, 1L)
    }
}