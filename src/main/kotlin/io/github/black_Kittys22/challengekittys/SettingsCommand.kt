package io.github.black_Kittys22.challengekittys.Commands

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.command.*
import org.bukkit.entity.Player

class SettingsCommand(private val plugin: Main) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            // Ruft die oben hinzugefügte Methode auf
            plugin.manager.openSettingsGUI(sender)
        }
        return true
    }
}