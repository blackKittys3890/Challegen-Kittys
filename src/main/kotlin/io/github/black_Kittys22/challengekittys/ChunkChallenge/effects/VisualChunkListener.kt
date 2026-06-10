package io.github.black_Kittys22.challengekittys.ChunkChallenge.effects

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.Random
import java.util.UUID

@Suppress("unused")
class VisualChunkListener(private val plugin: Main) : Listener {

    private val rng = Random()

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

    private val playerChunkMaterials = mutableMapOf<UUID, MutableMap<String, Material>>()
    // map: playerUUID -> (chunkKey -> potionEffectType)
    private val playerChunkEffects = mutableMapOf<UUID, MutableMap<String, PotionEffectType>>()

    // ── ALLE Effekte die /effect kennt – via Registry (Paper 1.20.5+) ──────────
    // Registry.EFFECT ist 1:1 die Quelle die Vanilla /effect intern nutzt.
    // Fallback auf das alte values()-Array falls die Registry nicht verfügbar ist.
    private val possibleEffects: List<PotionEffectType> = run {
        try {
            Registry.EFFECT.toList().also {
                plugin.logger.info("[ChunkChallenge] ${it.size} Potion-Effekte aus Registry.EFFECT geladen.")
            }
        } catch (_: Throwable) {
            @Suppress("DEPRECATION")
            PotionEffectType.values().filterNotNull().toList().also {
                plugin.logger.warning("[ChunkChallenge] Registry.EFFECT nicht verfügbar – Fallback auf values() mit ${it.size} Effekten.")
            }
        }
    }

