package io.github.black_Kittys22.challengekittys.Challenges

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Arrow
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.UUID

class InfiniteLoopChallenge(private val plugin: Main) : Listener {

    // Speichert laufende Tasks pro Spieler
    private val activeTasks = mutableMapOf<UUID, BukkitTask>()

    // Speichert den zuletzt abgebauten Block-Face (Richtung)
    private val lastBreakFace = mutableMapOf<UUID, BlockFace>()
    private val lastBreakBlock = mutableMapOf<UUID, Block>()

    // Speichert den zuletzt platzierten Block
    private val lastPlaceBlock = mutableMapOf<UUID, Block>()
    private val lastPlaceFace = mutableMapOf<UUID, BlockFace>()
    private val lastPlaceItem = mutableMapOf<UUID, Material>()

    // Projektil-Loop Daten
    data class ProjectileData(
        val type: ProjectileType,
        val direction: Vector,
        val location: Location
    )
    enum class ProjectileType { ARROW, SNOWBALL }
    private val lastProjectile = mutableMapOf<UUID, ProjectileData>()

    // Melee-Loop Daten
    data class MeleeData(val targetEntityId: Int, val direction: Vector, val location: Location)
    private val lastMelee = mutableMapOf<UUID, MeleeData>()

    // ─── BLOCK ABBAUEN ────────────────────────────────────────────────────────

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        if (!plugin.isInfiniteLoopActive) return

        val block = event.block
        val face = getPlayerFacingFace(player)

        lastBreakBlock[player.uniqueId] = block
        lastBreakFace[player.uniqueId] = face

        // Stoppe alten Task
        stopTask(player.uniqueId)

        // Starte neuen Loop-Task
        val task = object : BukkitRunnable() {
            var current = block.getRelative(face)

            override fun run() {
                val p = Bukkit.getPlayer(player.uniqueId) ?: run { cancel(); return }

                // Abbruch wenn sneaking
                if (p.isSneaking) {
                    stopTask(p.uniqueId)
                    cancel()
                    return
                }

                if (current.type == Material.AIR || current.type == Material.WATER || current.type == Material.LAVA) {
                    // Nichts mehr zum abbauen
                    cancel()
                    activeTasks.remove(p.uniqueId)
                    return
                }

                // Block abbauen mit Haltbarkeitsverlust
                val item = p.inventory.itemInMainHand
                breakBlockWithDurability(p, current, item)

                current = current.getRelative(face)
            }
        }.runTaskTimer(plugin, 2L, 4L)

