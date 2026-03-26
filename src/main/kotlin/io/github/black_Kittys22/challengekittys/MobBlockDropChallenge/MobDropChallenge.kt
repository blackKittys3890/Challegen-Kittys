package io.github.black_Kittys22.challengekittys.Challenges

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class MobDropChallenge(private val plugin: Main) : Listener {

    private val dropKey = NamespacedKey(plugin, "mob_drop_item")

    // Persistente Block → Mob Zuordnung (wird in blockMobMap.yml gespeichert)
    private val blockMobMap = mutableMapOf<String, EntityType>()
    private val mapFile get() = File(plugin.dataFolder, "blockMobMap.yml")

    // Dynamisch: alle EntityTypes die spawnable & lebendig sind
    private val mobPool: List<EntityType> by lazy {
        EntityType.entries.filter { it.isAlive && it.isSpawnable }
    }

    init {
        loadMap()
    }

    // ── Block → fixer Mob (persistent) ───────────────────────────────────────

    private fun getMobForBlock(material: Material): EntityType {
        val key = material.name
        return blockMobMap.getOrPut(key) {
            // Noch keine Zuordnung → zufällig wählen und speichern
            val chosen = mobPool.random()
            saveMap()
            chosen
        }
    }

    fun resetMap() {
        blockMobMap.clear()
        mapFile.delete()
    }

    private fun saveMap() {
        val yaml = YamlConfiguration()
        blockMobMap.forEach { (block, mob) -> yaml.set(block, mob.name) }
        yaml.save(mapFile)
    }

    private fun loadMap() {
        if (!mapFile.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(mapFile)
        for (key in yaml.getKeys(false)) {
            val mobName = yaml.getString(key) ?: continue
            val type = runCatching { EntityType.valueOf(mobName) }.getOrNull() ?: continue
            blockMobMap[key] = type
        }
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!plugin.isMobDropChallengeActive) return

        val player = event.player
        val block  = event.block

        // Drops berechnen – auch Blätter etc. die normalerweise nix droppen
        val drops: Collection<ItemStack> = block.getDrops(player.inventory.itemInMainHand)
            .ifEmpty { listOf(ItemStack(block.type, 1)) } // Fallback: Block selbst

        event.isDropItems = false

        val mobType  = getMobForBlock(block.type)
        val spawnLoc = block.location.add(0.5, 0.5, 0.5)
        val mob      = spawnLoc.world?.spawnEntity(spawnLoc, mobType) as? LivingEntity ?: return

        // Drops am Mob als NBT speichern
        mob.persistentDataContainer.set(dropKey, PersistentDataType.STRING, serializeDrops(drops))

        mob.isCustomNameVisible = true
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        if (!plugin.isMobDropChallengeActive) return

        val entity     = event.entity
        val dropString = entity.persistentDataContainer.get(dropKey, PersistentDataType.STRING) ?: return

        // Items auf den Boden droppen (wie normaler Mob-Drop)
        deserializeDrops(dropString).forEach { item ->
            entity.world.dropItemNaturally(entity.location, item)
        }
    }

    // ── Serialisierung ────────────────────────────────────────────────────────

    private fun serializeDrops(drops: Collection<ItemStack>): String =
        drops.joinToString(",") { "${it.type.name}:${it.amount}" }

    private fun deserializeDrops(data: String): List<ItemStack> {
        if (data.isBlank()) return emptyList()
        return data.split(",").mapNotNull { entry ->
            val parts  = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val mat    = Material.getMaterial(parts[0]) ?: return@mapNotNull null
            val amount = parts[1].toIntOrNull() ?: 1
            ItemStack(mat, amount)
        }
    }
}