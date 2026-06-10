package io.github.black_Kittys22.challengekittys.ChunkChallenge

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class BlocklistGuiListener(
    private val plugin: Main,
    private val guiCommand: BlocklistGuiCommand
) : Listener {

    // Welche Seite hat welcher Spieler gerade offen
    private val playerPages = mutableMapOf<java.util.UUID, Int>()

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = PlainTextComponentSerializer.plainText().serialize(event.view.title())

        // Prüfen ob unser GUI
        if (!title.startsWith("Blockliste")) return
        event.isCancelled = true

        val slot = event.rawSlot
        val page = playerPages[player.uniqueId] ?: 0
        val totalPages = (BlocklistGuiCommand.ALL_BLOCKS.size + BlocklistGuiCommand.PAGE_SIZE - 1) / BlocklistGuiCommand.PAGE_SIZE

        when {
            // ── Navigation ───────────────────────────────────────────────────
            slot == 45 && page > 0 -> {
                val newPage = page - 1
                playerPages[player.uniqueId] = newPage
                guiCommand.openPage(player, newPage)
            }
            slot == 53 && page < totalPages - 1 -> {
                val newPage = page + 1
                playerPages[player.uniqueId] = newPage
                guiCommand.openPage(player, newPage)
            }
            slot == 49 -> return // Seitenanzeige, nichts tun

            // ── Block togglen ─────────────────────────────────────────────────
            slot in 0 until BlocklistGuiCommand.PAGE_SIZE -> {
                val index = page * BlocklistGuiCommand.PAGE_SIZE + slot
                if (index >= BlocklistGuiCommand.ALL_BLOCKS.size) return

                val mat = BlocklistGuiCommand.ALL_BLOCKS[index]

                if (plugin.blacklistedMaterials.contains(mat)) {
                    plugin.blacklistedMaterials.remove(mat)
                    player.sendMessage("§a${mat.name} ist jetzt ERLAUBT.")
                } else {
                    plugin.blacklistedMaterials.add(mat)
                    player.sendMessage("§c${mat.name} ist jetzt VERBOTEN.")
                }

                // Sofort speichern
                saveBlacklist()

                // GUI aktualisieren
                guiCommand.openPage(player, page)
            }
        }
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val title = PlainTextComponentSerializer.plainText().serialize(event.view.title())
        if (title.startsWith("Blockliste")) {
            playerPages.remove(event.player.uniqueId)
        }
    }

    private fun saveBlacklist() {
        // 1. In plugin config speichern (wie bisher)
        plugin.savePluginConfig()

        // 2. Zusätzlich in eigene blacklist.yml speichern
        val file = File(plugin.dataFolder, "blacklist.yml")
        val yaml = YamlConfiguration()
        yaml.set("blacklistedBlocks", plugin.blacklistedMaterials.map { it.name })
        yaml.set("count", plugin.blacklistedMaterials.size)
        yaml.set("lastUpdated", System.currentTimeMillis())
        yaml.save(file)
    }
}