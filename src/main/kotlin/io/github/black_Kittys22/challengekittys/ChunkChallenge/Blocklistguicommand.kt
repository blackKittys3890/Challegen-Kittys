package io.github.black_Kittys22.challengekittys.ChunkChallenge

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class BlocklistGuiCommand(private val plugin: Main) : CommandExecutor {

    companion object {
        // Alle platzierbaren Blöcke (kein AIR, kein technisches Zeug)
        val ALL_BLOCKS: List<Material> = Material.entries.filter { mat ->
            mat.isBlock &&
                    mat != Material.AIR &&
                    mat != Material.CAVE_AIR &&
                    mat != Material.VOID_AIR &&
                    !mat.name.contains("LEGACY") &&
                    runCatching { ItemStack(mat); true }.getOrDefault(false)
        }.sortedBy { it.name }

        const val PAGE_SIZE = 45 // 5 Reihen für Blöcke, 1 Reihe Navigation
        const val GUI_SIZE = 54

        fun getTitle(page: Int, totalPages: Int) =
            Component.text("Blockliste  [Seite ${page + 1}/$totalPages]", NamedTextColor.DARK_AQUA, TextDecoration.BOLD)
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player || !sender.hasPermission("challenge.blocklist")) return true
        openPage(sender, 0)
        return true
    }

    fun openPage(player: Player, page: Int) {
        val totalPages = (ALL_BLOCKS.size + PAGE_SIZE - 1) / PAGE_SIZE
        val inv = Bukkit.createInventory(null, GUI_SIZE, getTitle(page, totalPages))

        val start = page * PAGE_SIZE
        val end = minOf(start + PAGE_SIZE, ALL_BLOCKS.size)

        for (i in start until end) {
            val mat = ALL_BLOCKS[i]
            val blocked = plugin.blacklistedMaterials.contains(mat)

            val item = ItemStack(mat)
            val meta = item.itemMeta ?: continue

            val color = if (blocked) NamedTextColor.RED else NamedTextColor.GREEN
            val status = if (blocked) "§c✖ VERBOTEN" else "§a✔ ERLAUBT"

            meta.displayName(
                Component.text(mat.name, color).decoration(TextDecoration.ITALIC, false)
            )
            meta.lore(listOf(
                Component.text(status).decoration(TextDecoration.ITALIC, false),
                Component.text("Klicken zum Umschalten", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ))
            item.itemMeta = meta
            inv.setItem(i - start, item)
        }

        // ── Navigationsleiste (Reihe 6) ──────────────────────────────────────
        // Zurück-Pfeil (Slot 45)
        if (page > 0) {
            val prev = ItemStack(Material.ARROW)
            val prevMeta = prev.itemMeta!!
            prevMeta.displayName(Component.text("◀ Zurück", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            prev.itemMeta = prevMeta
            inv.setItem(45, prev)
        }

        // Seitenanzeige (Slot 49)
        val pageInfo = ItemStack(Material.PAPER)
        val pageMeta = pageInfo.itemMeta!!
        pageMeta.displayName(
            Component.text("Seite ${page + 1} / $totalPages", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
        )
        pageMeta.lore(listOf(
            Component.text("${plugin.blacklistedMaterials.size} Blöcke verboten", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
        ))
        pageInfo.itemMeta = pageMeta
        inv.setItem(49, pageInfo)

        // Weiter-Pfeil (Slot 53)
        if (page < totalPages - 1) {
            val next = ItemStack(Material.ARROW)
            val nextMeta = next.itemMeta!!
            nextMeta.displayName(Component.text("Weiter ▶", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            next.itemMeta = nextMeta
            inv.setItem(53, next)
        }

        player.openInventory(inv)
    }
}