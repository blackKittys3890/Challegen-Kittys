package io.github.black_Kittys22.challengekittys.AllMobs

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
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import java.util.*

class AllMobsListener(private val plugin: Main) : Listener {

    fun getProgressString(): String = "$collectedCount/${possibleMobsPool.size}"
    private var currentTarget: EntityType? = null
    private var collectedCount = 0
    private val killedMobs = mutableListOf<String>()
    private val bossBar: BossBar = Bukkit.createBossBar("Lade Mobs...", BarColor.RED, BarStyle.SOLID)

    private val possibleMobsPool = EntityType.entries.filter {
        it.isAlive && it.isSpawnable && it != EntityType.PLAYER && it != EntityType.ARMOR_STAND
    }

    init {
        loadProgress()
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (plugin.isAllMobsChallengeActive) {
            bossBar.addPlayer(event.player)
        }
    }

    @EventHandler
    fun onTrophyInteract(event: PlayerInteractEvent) {
        val item = event.item ?: return
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return

        val meta = item.itemMeta ?: return
        val lore = meta.lore() ?: return

        if (lore.any { it.toString().contains("UNZERSTÖRBAR") }) {
            event.isCancelled = true
            event.player.sendMessage(Component.text("🛡 Diese Trophäe ist versiegelt und kann nicht benutzt werden!", NamedTextColor.RED))
        }
    }

    @EventHandler
    fun onAnvilRename(event: PrepareAnvilEvent) {
        val item = event.inventory.getItem(0) ?: return
        val meta = item.itemMeta ?: return
        val lore = meta.lore() ?: return

        if (lore.any { it.toString().contains("UNZERSTÖRBAR") }) {
            event.result = null
        }
    }

    @EventHandler
    fun onMobKill(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        if (!plugin.isAllMobsChallengeActive) return
        if (plugin.exemptPlayers.contains(killer.uniqueId)) return  // Exempt-Check

        if (event.entity.type == currentTarget) {
            killedMobs.add(currentTarget!!.name)
            collectedCount++

            broadcastSuccess(killer, currentTarget!!)
            giveTrophy(killer, currentTarget!!)
            selectNextMob()
            plugin.updateTablist()
        }
    }

    private fun giveTrophy(player: Player, type: EntityType) {
        val mobName = type.name.replace("_", " ")

        val materialName = "${type.name}_SPAWN_EGG"
        val eggMaterial = Material.getMaterial(materialName) ?: Material.NETHER_STAR

        val trophy = ItemStack(eggMaterial)
        val meta = trophy.itemMeta ?: return

        meta.displayName(
            Component.text("Trophäe: $mobName", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false)
        )

        val lore = mutableListOf<Component>()
        lore.add(Component.text("Besiegt von: ", NamedTextColor.GRAY)
            .append(Component.text(player.name, NamedTextColor.YELLOW))
            .decoration(TextDecoration.ITALIC, false))

        lore.add(Component.text("Fortschritt: ", NamedTextColor.GRAY)
            .append(Component.text("$collectedCount / ${possibleMobsPool.size}", NamedTextColor.GREEN))
            .decoration(TextDecoration.ITALIC, false))

        lore.add(Component.text("UNZERSTÖRBAR", NamedTextColor.DARK_RED, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false))

        meta.lore(lore)
        meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true)
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)

        trophy.itemMeta = meta

        val leftover = player.inventory.addItem(trophy)
        leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
    }

    fun selectNextMob() {
        val remainingMobs = possibleMobsPool.filter { !killedMobs.contains(it.name) }
        if (remainingMobs.isEmpty()) {
            currentTarget = null
            broadcastChallengeComplete()
        } else {
            currentTarget = remainingMobs.random()
        }
        updateBossBar()
        saveProgress()
    }

    private fun updateBossBar() {
        val mobName = currentTarget?.name?.replace("_", " ")?.lowercase()?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        } ?: "ALLE BESIEGT!"

        bossBar.setTitle("§cMob: §e$mobName §7(§a$collectedCount§7/§2${possibleMobsPool.size}§7)")
        bossBar.progress = 1.0
        Bukkit.getOnlinePlayers().forEach { bossBar.addPlayer(it) }
    }

    private fun broadcastSuccess(player: Player, type: EntityType) {
        val name = type.name.replace("_", " ")
        val msg = Component.text("⚔ ", NamedTextColor.RED)
            .append(Component.text(player.name, NamedTextColor.YELLOW))
            .append(Component.text(" hat besiegt: ", NamedTextColor.GRAY))
            .append(Component.text(name, NamedTextColor.GOLD, TextDecoration.BOLD))

        Bukkit.broadcast(msg)
        Bukkit.getOnlinePlayers().forEach { it.playSound(it.location, Sound.ENTITY_WITHER_DEATH, 0.3f, 2.0f) }
    }

    private fun broadcastChallengeComplete() {
        Bukkit.broadcast(Component.text("!!! ALLE MOBS WURDEN BESIEGT !!!", NamedTextColor.DARK_RED, TextDecoration.BOLD))
    }

    fun saveProgress() {
        plugin.config.set("challenges.allMobs.collectedCount", collectedCount)
        plugin.config.set("challenges.allMobs.currentTarget", currentTarget?.name)
        plugin.config.set("challenges.allMobs.killedMobsList", killedMobs)
        plugin.saveConfig()
    }

    private fun loadProgress() {
        collectedCount = plugin.config.getInt("challenges.allMobs.collectedCount", 0)
        val savedList = plugin.config.getStringList("challenges.allMobs.killedMobsList")
        killedMobs.clear()
        killedMobs.addAll(savedList)

        val savedMob = plugin.config.getString("challenges.allMobs.currentTarget")
        currentTarget = savedMob?.let {
            try { EntityType.valueOf(it) } catch (e: Exception) { null }
        }

        if (currentTarget == null && collectedCount == 0) {
            selectNextMob()
        } else {
            updateBossBar()
        }
    }

    fun reset() {
        collectedCount = 0
        killedMobs.clear()
        hideBar()
        selectNextMob()
    }

    fun showBar(player: Player) {
        if (plugin.isAllMobsChallengeActive) bossBar.addPlayer(player)
    }

    fun hideBar() {
        bossBar.removeAll()
    }
}