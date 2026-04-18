package io.github.black_Kittys22.challengekittys.Commands

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class LinkCommand(private val plugin: Main) : CommandExecutor {

    // Temporäre Codes: <Code> → <UUID>
    private val pendingCodes = mutableMapOf<String, UUID>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Nur für Spieler!", NamedTextColor.RED))
            return true
        }

        // Generiere einen zufälligen Code (z.B. "ABC-123")
        val code = generateLinkCode()
        pendingCodes[code] = sender.uniqueId

        sender.sendMessage(
            Component.text("Dein Link-Code: ", NamedTextColor.GREEN)
                .append(Component.text(code, NamedTextColor.YELLOW))
                .append(Component.text("\nGib im Discord-Bot: /link $code ein.", NamedTextColor.GRAY))
        )

        // Code nach 5 Minuten löschen
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            pendingCodes.remove(code)
        }, 20 * 60 * 5) // 5 Minuten

        return true
    }

    private fun generateLinkCode(): String {
        val chars = ('A'..'Z').toList()
        val numbers = (0..9).toList()
        val random = Random()
        val part1 = (1..3).map { chars.random() }.joinToString("")
        val part2 = (1..3).map { numbers.random() }.joinToString("")
        return "$part1-$part2"
    }

    // Für Discord-Bot: Prüfe ob Code gültig ist
    fun consumeCode(code: String): UUID? {
        return pendingCodes.remove(code)
    }
}