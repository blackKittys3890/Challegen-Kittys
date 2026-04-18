package io.github.black_Kittys22.challengekittys.SuperChallenges

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class SuperChallengeManager(private val plugin: Main) : Listener {

    private val mainTitle = Component.text("✦ Super Challenges ✦", NamedTextColor.GOLD, TextDecoration.BOLD)

    fun openMainGUI(player: Player) {
        val inv = Bukkit.createInventory(null, 27, mainTitle)

        val filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ")
        for (i in 0 until 27) inv.setItem(i, filler)

        inv.setItem(13, buildBeaconItem())

        player.openInventory(inv)
    }

    private fun buildBeaconItem(): ItemStack {
        val ch     = plugin.fullNetheriteBeaconChallenge
        val active = ch.isActive
        val done   = ch.isCompleted

        val item = ItemStack(Material.BEACON)
        val meta = item.itemMeta!!

        meta.displayName(
            Component.text("Full Netherite Beacon",
                when { done -> NamedTextColor.GOLD; active -> NamedTextColor.GREEN; else -> NamedTextColor.RED }
            ).decoration(TextDecoration.ITALIC, false)
        )

        meta.lore(listOf(
            Component.text(
                when { done -> "✔ GESCHAFFT!"; active -> "AKTIV"; else -> "INAKTIV" },
                when { done -> NamedTextColor.GOLD; active -> NamedTextColor.GREEN; else -> NamedTextColor.RED }
            ).decoration(TextDecoration.ITALIC, false),
            Component.text("").decoration(TextDecoration.ITALIC, false),
            Component.text("Hol das Advancement ", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Beaconator", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)),
            Component.text("mit einer Pyramide aus", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("Netherite-Blöcken", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(".", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
            Component.text("").decoration(TextDecoration.ITALIC, false),
            Component.text(
                "» Klicken zum " + if (active) "Deaktivieren" else "Aktivieren",
                NamedTextColor.YELLOW
            ).decoration(TextDecoration.ITALIC, false)
        ))

        if (active || done) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }

        item.itemMeta = meta
        return item
    }

    @EventHandler
    fun onClick(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return
        if (e.view.title() != mainTitle) return
        e.isCancelled = true

        val item = e.currentItem ?: return
        if (item.type == Material.AIR || item.type == Material.BLACK_STAINED_GLASS_PANE) return

        if (e.slot == 13) {
            val ch = plugin.fullNetheriteBeaconChallenge
            ch.isActive = !ch.isActive
            if (!ch.isActive) ch.isCompleted = false
            player.sendMessage(
                if (ch.isActive)
                    Component.text("[Super-Challenge] Full Netherite Beacon aktiviert!", NamedTextColor.GREEN)
                else
                    Component.text("[Super-Challenge] Full Netherite Beacon deaktiviert.", NamedTextColor.RED)
            )
            openMainGUI(player)
        }
    }

    private fun makeItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false))
        item.itemMeta = meta
        return item
    }
}