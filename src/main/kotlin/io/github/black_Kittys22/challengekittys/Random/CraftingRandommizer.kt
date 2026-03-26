package io.github.black_Kittys22.challengekittys.Challenges

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.inventory.ItemStack
import java.io.File

class CraftingRandomizer(private val plugin: Main) : Listener {

    private val craftingMappings = mutableMapOf<Material, Material>()

    // Merkt sich das originale Recipe-Ergebnis pro Inventory, damit CraftItemEvent nicht nochmal randomisiert
    private val pendingOriginal = mutableMapOf<Int, Material>()

    // FIX: Blacklist für Items die nicht im Pool landen sollen (nicht-survival-craftbar)
    private val blacklistedMaterials = setOf(
        Material.AIR, Material.BARRIER, Material.COMMAND_BLOCK,
        Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
        Material.COMMAND_BLOCK_MINECART, Material.STRUCTURE_BLOCK,
        Material.STRUCTURE_VOID, Material.JIGSAW, Material.DEBUG_STICK,
        Material.KNOWLEDGE_BOOK, Material.WRITTEN_BOOK,
        Material.BEDROCK, Material.END_PORTAL_FRAME, Material.END_GATEWAY,
        Material.SPAWNER, Material.LIGHT
    )

    // FIX: Blacklist angewendet damit nur sinnvolle Items im Pool landen
    private val itemPool: List<Material> by lazy {
        Material.values().filter {
            it.isItem &&
                    !it.isAir &&
                    !it.name.startsWith("LEGACY_") &&
                    it !in blacklistedMaterials
        }
    }

    // FIX: saveMappings() wird nach neuem Eintrag aufgerufen
    private fun getReplacement(original: Material): Material {
        return craftingMappings.getOrPut(original) {
            itemPool.random().also {
                saveMappings()
            }
        }
    }

    @EventHandler
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        if (!plugin.isCraftingRandomizerActive) return

        val result = event.inventory.result ?: return
        if (result.type == Material.AIR) return

        // Original merken damit CraftItemEvent nicht nochmal randomisiert
        pendingOriginal[event.inventory.hashCode()] = result.type

        val replacementMaterial = getReplacement(result.type)
        event.inventory.result = ItemStack(replacementMaterial, result.amount)
    }

    // FIX: Werker (Crafter Block, 1.21+) – automatisches Crafting wird ebenfalls randomisiert
    @EventHandler
    fun onCrafterCraft(event: CrafterCraftEvent) {
        if (!plugin.isCraftingRandomizerActive) return

        val result = event.result
        if (result.type == Material.AIR) return

        val replacementMaterial = getReplacement(result.type)
        event.result = ItemStack(replacementMaterial, result.amount)
    }

    // FIX: Nutzt das gespeicherte Original damit nicht nochmal randomisiert wird
    @EventHandler
    fun onCraft(event: CraftItemEvent) {
        if (!plugin.isCraftingRandomizerActive) return

        val inventoryHash = event.inventory.hashCode()
        val originalMaterial = pendingOriginal.remove(inventoryHash) ?: return

        val result = event.inventory.result ?: return
        if (result.type == Material.AIR) return

        val replacementMaterial = getReplacement(originalMaterial)
        event.inventory.result = ItemStack(replacementMaterial, result.amount)
    }

    // --- Persistenz ---

    fun saveMappings() {
        val file = File(plugin.dataFolder, "crafting_mappings.yml")
        val yaml = YamlConfiguration()
        craftingMappings.forEach { (orig, repl) ->
            yaml.set(orig.name, repl.name)
        }
        yaml.save(file)
    }

    // FIX: loadMappings() sollte in Main beim Start aufgerufen werden (onEnable)
    fun loadMappings() {
        val file = File(plugin.dataFolder, "crafting_mappings.yml")
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        craftingMappings.clear()
        for (key in yaml.getKeys(false)) {
            val orig = Material.getMaterial(key) ?: continue
            val repl = Material.getMaterial(yaml.getString(key) ?: "") ?: continue
            craftingMappings[orig] = repl
        }
    }

    fun resetAll() {
        craftingMappings.clear()
        File(plugin.dataFolder, "crafting_mappings.yml").delete()
    }
}