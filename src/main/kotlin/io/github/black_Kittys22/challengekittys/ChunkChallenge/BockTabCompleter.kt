package io.github.black_Kittys22.challengekittys.ChunkChallenge

import org.bukkit.Material
import org.bukkit.command.*
import org.bukkit.util.StringUtil

class BlockTabCompleter : TabCompleter {
    override fun onTabComplete(s: CommandSender, c: Command, l: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val names = Material.entries.filter { it.isBlock }.map { it.name }
            return StringUtil.copyPartialMatches(args[0], names, mutableListOf())
        }
        return emptyList()
    }
}