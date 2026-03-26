package io.github.black_Kittys22.challengekittys.MonsterBattle

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

// WICHTIG: Hier stehen jetzt BEIDE Interfaces (CommandExecutor UND TabCompleter)
class MonsterBattleCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true

        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Nutze: /monsterbattle <start/stop/stats>", NamedTextColor.RED))
            return true
        }

        when (args[0].lowercase()) {
            "start" -> {
                val minutes = args.getOrNull(1)?.toIntOrNull() ?: 30
                plugin.monsterBattleChallenge.startChallenge(minutes)
                sender.sendMessage(Component.text("Monster Battle gestartet!", NamedTextColor.GREEN))
            }
            "stop" -> {
                plugin.monsterBattleChallenge.stopChallenge()
                sender.sendMessage(Component.text("Monster Battle gestoppt.", NamedTextColor.RED))
            }
            "stats" -> {
                val count = plugin.monsterBattleChallenge.getKilledMonsterCount(sender.uniqueId)
                sender.sendMessage(Component.text("Du hast aktuell $count Wesen gesammelt.", NamedTextColor.YELLOW))
            }
        }
        return true
    }

    // Diese Funktion behebt den "Type mismatch"-Fehler in der Main.kt
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String>? {
        if (args.size == 1) {
            return mutableListOf("start", "stop", "stats").filter { it.startsWith(args[0].lowercase()) }.toMutableList()
        }
        return mutableListOf()
    }
}