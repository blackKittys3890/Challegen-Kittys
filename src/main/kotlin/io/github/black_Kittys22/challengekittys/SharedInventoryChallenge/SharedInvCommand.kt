package io.github.black_Kittys22.challengekittys.Challenges

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class SharedInvCommand(private val plugin: Main) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("challenge.admin")) return true

        if (args.size < 3) {
            sender.sendMessage(Component.text("Nutze: /shareinv <add|remove> <Spieler> <Gruppe>", NamedTextColor.RED))
            return true
        }

        val action = args[0].lowercase()
        val target = Bukkit.getPlayer(args[1])
        val groupName = args[2]

        if (target == null) {
            sender.sendMessage(Component.text("Spieler nicht gefunden!", NamedTextColor.RED))
            return true
        }

        if (action == "add") {
            plugin.sharedInvGroups[target.uniqueId] = groupName
            sender.sendMessage(Component.text("${target.name} wurde Gruppe $groupName hinzugefügt!", NamedTextColor.GREEN))
        } else if (action == "remove") {
            plugin.sharedInvGroups.remove(target.uniqueId)
            sender.sendMessage(Component.text("${target.name} wurde aus allen Gruppen entfernt!", NamedTextColor.YELLOW))
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("add", "remove")
            2 -> Bukkit.getOnlinePlayers().map { it.name }
            else -> emptyList()
        }
    }
}