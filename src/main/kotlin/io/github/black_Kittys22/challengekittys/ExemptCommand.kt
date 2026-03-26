package io.github.black_Kittys22.challengekittys.Commands

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ExemptCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("challenge.exempt")) return true
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Nutze: /exempt <Spieler>", NamedTextColor.RED))
            return true
        }

        val target = plugin.server.getPlayerExact(args[0])
            ?: run {
                sender.sendMessage(Component.text("Spieler nicht gefunden!", NamedTextColor.RED))
                return true
            }

        if (plugin.exemptPlayers.contains(target.uniqueId)) {
            plugin.exemptPlayers.remove(target.uniqueId)
            plugin.saveConfig()  // ← direkt persistieren
            sender.sendMessage(Component.text("${target.name} ist jetzt NICHT mehr von Challenges befreit.", NamedTextColor.RED))
            target.sendMessage(Component.text("Du nimmst wieder an allen Challenges teil.", NamedTextColor.RED))
        } else {
            plugin.exemptPlayers.add(target.uniqueId)
            plugin.saveConfig()  // ← direkt persistieren
            sender.sendMessage(Component.text("${target.name} ist jetzt von allen Challenges befreit.", NamedTextColor.GREEN))
            target.sendMessage(Component.text("Du bist jetzt von allen Challenges befreit!", NamedTextColor.GREEN))
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[0], true) }
        }
        return emptyList()
    }
}