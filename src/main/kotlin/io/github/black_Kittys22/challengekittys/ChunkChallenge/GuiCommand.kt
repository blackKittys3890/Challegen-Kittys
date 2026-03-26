package io.github.black_Kittys22.challengekittys.ChunkChallenge

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class GuiCommand(private val plugin: Main) : CommandExecutor {

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true

        // Erstelle ein Inventar mit 54 Slots (6 Reihen)
        val inv = Bukkit.createInventory(null, 54, Component.text("Chunk Blacklist", NamedTextColor.DARK_GRAY))

        // FIX: Zugriff direkt auf plugin.blacklistedMaterials (nicht plugin.manager)
        val blacklist = plugin.blacklistedMaterials.toList()

        for (i in blacklist.indices) {
            if (i >= 54) break
            val material = blacklist[i]

            // Falls das Material AIR ist, überspringen wir es, um leere Slots zu vermeiden
            if (material == Material.AIR) continue

            val item = ItemStack(material)
            val meta = item.itemMeta

            if (meta != null) {
                // Anzeige-Name ohne kursiv (decoration clean)
                meta.displayName(Component.text(material.name, NamedTextColor.RED)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))

                val lore = mutableListOf<Component>()
                lore.add(Component.text("Klicken zum Entfernen", NamedTextColor.GRAY)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))

                meta.lore(lore)
                item.itemMeta = meta
            }
            inv.setItem(i, item)
        }

        sender.openInventory(inv)
        return true
    }
}