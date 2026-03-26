package io.github.black_Kittys22.challengekittys.AllItems

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import java.util.*

class AllItemsListener(private val plugin: Main) : Listener {


    fun getProgressString(): String = "$collectedCount/${possibleItemsPool.size}"
    private var currentTarget: Material? = null
    private var collectedCount = 0
    private val collectedItems = mutableListOf<String>()
    private val bossBar: BossBar = Bukkit.createBossBar("Lade Challenge...", BarColor.YELLOW, BarStyle.SOLID)

    private val possibleItemsPool = Material.entries.filter {
        it.isItem && !it.isAir && !it.name.contains("LEGACY") && !it.name.contains("SPAWN_EGG") && !it.name.contains("BEDROCK")
    }

    init {
        loadProgress()
    }

    fun selectNextItem() {
        val remainingItems = possibleItemsPool.filter { !collectedItems.contains(it.name) }

        if (remainingItems.isEmpty()) {
            currentTarget = null
            broadcastChallengeComplete()
        } else {
            currentTarget = remainingItems.random()
        }

        updateBossBar()
        saveProgress()
    }

    private fun updateBossBar() {
        val itemName = currentTarget?.name?.replace("_", " ")?.lowercase()?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        } ?: "ALLE GEFUNDEN!"

        bossBar.setTitle("§6Item: §e$itemName §7(§a$collectedCount§7/§2${possibleItemsPool.size}§7)")
        bossBar.progress = 1.0

        Bukkit.getOnlinePlayers().forEach { bossBar.addPlayer(it) }
    }

    @EventHandler
    fun onPickup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        if (!plugin.isAllItemsChallengeActive) return
        if (plugin.exemptPlayers.contains(player.uniqueId)) return  // Exempt-Check

        if (event.item.itemStack.type == currentTarget) {
            collectedItems.add(currentTarget!!.name)
            collectedCount++
            broadcastSuccess(player, currentTarget!!)
            plugin.updateTablist()
            selectNextItem()
        }
    }

    private fun broadcastSuccess(player: Player, material: Material) {
        val msg = Component.text("✓ ", NamedTextColor.GREEN)
            .append(Component.text(player.name, NamedTextColor.YELLOW))
            .append(Component.text(" hat das Item gefunden: ", NamedTextColor.GRAY))
            .append(Component.text(material.name.replace("_", " "), NamedTextColor.GOLD, TextDecoration.BOLD))

        Bukkit.broadcast(msg)
        Bukkit.getOnlinePlayers().forEach { it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f) }
    }

    private fun broadcastChallengeComplete() {
        Bukkit.broadcast(Component.text("!!! DIE ALL-ITEMS CHALLENGE WURDE ABGESCHLOSSEN !!!", NamedTextColor.AQUA, TextDecoration.BOLD))
    }

    fun saveProgress() {
        plugin.config.set("challenges.allItems.collectedCount", collectedCount)
        plugin.config.set("challenges.allItems.currentTarget", currentTarget?.name)
        plugin.config.set("challenges.allItems.collectedItemsList", collectedItems)
        plugin.saveConfig()
    }

    private fun loadProgress() {
        collectedCount = plugin.config.getInt("challenges.allItems.collectedCount", 0)

        val savedList = plugin.config.getStringList("challenges.allItems.collectedItemsList")
        collectedItems.clear()
        collectedItems.addAll(savedList)

        val savedMaterial = plugin.config.getString("challenges.allItems.currentTarget")
        currentTarget = if (savedMaterial != null) {
            Material.getMaterial(savedMaterial)
        } else {
            null
        }

        if (currentTarget == null && collectedCount == 0) {
            selectNextItem()
        } else {
            updateBossBar()
        }
    }

    fun reset() {
        collectedCount = 0
        collectedItems.clear()
        hideBar()
        selectNextItem()
    }

    fun showBar(player: Player) {
        if (plugin.isAllItemsChallengeActive) bossBar.addPlayer(player)
    }

    fun hideBar() {
        bossBar.removeAll()
    }
}