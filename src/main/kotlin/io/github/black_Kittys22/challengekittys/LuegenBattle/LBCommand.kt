package io.github.black_Kittys22.challengekittys.Commands

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class LBCommand(private val plugin: Main) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true
        if (args.isNotEmpty()) {
            when (args[0].lowercase()) {
                "round" -> {
                    if (args.size > 1 && args[1].lowercase() == "start") {
                        plugin.structureBattleManager.startRound()
                    }
                    if (args.size > 1 && args[1].lowercase() == "stop") {
                        plugin.structureBattleManager.stopAndTeleportToZero()
                    }
                }
                "reveal", "aufloesung" -> {
                    plugin.structureBattleManager.revealTruth()
                }
            }
        }
        return true
    }
}