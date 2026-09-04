package io.github.black_Kittys22.challengekittys.CollectChallenges.AllAchievments

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.advancement.Advancement
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import java.util.*

class AllAchievments(private val plugin: Main) : Listener {

    fun getProgressString(): String = "$collectedCount/${advancementPool.size}"

    private var currentTarget: NamespacedKey? = null
    private var collectedCount = 0
    private val completedAdvancements = mutableListOf<String>()

    private val bossBarName: BossBar = BossBar.bossBar(
        Component.text("Lade Advancements..."),
        1.0f,
        BossBar.Color.GREEN,
        BossBar.Overlay.PROGRESS
    )
    private val bossBarDesc: BossBar = BossBar.bossBar(
        Component.text(""),
        0.999f,
        BossBar.Color.YELLOW,
        BossBar.Overlay.PROGRESS
    )

    private val advancementPool = mutableListOf<NamespacedKey>()
    private var isReady = false

    init {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            buildAdvancementPool()
            loadProgress()
            isReady = true
            if (plugin.isSharedAdvancementsActive) {
                refreshBarForAll()
            }
        }, 5L)
    }

    private fun buildAdvancementPool() {
        advancementPool.clear()
        val iterator: Iterator<Advancement> = Bukkit.advancementIterator()
        while (iterator.hasNext()) {
            val adv = iterator.next()
            val key = adv.key.key
            if (key.endsWith("/root") || key == "root") continue
            if (adv.key.namespace == "minecraft" && key.startsWith("recipes/")) continue
            advancementPool.add(adv.key)
        }
        plugin.logger.info("[AllAdvancements] Pool geladen: ${advancementPool.size} Advancements")
    }

    private fun getAdvancementDescription(key: NamespacedKey): Component? {
        return try {
            Bukkit.getAdvancement(key)?.display?.description()
        } catch (e: Exception) { null }
    }

    private fun refreshBarForAll() {
        val name = currentTarget?.let { formatName(it.key) } ?: "ALLE GESCHAFFT!"
        val desc: Component? = currentTarget?.let { getAdvancementDescription(it) }

        bossBarName.name(
            Component.text("", NamedTextColor.WHITE)
                .append(Component.text(name, NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text("  ($collectedCount/${advancementPool.size})", NamedTextColor.GRAY))
        )
        bossBarName.progress(1.0f)

        if (desc != null) {
            bossBarDesc.name(desc)
            bossBarDesc.progress(0.999f)
            Bukkit.getOnlinePlayers().forEach { player ->
                player.showBossBar(bossBarName)
                player.showBossBar(bossBarDesc)
            }
        } else {
            Bukkit.getOnlinePlayers().forEach { player ->
                player.showBossBar(bossBarName)
                player.hideBossBar(bossBarDesc)
            }
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (plugin.isSharedAdvancementsActive && isReady) {
                event.player.showBossBar(bossBarName)
                val desc: Component? = currentTarget?.let { getAdvancementDescription(it) }
                if (desc != null) event.player.showBossBar(bossBarDesc)

                checkAndAwardTarget(event.player)
            }
        }, 5L)
    }

    private fun checkAndAwardTarget(triggerPlayer: Player? = null) {
        val target = currentTarget ?: return
        val advancement = Bukkit.getAdvancement(target) ?: return

        // Nur nicht-exemptierte Spieler als "Achiever" zählen
        val achiever = Bukkit.getOnlinePlayers().firstOrNull { player ->
            !plugin.exemptPlayers.contains(player.uniqueId) &&  // Exempt-Check
                    player.getAdvancementProgress(advancement).isDone
        } ?: return

        completedAdvancements.add(target.toString())
        collectedCount++
        broadcastSuccess(achiever, advancement)

        // Advancement an alle nicht-exemptierten Spieler vergeben
        for (other in Bukkit.getOnlinePlayers()) {
            if (plugin.exemptPlayers.contains(other.uniqueId)) continue  // Exempt-Check
            if (other.uniqueId == achiever.uniqueId) continue
            val progress = other.getAdvancementProgress(advancement)
            if (!progress.isDone) {
                progress.remainingCriteria.forEach { progress.awardCriteria(it) }
            }
        }

        selectNextAdvancement()
        plugin.updateTablist()
    }

    @EventHandler
    fun onAdvancementDone(event: PlayerAdvancementDoneEvent) {
        if (!plugin.isSharedAdvancementsActive || !isReady) return

        val player = event.player
        if (plugin.exemptPlayers.contains(player.uniqueId)) return  // Exempt-Check

        val advancement = event.advancement
        val key = advancement.key

        if (key.namespace == "minecraft" && key.key.startsWith("recipes/")) return

        event.message(null)

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (key == currentTarget) {
                completedAdvancements.add(key.toString())
                collectedCount++
                broadcastSuccess(player, advancement)

                // Nur an nicht-exemptierte Spieler vergeben
                for (other in Bukkit.getOnlinePlayers()) {
                    if (plugin.exemptPlayers.contains(other.uniqueId)) continue  // Exempt-Check
                    if (other.uniqueId == player.uniqueId) continue
                    val progress = other.getAdvancementProgress(advancement)
                    if (!progress.isDone) {
                        progress.remainingCriteria.forEach { progress.awardCriteria(it) }
                    }
                }

                selectNextAdvancement()
                plugin.updateTablist()
            }
        }, 1L)
    }

    fun selectNextAdvancement() {
        val remaining = advancementPool.filter { !completedAdvancements.contains(it.toString()) }
        currentTarget = if (remaining.isEmpty()) {
            broadcastChallengeComplete()
            null
        } else {
            val noOneHas = remaining.filter { key ->
                val adv = Bukkit.getAdvancement(key) ?: return@filter true
                // Nur nicht-exemptierte Spieler berücksichtigen
                Bukkit.getOnlinePlayers()
                    .filter { !plugin.exemptPlayers.contains(it.uniqueId) }  // Exempt-Check
                    .none { it.getAdvancementProgress(adv).isDone }
            }
            val pool = if (noOneHas.isNotEmpty()) noOneHas else remaining
            pool.random()
        }

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            checkAndAwardTarget()
        }, 1L)

        refreshBarForAll()
        saveProgress()
    }

    private fun broadcastSuccess(player: Player, advancement: Advancement) {
        val name = formatName(advancement.key.key)
        val msg = Component.text("", NamedTextColor.GOLD)
            .append(Component.text(player.name, NamedTextColor.YELLOW))
            .append(Component.text(" hat freigeschaltet: ", NamedTextColor.GRAY))
            .append(Component.text(name, NamedTextColor.GREEN, TextDecoration.BOLD))
            .append(Component.text(" – alle haben es erhalten!", NamedTextColor.GRAY))
        Bukkit.broadcast(msg)
        Bukkit.getOnlinePlayers().forEach { it.playSound(it.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.0f) }
    }

    private fun broadcastChallengeComplete() {
        Bukkit.broadcast(Component.text("!!! ALLE ADVANCEMENTS WURDEN FREIGESCHALTET !!!", NamedTextColor.GOLD, TextDecoration.BOLD))
        Bukkit.getOnlinePlayers().forEach { it.playSound(it.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f) }
    }

    private fun formatName(key: String): String =
        key.substringAfterLast("/")
            .replace("_", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    fun saveProgress() {
        plugin.config.set("challenges.allAdvancements.collectedCount", collectedCount)
        plugin.config.set("challenges.allAdvancements.currentTarget", currentTarget?.toString())
        plugin.config.set("challenges.allAdvancements.completedList", completedAdvancements)
        plugin.saveConfig()
    }

    private fun loadProgress() {
        collectedCount = plugin.config.getInt("challenges.allAdvancements.collectedCount", 0)
        val savedList = plugin.config.getStringList("challenges.allAdvancements.completedList")
        completedAdvancements.clear()
        completedAdvancements.addAll(savedList)

        val savedTarget = plugin.config.getString("challenges.allAdvancements.currentTarget")
        currentTarget = savedTarget?.let {
            try {
                val parts = it.split(":")
                if (parts.size == 2) NamespacedKey(parts[0], parts[1]) else null
            } catch (e: Exception) { null }
        }

        if (currentTarget == null && collectedCount == 0) {
            selectNextAdvancement()
        } else {
            refreshBarForAll()
        }
    }

    fun reset() {
        collectedCount = 0
        completedAdvancements.clear()
        hideBar()
        selectNextAdvancement()
    }

    fun showBar(player: Player) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            refreshBarForAll()
        }, 2L)
    }

    fun hideBar() {
        Bukkit.getOnlinePlayers().forEach {
            it.hideBossBar(bossBarName)
            it.hideBossBar(bossBarDesc)
        }
    }
}