package io.github.black_Kittys22.challengekittys.Commands

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

class SpectatorManager(private val plugin: Main) : CommandExecutor, Listener {

    private val spectators = mutableSetOf<java.util.UUID>()
    private val vanished = mutableSetOf<java.util.UUID>()
    private val tpGuiTitle = Component.text("Spieler Teleport", NamedTextColor.DARK_PURPLE)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player || !sender.hasPermission("challenge.spec")) return true

        if (spectators.contains(sender.uniqueId)) {
            // Aus dem Spectator-Modus austreten
            spectators.remove(sender.uniqueId)
            vanished.remove(sender.uniqueId)
            sender.gameMode = GameMode.SURVIVAL
            showPlayer(sender)
            sender.inventory.clear()
            sender.sendMessage(Component.text("Zuschauermodus beendet.", NamedTextColor.RED))
        } else {
            // Spectator-Modus aktivieren
            spectators.add(sender.uniqueId)
            vanished.add(sender.uniqueId)
            sender.gameMode = GameMode.CREATIVE
            hidePlayer(sender)
            giveSpectatorItems(sender)
            sender.sendMessage(Component.text("Zuschauermodus aktiviert (Vanish AN).", NamedTextColor.GREEN))
        }
        return true
    }

    private fun giveSpectatorItems(player: Player) {
        player.inventory.clear()

        // Kompass für TP
        val compass = ItemStack(Material.COMPASS)
        compass.itemMeta = compass.itemMeta?.apply { displayName(Component.text("Teleporter (Rechtsklick)", NamedTextColor.GOLD)) }

        // Truhe für Inventar-Check
        val chest = ItemStack(Material.CHEST)
        chest.itemMeta = chest.itemMeta?.apply { displayName(Component.text("Inventar-Einblick (Rechtsklick auf Spieler)", NamedTextColor.AQUA)) }

        // Sichtbarkeit (Vanish-Item)
        val vanishItem = ItemStack(Material.LIME_DYE)
        vanishItem.itemMeta = vanishItem.itemMeta?.apply { displayName(Component.text("Status: UNSICHTBAR", NamedTextColor.GREEN)) }

        player.inventory.setItem(0, compass)
        player.inventory.setItem(4, chest)
        player.inventory.setItem(8, vanishItem)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!spectators.contains(player.uniqueId)) return

        val item = event.item ?: return
        when (item.type) {
            Material.COMPASS -> {
                if (event.action.name.contains("RIGHT")) openTeleportGUI(player)
            }
            Material.LIME_DYE, Material.GRAY_DYE -> {
                if (event.action.name.contains("RIGHT")) toggleVanish(player)
            }
            else -> {}
        }
    }

    @EventHandler
    fun onEntityInteract(event: PlayerInteractEntityEvent) {
        val observer = event.player
        val target = event.rightClicked
        if (spectators.contains(observer.uniqueId) && target is Player) {
            val item = observer.inventory.itemInMainHand
            if (item.type == Material.CHEST) {
                // Inventar und Herzen anzeigen
                val health = String.format("%.1f", target.health)
                val maxHealth = target.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
                observer.openInventory(target.inventory)
                observer.sendMessage(Component.text("Schaue in das Inventar von ${target.name} (❤ $health / $maxHealth)", NamedTextColor.YELLOW))
            }
        }
    }

    private fun toggleVanish(player: Player) {
        if (vanished.contains(player.uniqueId)) {
            vanished.remove(player.uniqueId)
            showPlayer(player)
            player.inventory.setItem(8, ItemStack(Material.GRAY_DYE).apply {
                itemMeta = itemMeta?.apply { displayName(Component.text("Status: SICHTBAR", NamedTextColor.GRAY)) }
            })
            player.sendMessage(Component.text("Du bist nun für alle sichtbar!", NamedTextColor.GRAY))
        } else {
            vanished.add(player.uniqueId)
            hidePlayer(player)
            player.inventory.setItem(8, ItemStack(Material.LIME_DYE).apply {
                itemMeta = itemMeta?.apply { displayName(Component.text("Status: UNSICHTBAR", NamedTextColor.GREEN)) }
            })
            player.sendMessage(Component.text("Vanish aktiviert!", NamedTextColor.GREEN))
        }
    }

    private fun openTeleportGUI(player: Player) {
        val inv = Bukkit.createInventory(null, 54, tpGuiTitle)
        Bukkit.getOnlinePlayers().filter { it.uniqueId != player.uniqueId }.forEach { target ->
            val skull = ItemStack(Material.PLAYER_HEAD)
            val meta = skull.itemMeta as SkullMeta
            meta.owningPlayer = target
            meta.displayName(Component.text(target.name, NamedTextColor.YELLOW))
            skull.itemMeta = meta
            inv.addItem(skull)
        }
        player.openInventory(inv)
    }

    @EventHandler
    fun onGuiClick(event: InventoryClickEvent) {
        if (event.view.title() != tpGuiTitle) return
        event.isCancelled = true
        val player = event.whoClicked as Player
        val clickedItem = event.currentItem ?: return

        if (clickedItem.type == Material.PLAYER_HEAD) {
            val meta = clickedItem.itemMeta as SkullMeta
            val target = meta.owningPlayer?.player
            if (target != null) {
                player.teleport(target.location)
                player.sendMessage(Component.text("Zu ${target.name} teleportiert.", NamedTextColor.GRAY))
            }
        }
    }

    private fun hidePlayer(player: Player) {
        Bukkit.getOnlinePlayers().forEach { it.hidePlayer(plugin, player) }
    }

    private fun showPlayer(player: Player) {
        Bukkit.getOnlinePlayers().forEach { it.showPlayer(plugin, player) }
    }
}