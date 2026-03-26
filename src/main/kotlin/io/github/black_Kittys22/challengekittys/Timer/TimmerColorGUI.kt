package io.github.black_Kittys22.challengekittys.Timer

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

class TimerColorGUI(private val plugin: Main) : Listener {

    private val guiTitle = Component.text("Timer Farbe wählen", NamedTextColor.DARK_GRAY)

    fun openGUI(player: Player) {
        val inv = Bukkit.createInventory(null, 9, guiTitle)

        // Slot 2: Schwarz/Grau
        val blackGray = ItemStack(Material.GRAY_DYE)
        blackGray.editMeta { meta ->
            meta.displayName(
                Component.text("✦ Schwarz/Grau", NamedTextColor.GRAY, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false)
            )
            meta.lore(listOf(
                Component.text("Edler Grau-Schwarz Verlauf", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                if (plugin.timer.colorTheme == TimerColorTheme.BLACK_GRAY)
                    Component.text("» Aktuell ausgewählt «", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)
                else
                    Component.text("Klicken zum Auswählen", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
            ))
        }

        // Slot 4: Grün
        val green = ItemStack(Material.LIME_DYE)
        green.editMeta { meta ->
            meta.displayName(
                Component.text("✦ Grün", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false)
            )
            meta.lore(listOf(
                Component.text("Frischer Grün-Verlauf", NamedTextColor.DARK_GREEN)
                    .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                if (plugin.timer.colorTheme == TimerColorTheme.GREEN)
                    Component.text("» Aktuell ausgewählt «", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)
                else
                    Component.text("Klicken zum Auswählen", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
            ))
        }

        // Slot 6: Lila
        val purple = ItemStack(Material.PURPLE_DYE)
        purple.editMeta { meta ->
            meta.displayName(
                Component.text("✦ Lila", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false)
            )
            meta.lore(listOf(
                Component.text("Mystischer Lila-Verlauf", NamedTextColor.DARK_PURPLE)
                    .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                if (plugin.timer.colorTheme == TimerColorTheme.PURPLE)
                    Component.text("» Aktuell ausgewählt «", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)
                else
                    Component.text("Klicken zum Auswählen", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
            ))
        }

        // Glasscheiben als Füller
        val filler = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        filler.editMeta { meta ->
            meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false))
        }
        for (i in 0 until 9) inv.setItem(i, filler)

        inv.setItem(2, blackGray)
        inv.setItem(4, green)
        inv.setItem(6, purple)

        player.openInventory(inv)
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        if (e.view.title() != guiTitle) return
        e.isCancelled = true

        val player = e.whoClicked as? Player ?: return
        if (!player.hasPermission("timer.use")) return

        val theme = when (e.rawSlot) {
            2 -> TimerColorTheme.BLACK_GRAY
            4 -> TimerColorTheme.GREEN
            6 -> TimerColorTheme.PURPLE
            else -> return
        }

        plugin.timer.colorTheme = theme
        player.sendMessage(
            Component.text("Timer-Farbe auf ", NamedTextColor.GRAY)
                .append(Component.text(theme.displayName, NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" gesetzt!", NamedTextColor.GRAY))
        )
        // GUI neu öffnen, damit der "ausgewählt"-Status aktualisiert wird
        openGUI(player)
    }
}