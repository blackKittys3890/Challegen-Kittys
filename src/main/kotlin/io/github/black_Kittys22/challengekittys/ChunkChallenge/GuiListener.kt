package io.github.black_Kittys22.challengekittys.ChunkChallenge

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class GuiListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val view = event.view
        val titleComponent = view.title()

        // Wandle das Title-Component in Text um
        val title = PlainTextComponentSerializer.plainText().serialize(titleComponent)

        // Prüfen, ob es unser GUI ist
        if (title == "Chunk Blacklist") {
            event.isCancelled = true

            val player = event.whoClicked as? Player ?: return
            val clickedItem = event.currentItem ?: return
            val material = clickedItem.type

            if (material == Material.AIR) return

            // FIX: Zugriff direkt auf plugin.blacklistedMaterials (nicht plugin.manager)
            if (plugin.blacklistedMaterials.contains(material)) {
                plugin.blacklistedMaterials.remove(material)
                player.sendMessage("§a${material.name} wurde von der Blacklist entfernt!")
            } else {
                plugin.blacklistedMaterials.add(material)
                player.sendMessage("§c${material.name} wurde zur Blacklist hinzugefügt.")
            }

            // FIX: Speichern über die Methode in der Main-Klasse
            plugin.savePluginConfig()
            player.closeInventory()
        }
    }
}