        activeTasks[player.uniqueId] = task
    }

    // ─── BLOCK PLATZIEREN ─────────────────────────────────────────────────────

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        if (!plugin.isInfiniteLoopActive) return

        val block = event.blockPlaced
        val face = event.blockAgainst.getFace(block) ?: getPlayerFacingFace(player)
        val material = event.itemInHand.type

        lastPlaceBlock[player.uniqueId] = block
        lastPlaceFace[player.uniqueId] = face
        lastPlaceItem[player.uniqueId] = material

        stopTask(player.uniqueId)

        val task = object : BukkitRunnable() {
            var next = block.getRelative(face)

            override fun run() {
                val p = Bukkit.getPlayer(player.uniqueId) ?: run { cancel(); return }

                if (p.isSneaking) {
                    stopTask(p.uniqueId)
                    cancel()
                    return
                }

                // Prüfe ob Platz frei ist
                if (next.type != Material.AIR) {
                    cancel()
                    activeTasks.remove(p.uniqueId)
                    return
                }

                // Prüfe ob Spieler noch Items hat
                val item = p.inventory.itemInMainHand
                if (item.type != material || item.amount <= 0) {
                    cancel()
                    activeTasks.remove(p.uniqueId)
                    return
                }

                // Block platzieren
                next.type = material
                next.world.playEffect(next.location, Effect.STEP_SOUND, material)
                try {
                    next.world.playSound(next.location, Sound.valueOf("BLOCK_${material.name.replace("_BLOCK", "").replace("_PLANKS", "WOOD")}_PLACE"), 1f, 1f)
                } catch (_: IllegalArgumentException) { /* unbekannter Sound, ignorieren */ }

                // Item-Menge reduzieren
                if (item.amount > 1) {
                    item.amount -= 1
                    p.inventory.setItemInMainHand(item)
                } else {
                    p.inventory.setItemInMainHand(ItemStack(Material.AIR))
                    cancel()
                    activeTasks.remove(p.uniqueId)
                    return
                }

                next = next.getRelative(face)
            }
        }.runTaskTimer(plugin, 2L, 4L)

        activeTasks[player.uniqueId] = task
    }

    // ─── PFEIL / BOGen ─────────────────────────────────────────────────────────

    @EventHandler
    fun onBowShoot(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return
        if (!plugin.isInfiniteLoopActive) return

        val direction = player.eyeLocation.direction.clone()
        val location = player.eyeLocation.clone()

        lastProjectile[player.uniqueId] = ProjectileData(ProjectileType.ARROW, direction, location)

        stopTask(player.uniqueId)

        val task = object : BukkitRunnable() {
            override fun run() {
                val p = Bukkit.getPlayer(player.uniqueId) ?: run { cancel(); return }

                if (p.isSneaking) {
                    stopTask(p.uniqueId)
                    cancel()
                    return
                }

                // Prüfe ob Spieler Pfeile hat
                val hasArrow = p.inventory.contains(Material.ARROW) || p.gameMode == GameMode.CREATIVE
                if (!hasArrow) {
                    cancel()
                    activeTasks.remove(p.uniqueId)
                    return
                }

                // Pfeil abfeuern
                val arrow = p.launchProjectile(Arrow::class.java)
                arrow.velocity = direction.clone().multiply(3.0)
                arrow.shooter = p

                // Pfeil aus Inventar nehmen (Haltbarkeit-Logik)
                if (p.gameMode != GameMode.CREATIVE) {
                    removeOneArrow(p)
                    damageItemInHand(p)
                }

                p.world.playSound(p.location, Sound.ENTITY_ARROW_SHOOT, 1f, 1f)
            }
        }.runTaskTimer(plugin, 20L, 20L) // Alle 1 Sekunde

        activeTasks[player.uniqueId] = task
    }

    // ─── SCHNEEBALL ──────────────────────────────────────────────────────────

    @EventHandler
    fun onSnowballThrow(event: ProjectileLaunchEvent) {
        if (event.entity !is Snowball) return
        val player = event.entity.shooter as? Player ?: return
        if (!plugin.isInfiniteLoopActive) return

        val direction = player.eyeLocation.direction.clone()
        val location = player.eyeLocation.clone()

        lastProjectile[player.uniqueId] = ProjectileData(ProjectileType.SNOWBALL, direction, location)

        stopTask(player.uniqueId)

        val task = object : BukkitRunnable() {
            override fun run() {
                val p = Bukkit.getPlayer(player.uniqueId) ?: run { cancel(); return }

                if (p.isSneaking) {
                    stopTask(p.uniqueId)
                    cancel()
                    return
                }

                val hasSnowball = p.inventory.contains(Material.SNOWBALL) || p.gameMode == GameMode.CREATIVE
                if (!hasSnowball) {
                    cancel()
                    activeTasks.remove(p.uniqueId)
                    return
                }

                val snowball = p.launchProjectile(Snowball::class.java)
                snowball.velocity = direction.clone().multiply(2.5)
                snowball.shooter = p

                if (p.gameMode != GameMode.CREATIVE) {
                    removeOneItem(p, Material.SNOWBALL)
                }

                p.world.playSound(p.location, Sound.ENTITY_SNOWBALL_THROW, 1f, 1f)
            }
        }.runTaskTimer(plugin, 15L, 15L) // Alle 0.75 Sekunden

        activeTasks[player.uniqueId] = task
    }

    // ─── MELEE ANGRIFF ────────────────────────────────────────────────────────

    @EventHandler
    fun onEntityHit(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        if (!plugin.isInfiniteLoopActive) return
        val target = event.entity as? LivingEntity ?: return

        val direction = player.eyeLocation.direction.clone()

        lastMelee[player.uniqueId] = MeleeData(target.entityId, direction, player.location.clone())

        stopTask(player.uniqueId)

        val task = object : BukkitRunnable() {
            override fun run() {
                val p = Bukkit.getPlayer(player.uniqueId) ?: run { cancel(); return }

                if (p.isSneaking) {
                    stopTask(p.uniqueId)
                    cancel()
                    return
                }

                // Suche Entity in der Nähe
                val nearbyEntities = p.getNearbyEntities(5.0, 5.0, 5.0)
                    .filterIsInstance<LivingEntity>()
                    .filter { it.entityId == target.entityId && !it.isDead }

                if (nearbyEntities.isEmpty()) {
                    cancel()
                    activeTasks.remove(p.uniqueId)
                    return
                }

                val entity = nearbyEntities.first()
                entity.damage(event.damage, p)

                // Werkzeug-Haltbarkeit reduzieren
                damageItemInHand(p)

                p.world.playSound(p.location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f)
            }
        }.runTaskTimer(plugin, 20L, 20L) // Alle 1 Sekunde

        activeTasks[player.uniqueId] = task
    }

    // ─── SNEAKING = STOP ──────────────────────────────────────────────────────

    @EventHandler
    fun onSneak(event: PlayerToggleSneakEvent) {
        if (!plugin.isInfiniteLoopActive) return
        if (event.isSneaking) {
            stopTask(event.player.uniqueId)
        }
    }

    // ─── HILFSMETHODEN ────────────────────────────────────────────────────────

    private fun stopTask(uuid: UUID) {
        activeTasks[uuid]?.cancel()
        activeTasks.remove(uuid)
    }

    fun stopAllTasks() {
        activeTasks.values.forEach { it.cancel() }
        activeTasks.clear()
    }

    private fun getPlayerFacingFace(player: Player): BlockFace {
        val yaw = player.location.yaw.toDouble()
        val pitch = player.location.pitch.toDouble()

        return when {
            pitch < -45 -> BlockFace.UP
            pitch > 45 -> BlockFace.DOWN
            yaw in -45.0..45.0 -> BlockFace.SOUTH
            yaw in 45.0..135.0 -> BlockFace.WEST
            yaw in 135.0..180.0 || yaw in -180.0..-135.0 -> BlockFace.NORTH
            else -> BlockFace.EAST
        }
    }

    /**
     * Baut einen Block ab und verursacht Haltbarkeitsverlust am Werkzeug.
     */
    private fun breakBlockWithDurability(player: Player, block: Block, tool: ItemStack) {
        block.world.playEffect(block.location, Effect.STEP_SOUND, block.type)
        try {
            block.world.playSound(block.location, Sound.valueOf("BLOCK_${block.type.name.replace("_ORE", "").replace("_BLOCK", "")}_BREAK"), 1f, 1f)
        } catch (_: IllegalArgumentException) { /* unbekannter Sound */ }

        // Drops generieren
        block.getDrops(tool).forEach { drop ->
            block.world.dropItemNaturally(block.location.add(0.5, 0.5, 0.5), drop)
        }

        block.type = Material.AIR

        // Haltbarkeit reduzieren
        if (player.gameMode != GameMode.CREATIVE) {
            damageItemInHand(player)
        }
    }

    /**
     * Reduziert die Haltbarkeit des Werkzeugs in der Hand des Spielers.
     * Wenn die Haltbarkeit 0 erreicht, wird das Item zerstört.
     */
    @Suppress("DEPRECATION")
    private fun damageItemInHand(player: Player) {
        val item = player.inventory.itemInMainHand
        if (item.type == Material.AIR || !item.type.maxDurability.toInt().coerceAtLeast(1).let { it > 0 }) return
        val meta = item.itemMeta as? org.bukkit.inventory.meta.Damageable ?: return

        val unbreaking = item.enchantments[org.bukkit.enchantments.Enchantment.UNBREAKING] ?: 0
        val chance = 1.0 / (unbreaking + 1)
        if (Math.random() > chance) return // Unbreaking-Effekt

        val newDamage = meta.damage + 1
        if (newDamage >= item.type.maxDurability) {
            // Item zerstören
            player.inventory.setItemInMainHand(ItemStack(Material.AIR))
            player.world.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1f, 1f)
        } else {
            meta.damage = newDamage
            item.itemMeta = meta
        }
    }

    private fun removeOneArrow(player: Player) {
        removeOneItem(player, Material.ARROW)
    }

    private fun removeOneItem(player: Player, material: Material) {
        val inv = player.inventory
        for (i in 0 until inv.size) {
            val item = inv.getItem(i) ?: continue
            if (item.type == material) {
                if (item.amount > 1) {
                    item.amount -= 1
                } else {
                    inv.setItem(i, ItemStack(Material.AIR))
                }
                break
            }
        }
    }
}