package io.github.black_Kittys22.challengekittys.Commands

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class WarnCommand(private val plugin: Main) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("warn.use")) {
            sender.sendMessage(Component.text("Keine Berechtigung!", NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Nutze: /warn <Nachricht>", NamedTextColor.RED))
            return true
        }

        val message = args.joinToString(" ")

        Bukkit.broadcast(
            Component.text("[", NamedTextColor.DARK_RED, TextDecoration.BOLD)
                .append(Component.text("WARNING", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text(message, NamedTextColor.WHITE))
        )

        return true
    }
}