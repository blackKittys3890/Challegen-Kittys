package io.github.black_Kittys22.challengekittys.ChunkChallenge

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class BlockCommand(private val plugin: Main) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player || !sender.hasPermission("challenge.block")) return true

        val targetBlock = sender.getTargetBlockExact(5)
        if (targetBlock == null || targetBlock.type == Material.AIR) {
            sender.sendMessage(Component.text("Kein Block im Visier!", NamedTextColor.RED))
            return true
        }

        val mat = targetBlock.type
        if (plugin.blacklistedMaterials.contains(mat)) {
            plugin.blacklistedMaterials.remove(mat)
            sender.sendMessage(Component.text("${mat.name} entfernt!", NamedTextColor.GREEN))
        } else {
            plugin.blacklistedMaterials.add(mat)
            sender.sendMessage(Component.text("${mat.name} verboten!", NamedTextColor.YELLOW))
        }
        plugin.savePluginConfig()
        return true
    }
}