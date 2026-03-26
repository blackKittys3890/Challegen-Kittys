package io.github.black_Kittys22.challengekittys.Commands

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class BCCommand(private val plugin: Main) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Benutzung: /BC <Zeit> (z.B. 1h, 30m, 500s)", NamedTextColor.RED))
            return true
        }

        val timeInput = args[0].lowercase()
        val seconds = parseTimeToSeconds(timeInput)

        if (seconds == null || seconds <= 0) {
            sender.sendMessage(Component.text("Ungültiges Zeitformat! Nutze h, m oder s.", NamedTextColor.RED))
            return true
        }

        // Timer einstellen und starten
        plugin.timer.timeSeconds = seconds
        plugin.timer.paused = false

        // Bedrock Challenge starten
        plugin.bedrockChallenge.start()

        Bukkit.broadcast(Component.text("Bedrock Challenge gestartet! Zeit: $timeInput", NamedTextColor.GOLD))
        return true
    }

    private fun parseTimeToSeconds(input: String): Int? {
        val unit = input.last()
        val value = input.dropLast(1).toIntOrNull() ?: return null

        return when (unit) {
            'h' -> value * 3600
            'm' -> value * 60
            's' -> value
            else -> input.toIntOrNull() // Falls nur eine Zahl ohne Einheit kommt
        }
    }
}