    @EventHandler
    fun onPlayerMove(e: PlayerMoveEvent) {
        // FIX: Objekt-Vergleich (==) funktioniert nicht für Chunks → Koordinaten prüfen
        val from = e.from
        val to   = e.to ?: return
        if (from.blockX shr 4 == to.blockX shr 4 &&
            from.blockZ shr 4 == to.blockZ shr 4) return

        if (!plugin.isChunkChallengeSelected || plugin.timer.paused) return

        val player = e.player
        val chunk  = to.chunk
        val key    = plugin.makeChunkKey(chunk.x, chunk.z)

        val previousKey = plugin.playerActiveChunk[player.uniqueId]
        if (previousKey == key) return

        // --- Alten Mob entfernen ---
        val oldEntry = plugin.chunkEntityMap.entries.find { it.value == player.uniqueId }
        if (oldEntry != null) {
            Bukkit.getEntity(UUID.fromString(oldEntry.key))?.remove()
            plugin.chunkEntityMap.remove(oldEntry.key)
        }

        // --- Alten Potion-Effekt entfernen ---
        if (previousKey != null) {
            playerChunkEffects[player.uniqueId]?.remove(previousKey)?.let { prevEffect ->
                try { player.removePotionEffect(prevEffect) } catch (_: Exception) {}
            }
        }

        plugin.playerActiveChunk[player.uniqueId] = key

        // --- World-Border setzen ---
        val chunkCenterX = (chunk.x * 16 + 8).toDouble()
        val chunkCenterZ = (chunk.z * 16 + 8).toDouble()
        val border = Bukkit.createWorldBorder()
        border.setCenter(chunkCenterX, chunkCenterZ)
        border.size = 16.0
        border.warningDistance = 0
        player.worldBorder = border

        // --- Fake-Material für diesen Chunk ---
        val perPlayerMap     = playerChunkMaterials.getOrPut(player.uniqueId) { mutableMapOf() }
        val materialForChunk = perPlayerMap.getOrPut(key) {
            replaceMaterials[rng.nextInt(replaceMaterials.size)]
        }

        // --- Potion-Effekt für diesen Chunk (aus ALLEN /effect-Effekten) ---
        val perEffectMap = playerChunkEffects.getOrPut(player.uniqueId) { mutableMapOf() }
        val effectType   = perEffectMap.getOrPut(key) {
            possibleEffects[rng.nextInt(possibleEffects.size)]
        }

        // Effekt anwenden – 1 Stunde Dauer, wird beim Verlassen/Mob-Tod entfernt
        try {
            val durationTicks = 20 * 60 * 60 // 1 Stunde
            val amplifier     = 0             // Stufe I (wie /effect give <player> <effect> default)
            player.addPotionEffect(
                PotionEffect(effectType, durationTicks, amplifier, false, true, true)
            )
            // Anzeigename: z.B. "minecraft:speed" -> "Speed"
            val displayName = effectType.key.key.replace('_', ' ')
                .split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            player.sendMessage("§7Chunk-Effekt: §b$displayName I")
        } catch (_: Exception) {}

        // --- Blöcke visuell ersetzen ---
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val minY = chunk.world.minHeight
            for (x in 0 until 16) {
                for (z in 0 until 16) {
                    val realX = chunk.x * 16 + x
                    val realZ = chunk.z * 16 + z
                    val topY  = chunk.world.getHighestBlockYAt(realX, realZ)
                    for (y in minY..topY) {
                        val block = chunk.getBlock(x, y, z)
                        val t = block.type
                        if (t == Material.AIR || t == Material.END_PORTAL_FRAME || t == Material.SPAWNER) continue
                        try { player.sendBlockChange(block.location, materialForChunk.createBlockData()) } catch (_: Exception) {}
                    }
                }
            }
            player.sendMessage("§7Dieser Chunk erscheint für dich als: §e${materialForChunk.name}")
        })

        // --- Bewacher-Mob spawnen ---
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val spawnY   = chunk.world.getHighestBlockYAt(chunk.x * 16 + 8, chunk.z * 16 + 8).toDouble() + 1
            val spawnLoc = org.bukkit.Location(chunk.world, chunkCenterX, spawnY, chunkCenterZ)
            val mobType  = spawnableMobs[rng.nextInt(spawnableMobs.size)]
            val mob      = chunk.world.spawnEntity(spawnLoc, mobType)
            mob.isGlowing = true
            plugin.chunkEntityMap[mob.uniqueId.toString()] = player.uniqueId
            player.sendMessage("§6Ein §e${mobType.name} §6bewacht diesen Chunk! Besiege ihn, um weiterzukommen.")
        }, 40L)
    }

    @EventHandler
    fun onMobMove(e: EntityMoveEvent) {
        val entity = e.entity
        if (entity is org.bukkit.entity.Player) return

        val bossId   = entity.uniqueId.toString()
        val playerId = plugin.chunkEntityMap[bossId] ?: return
        val player   = Bukkit.getPlayer(playerId) ?: return
        val key      = plugin.playerActiveChunk[player.uniqueId] ?: return

        if (!plugin.isLocationInChunk(e.to, key)) {
            e.isCancelled = true
            entity.teleport(e.from)
        }
    }

    @EventHandler
    fun onBossKnockback(e: EntityDamageEvent) {
        val entity = e.entity
        if (entity is org.bukkit.entity.Player) return

        val bossId   = entity.uniqueId.toString()
        val playerId = plugin.chunkEntityMap[bossId] ?: return
        val player   = Bukkit.getPlayer(playerId) ?: return
        val key      = plugin.playerActiveChunk[player.uniqueId] ?: return

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (!plugin.isLocationInChunk(entity.location, key)) {
                val highestY = entity.world.getHighestBlockYAt(entity.location)
                entity.teleport(entity.location.apply { y = highestY.toDouble() + 1 })
            }
        }, 1L)
    }

    @EventHandler
    fun onEntityDeath(e: EntityDeathEvent) {
        val ent      = e.entity
        val bossId   = ent.uniqueId.toString()
        val playerId = plugin.chunkEntityMap[bossId] ?: return
        val player   = Bukkit.getPlayer(playerId) ?: return

        val key = plugin.playerActiveChunk[player.uniqueId]
        if (key != null) {
            playerChunkEffects[player.uniqueId]?.remove(key)?.let { eff ->
                try { player.removePotionEffect(eff) } catch (_: Exception) {}
            }
        }
    }

    fun cleanupAllEffects() {
        for ((playerId, map) in playerChunkEffects) {
            val p = Bukkit.getPlayer(playerId)
            if (p != null && p.isOnline) {
                for ((_, eff) in map) {
                    try { p.removePotionEffect(eff) } catch (_: Exception) {}
                }
            }
        }
        playerChunkEffects.clear()
        playerChunkMaterials.clear()
    }
}