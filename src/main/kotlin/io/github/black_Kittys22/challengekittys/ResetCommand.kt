package io.github.black_Kittys22.challengekittys

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File

class ResetCommand(private val plugin: Main) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true
        if (!sender.hasPermission("challenge.reset")) return true

        if (args.isEmpty() || args[0] != "confirm") {
            sender.sendMessage(Component.text("Nutze: /reset confirm um alle Welten zu löschen!", NamedTextColor.RED))
            return true
        }

        val broadcastHeader = Component.text("SERVER-RESET", NamedTextColor.DARK_RED, TextDecoration.BOLD)
        val broadcastInfo = Component.text("Reset ausgelöst von: ", NamedTextColor.GRAY)
            .append(Component.text(sender.name, NamedTextColor.YELLOW))

        Bukkit.broadcast(broadcastHeader)
        Bukkit.broadcast(broadcastInfo)

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {



            val kickMessage = Component.text("Server Reset\n\n", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("Die Welten wurden von ", NamedTextColor.GRAY))
                .append(Component.text(sender.name, NamedTextColor.YELLOW))
                .append(Component.text(" gelöscht.\n", NamedTextColor.GRAY))
                .append(Component.text("Der Server startet nun neu!", NamedTextColor.WHITE))

            Bukkit.getOnlinePlayers().forEach { it.kick(kickMessage) }

            // Reset All Items Challenge
            plugin.allItemsListener.reset()
            plugin.isAllItemsChallengeActive = false

            Bukkit.getWorlds().forEach { Bukkit.unloadWorld(it, false) }

            deleteWorldFolder(File("world"))
            deleteWorldFolder(File("world_nether"))
            deleteWorldFolder(File("world_the_end"))

            plugin.allItemsListener.reset()
            plugin.allMobsListener.reset()
            plugin.craftingRandomizer.resetAll()
            plugin.mobDropChallenge.resetMap()
            plugin.mobRandomizerChallenge.resetAll()
            plugin.swapKeysChallenge.reset()
            plugin.farbspurChallenge.reset()
            plugin.randomizerChallenge.resetAll()
            plugin.chainedTogetherChallenge.resetFixedRoles()
            plugin.updateTablist()

            File(plugin.dataFolder, "timer_data.yml").delete()

            Bukkit.shutdown()

        }, 60L)

        return true
    }

    private fun deleteWorldFolder(path: File) {
        if (path.exists()) {
            path.deleteRecursively()
        }
    }
}