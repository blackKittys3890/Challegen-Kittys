package io.github.black_Kittys22.challengekittys.Timer

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TimerCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("timer.use")) return true
        if (args.isEmpty()) return false

        when (args[0].lowercase()) {
            "resume" -> {
                plugin.timer.paused = false
                sender.sendMessage(Component.text("Timer gestartet.", NamedTextColor.GREEN))
            }
            "pause" -> {
                plugin.timer.paused = true
                sender.sendMessage(Component.text("Timer pausiert.", NamedTextColor.YELLOW))
            }
            "reset" -> {
                plugin.timer.paused = true
                plugin.timer.timeSeconds = 0
                sender.sendMessage(Component.text("Timer zurückgesetzt.", NamedTextColor.RED))
            }
            "set" -> {
                if (args.size < 2) {
                    sender.sendMessage(Component.text("Nutze: /timer set <Sekunden>", NamedTextColor.RED))
                    return true
                }
                val newTime = args[1].toIntOrNull()
                if (newTime == null) {
                    sender.sendMessage(Component.text("Bitte gib eine gültige Zahl an!", NamedTextColor.RED))
                    return true
                }
                plugin.timer.timeSeconds = newTime
                sender.sendMessage(Component.text("Zeit auf $newTime Sekunden gesetzt.", NamedTextColor.GREEN))
            }
            "autostart" -> {
                if (args.size < 2) {
                    sender.sendMessage(Component.text("Nutze: /timer autostart <on|off>", NamedTextColor.RED))
                    return true
                }

                val state = args[1].lowercase() == "on" || args[1].lowercase() == "true"
                plugin.isTimerAutoStartEnabled = state

                val statusText = if (state) "aktiviert" else "deaktiviert"
                val color = if (state) NamedTextColor.GREEN else NamedTextColor.RED
                sender.sendMessage(Component.text("Automatischer Timer-Start wurde $statusText.", color))
            }
            "color" -> {
                if (sender !is Player) {
                    sender.sendMessage(Component.text("Dieser Befehl kann nur von Spielern genutzt werden!", NamedTextColor.RED))
                    return true
                }
                plugin.timerColorGUI.openGUI(sender)
            }
            else -> sender.sendMessage(Component.text("Unbekannter Befehl!", NamedTextColor.RED))
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return listOf("resume", "pause", "reset", "set", "autostart", "color").filter { it.startsWith(args[0], true) }
        }
        if (args.size == 2 && args[0].lowercase() == "autostart") {
            return listOf("on", "off", "true", "false").filter { it.startsWith(args[1], true) }
        }
        return emptyList()
    }
}