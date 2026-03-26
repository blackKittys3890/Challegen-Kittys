package io.github.black_Kittys22.challengekittys.SharedInventoryChallenge

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockBreakEvent

class SharedInvListener(private val plugin: Main) : Listener {

    private fun sync(source: Player) {
        if (!plugin.isSharedInventoryActive) return
        val group = plugin.sharedInvGroups[source.uniqueId] ?: return
        val items = source.inventory.contents

        Bukkit.getOnlinePlayers().forEach {
            if (it.uniqueId != source.uniqueId && plugin.sharedInvGroups[it.uniqueId] == group) {
                it.inventory.contents = items
            }
        }
    }

    // FIX: Unit return type explizit durch geschweifte Klammern – kein BukkitTask mehr zurückgegeben
    private fun later(p: Player) { Bukkit.getScheduler().runTask(plugin, Runnable { sync(p) }) }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onClick(e: InventoryClickEvent) { later(e.whoClicked as Player) }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDrop(e: PlayerDropItemEvent) { later(e.player) }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPickup(e: PlayerAttemptPickupItemEvent) { later(e.player) }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlace(e: BlockPlaceEvent) { later(e.player) }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBreak(e: BlockBreakEvent) { later(e.player) }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBucketE(e: PlayerBucketEmptyEvent) { later(e.player) }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBucketF(e: PlayerBucketFillEvent) { later(e.player) }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInteract(e: PlayerInteractEvent) { if (e.action.name.contains("RIGHT")) later(e.player) }
}