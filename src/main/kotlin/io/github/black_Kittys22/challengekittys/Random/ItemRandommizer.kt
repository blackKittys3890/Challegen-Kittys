package io.github.black_Kittys22.challengekittys.Challenges

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.UUID

class RandomizerChallenge(private val plugin: Main) : Listener {

    private val randomPool: List<Material> = Material.values().filter { material ->
        !material.isAir &&
                material.isItem &&
                !material.name.startsWith("LEGACY_")
    }

    private val playerMappings = mutableMapOf<UUID, MutableMap<Material, Material>>()

    private fun getDropFor(playerUUID: UUID, blockMaterial: Material): Material {
        val mapping = playerMappings.getOrPut(playerUUID) { mutableMapOf() }
        return mapping.getOrPut(blockMaterial) {
            randomPool.random().also { saveMappings() }
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!plugin.isRandomizerActive) return
        val player = event.player
        val blockType = event.block.type
        val tool = player.inventory.itemInMainHand

        event.isDropItems = false

        val hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH)

        val fortuneLevel = tool.getEnchantmentLevel(Enchantment.FORTUNE)
        val amount = if (fortuneLevel > 0 && !hasSilkTouch) {
            val bonus = (1..fortuneLevel).sumOf { (0..1).random() }
            (1 + bonus).coerceAtMost(4)
        } else {
            1
        }

        val dropMaterial = getDropFor(player.uniqueId, blockType)
        val drop = ItemStack(dropMaterial, amount)

        val dropLocation = event.block.location.add(0.5, 0.5, 0.5)
        dropLocation.world?.dropItemNaturally(dropLocation, drop)
    }

    fun saveMappings() {
        val file = File(plugin.dataFolder, "randomizer_mappings.yml")
        val yaml = YamlConfiguration()
        for ((uuid, mapping) in playerMappings) {
            for ((block, drop) in mapping) {
                yaml.set("$uuid.${block.name}", drop.name)
            }
        }
        yaml.save(file)
    }

    fun loadMappings() {
        val file = File(plugin.dataFolder, "randomizer_mappings.yml")
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        playerMappings.clear()
        for (uuidStr in yaml.getKeys(false)) {
            val uuid = try { UUID.fromString(uuidStr) } catch (e: Exception) { continue }
            val section = yaml.getConfigurationSection(uuidStr) ?: continue
            val mapping = mutableMapOf<Material, Material>()
            for (blockName in section.getKeys(false)) {
                val block = Material.getMaterial(blockName) ?: continue
                val drop = Material.getMaterial(section.getString(blockName) ?: "") ?: continue
                mapping[block] = drop
            }
            playerMappings[uuid] = mapping
        }
    }

    fun resetPlayer(uuid: UUID) {
        playerMappings.remove(uuid)
    }

    fun resetAll() {
        playerMappings.clear()
        File(plugin.dataFolder, "randomizer_mappings.yml").delete()
    }
}