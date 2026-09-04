package io.github.black_Kittys22.challengekittys.Battles.MobForceBattle

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * /mobforce <subcommand> [args]
 *
 *  start [zeit]       – Startet die Challenge, optional mit Zeit (z.B. 1h, 30m, 3600)
 *  stop               – Stoppt die Challenge
 *  reset              – Komplett zurücksetzen
 *  status             – Aktuellen Stand anzeigen
 *  ranking            – Ranglisten-GUI öffnen
 *  joker [teamId]     – Joker einsetzen
 *  team create <id> <name> [farbe]
 *  team delete <id>
 *  team add <id> <spieler>
 *  team remove <spieler>
 *  team list
 */
class MobForceBattleCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

    private val PREFIX = Component.text("[MobForceBattle] ", NamedTextColor.GOLD, TextDecoration.BOLD)

    private val COLORS = mapOf(
        "red" to NamedTextColor.RED, "blue" to NamedTextColor.BLUE,
        "green" to NamedTextColor.GREEN, "yellow" to NamedTextColor.YELLOW,
        "aqua" to NamedTextColor.AQUA, "white" to NamedTextColor.WHITE,
        "gold" to NamedTextColor.GOLD, "purple" to NamedTextColor.LIGHT_PURPLE,
        "dark_red" to NamedTextColor.DARK_RED, "dark_green" to NamedTextColor.DARK_GREEN,
        "dark_aqua" to NamedTextColor.DARK_AQUA, "dark_blue" to NamedTextColor.DARK_BLUE
    )

    // Beispiele für Tab-Completion der Zeit
    private val TIME_EXAMPLES = listOf("1h", "2h", "30m", "45m", "90m", "3600", "7200")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("challenge.mobforce")) {
            sender.sendMessage(PREFIX.append(Component.text("Keine Berechtigung!", NamedTextColor.RED)))
            return true
        }
        if (args.isEmpty()) { sendHelp(sender); return true }

        val manager = plugin.mobForceBattleManager

        when (args[0].lowercase()) {

            // ── start [zeit] ──────────────────────────────────────────────────
            "start" -> {
                if (manager.isActive) {
                    sender.sendMessage(PREFIX.append(Component.text("Challenge läuft bereits!", NamedTextColor.RED)))
                    return true
                }
                val timeSecs = if (args.size >= 2) parseTime(args[1]) else 0
                if (args.size >= 2 && timeSecs <= 0) {
                    sender.sendMessage(PREFIX.append(
                        Component.text("Ungültige Zeit! Beispiele: ", NamedTextColor.RED)
                            .append(Component.text("1h  2h30m  45m  3600", NamedTextColor.YELLOW))
                    ))
                    return true
                }
                manager.start(timeSecs)
                if (timeSecs > 0) {
                    sender.sendMessage(PREFIX.append(
                        Component.text("Challenge gestartet mit ", NamedTextColor.GREEN)
                            .append(Component.text(manager.formatTime(timeSecs), NamedTextColor.YELLOW, TextDecoration.BOLD))
                            .append(Component.text(" Zeit!", NamedTextColor.GREEN))
                    ))
                } else {
                    sender.sendMessage(PREFIX.append(Component.text("Challenge gestartet (kein Zeitlimit).", NamedTextColor.GREEN)))
                }
            }

            // ── stop ──────────────────────────────────────────────────────────
            "stop" -> {
                if (!manager.isActive) {
                    sender.sendMessage(PREFIX.append(Component.text("Challenge ist nicht aktiv.", NamedTextColor.RED)))
                    return true
                }
                manager.stop()
                sender.sendMessage(PREFIX.append(Component.text("Challenge gestoppt.", NamedTextColor.YELLOW)))
            }

            // ── reset ─────────────────────────────────────────────────────────
            "reset" -> {
                manager.reset()
                sender.sendMessage(PREFIX.append(Component.text("Alles zurückgesetzt.", NamedTextColor.YELLOW)))
            }

            // ── status ────────────────────────────────────────────────────────
            "status" -> sender.sendMessage(manager.buildStatusComponent())

            // ── ranking ───────────────────────────────────────────────────────
            "ranking" -> {
                if (sender !is Player) {
                    sender.sendMessage(PREFIX.append(Component.text("Nur für Spieler!", NamedTextColor.RED)))
                    return true
                }
                MobForceRankingGUI.open(sender)
            }

            // ── reveal ────────────────────────────────────────────────────────
            "reveal" -> {
                MobForceRankingGUI.revealNext(plugin)
            }

            // ── joker ─────────────────────────────────────────────────────────
            "joker" -> {
                val team = if (args.size >= 2) {
                    manager.teams[args[1].lowercase()]
                        ?: run {
                            sender.sendMessage(PREFIX.append(Component.text("Team '${args[1]}' nicht gefunden!", NamedTextColor.RED)))
                            return true
                        }
                } else {
                    if (sender !is Player) {
                        sender.sendMessage(PREFIX.append(Component.text("Bitte Team-ID angeben!", NamedTextColor.RED)))
                        return true
                    }
                    manager.getTeamOf(sender.uniqueId)
                        ?: run {
                            sender.sendMessage(PREFIX.append(Component.text("Du bist in keinem Team!", NamedTextColor.RED)))
                            return true
                        }
                }
                if (!manager.useJoker(team)) {
                    sender.sendMessage(PREFIX.append(Component.text("Keine Joker mehr oder Challenge beendet!", NamedTextColor.RED)))
                }
            }

            // ── team ──────────────────────────────────────────────────────────
            "team" -> handleTeam(sender, args, manager)

            else -> sendHelp(sender)
        }
        return true
    }

    // ─── Zeit parsen: "1h", "30m", "1h30m", "3600" ───────────────────────────
    private fun parseTime(input: String): Int {
        // Reine Zahl = Sekunden
        input.toIntOrNull()?.let { return it }

        var total = 0
        val hourMatch = Regex("(\\d+)h").find(input)
        val minMatch  = Regex("(\\d+)m").find(input)
        hourMatch?.groupValues?.get(1)?.toIntOrNull()?.let { total += it * 3600 }
        minMatch?.groupValues?.get(1)?.toIntOrNull()?.let { total += it * 60 }
        return total
    }

    private fun handleTeam(sender: CommandSender, args: Array<out String>, manager: MobForceBattleManager) {
        if (args.size < 2) { sendTeamHelp(sender); return }
        when (args[1].lowercase()) {

            "create" -> {
                if (args.size < 4) {
                    sender.sendMessage(PREFIX.append(Component.text("Nutze: /mobforce team create <id> <name> [farbe]", NamedTextColor.RED)))
                    return
                }
                val color = if (args.size >= 5) COLORS[args[4].lowercase()] ?: NamedTextColor.WHITE else NamedTextColor.WHITE
                if (manager.createTeam(args[2], args[3], color)) {
                    sender.sendMessage(PREFIX.append(
                        Component.text("Team ", NamedTextColor.GREEN)
                            .append(Component.text(args[3], color, TextDecoration.BOLD))
                            .append(Component.text(" erstellt.", NamedTextColor.GREEN))
                    ))
                } else {
                    sender.sendMessage(PREFIX.append(Component.text("Team '${args[2]}' existiert bereits!", NamedTextColor.RED)))
                }
            }

            "delete" -> {
                if (args.size < 3) { sender.sendMessage(PREFIX.append(Component.text("Nutze: /mobforce team delete <id>", NamedTextColor.RED))); return }
                if (manager.deleteTeam(args[2]))
                    sender.sendMessage(PREFIX.append(Component.text("Team '${args[2]}' gelöscht.", NamedTextColor.YELLOW)))
                else
                    sender.sendMessage(PREFIX.append(Component.text("Team '${args[2]}' nicht gefunden!", NamedTextColor.RED)))
            }

            "add" -> {
                if (args.size < 4) { sender.sendMessage(PREFIX.append(Component.text("Nutze: /mobforce team add <id> <spieler>", NamedTextColor.RED))); return }
                val target = plugin.server.getPlayerExact(args[3])
                    ?: run { sender.sendMessage(PREFIX.append(Component.text("Spieler nicht gefunden!", NamedTextColor.RED))); return }
                if (manager.addPlayerToTeam(target.uniqueId, args[2])) {
                    val team = manager.teams[args[2].lowercase()]!!
                    sender.sendMessage(PREFIX.append(
                        Component.text("${target.name} → Team ", NamedTextColor.GREEN)
                            .append(Component.text(team.displayName, team.color, TextDecoration.BOLD))
                    ))
                    target.sendMessage(PREFIX.append(
                        Component.text("Du bist jetzt in Team ", NamedTextColor.AQUA)
                            .append(Component.text(team.displayName, team.color, TextDecoration.BOLD))
                    ))
                } else {
                    sender.sendMessage(PREFIX.append(Component.text("Team '${args[2]}' nicht gefunden!", NamedTextColor.RED)))
                }
            }

            "remove" -> {
                if (args.size < 3) { sender.sendMessage(PREFIX.append(Component.text("Nutze: /mobforce team remove <spieler>", NamedTextColor.RED))); return }
                val target = plugin.server.getPlayerExact(args[2])
                    ?: run { sender.sendMessage(PREFIX.append(Component.text("Spieler nicht gefunden!", NamedTextColor.RED))); return }
                manager.removePlayerFromAllTeams(target.uniqueId)
                sender.sendMessage(PREFIX.append(Component.text("${target.name} aus allen Teams entfernt.", NamedTextColor.YELLOW)))
                target.sendMessage(PREFIX.append(Component.text("Du wurdest aus deinem Team entfernt.", NamedTextColor.YELLOW)))
            }

            "list" -> {
                if (manager.teams.isEmpty()) {
                    sender.sendMessage(PREFIX.append(Component.text("Keine Teams vorhanden.", NamedTextColor.GRAY)))
                    return
                }
                sender.sendMessage(Component.text("══ Teams ══", NamedTextColor.GOLD, TextDecoration.BOLD))
                manager.teams.values.forEach { team ->
                    val members = team.members.mapNotNull { plugin.server.getOfflinePlayer(it).name }
                        .joinToString(", ").ifEmpty { "keine Mitglieder" }
                    sender.sendMessage(
                        Component.text("  [", NamedTextColor.DARK_GRAY)
                            .append(Component.text(team.displayName, team.color, TextDecoration.BOLD))
                            .append(Component.text("] ${team.id}  →  ", NamedTextColor.GRAY))
                            .append(Component.text(members, NamedTextColor.WHITE))
                    )
                }
            }

            else -> sendTeamHelp(sender)
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("══ /mobforce ══", NamedTextColor.GOLD, TextDecoration.BOLD))
        listOf(
            "/mobforce start [zeit]" to "Starten (z.B. 1h, 30m, 1h30m)",
            "/mobforce stop" to "Challenge stoppen",
            "/mobforce reset" to "Alles zurücksetzen",
            "/mobforce status" to "Aktuellen Stand anzeigen",
            "/mobforce ranking" to "Ranglisten-GUI öffnen",
            "/mobforce reveal" to "Nächsten Platz enthüllen (von hinten)",
            "/mobforce joker [teamId]" to "Joker einsetzen",
            "/mobforce team ..." to "Teams verwalten"
        ).forEach { (cmd, desc) ->
            sender.sendMessage(
                Component.text("  $cmd", NamedTextColor.YELLOW)
                    .append(Component.text(" – $desc", NamedTextColor.GRAY))
            )
        }
    }

    private fun sendTeamHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("══ /mobforce team ══", NamedTextColor.GOLD, TextDecoration.BOLD))
        listOf(
            "create <id> <name> [farbe]" to "Team erstellen",
            "delete <id>" to "Team löschen",
            "add <id> <spieler>" to "Spieler hinzufügen",
            "remove <spieler>" to "Spieler entfernen",
            "list" to "Alle Teams anzeigen"
        ).forEach { (cmd, desc) ->
            sender.sendMessage(
                Component.text("  /mobforce team $cmd", NamedTextColor.YELLOW)
                    .append(Component.text(" – $desc", NamedTextColor.GRAY))
            )
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        val manager = plugin.mobForceBattleManager
        return when {
            args.size == 1 -> listOf("start", "stop", "reset", "status", "ranking", "reveal", "joker", "team")
                .filter { it.startsWith(args[0], true) }
            args.size == 2 && args[0].equals("start", true) ->
                TIME_EXAMPLES.filter { it.startsWith(args[1], true) }
            args.size == 2 && args[0].equals("team", true) ->
                listOf("create", "delete", "add", "remove", "list").filter { it.startsWith(args[1], true) }
            args.size == 2 && args[0].equals("joker", true) ->
                manager.teams.keys.filter { it.startsWith(args[1], true) }
            args.size == 3 && args[0].equals("team", true) && args[1].lowercase() in listOf("delete", "add") ->
                manager.teams.keys.filter { it.startsWith(args[2], true) }
            args.size == 4 && args[0].equals("team", true) && args[1].equals("add", true) ->
                plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[3], true) }
            args.size == 3 && args[0].equals("team", true) && args[1].equals("remove", true) ->
                plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[2], true) }
            args.size == 5 && args[0].equals("team", true) && args[1].equals("create", true) ->
                listOf("red","blue","green","yellow","aqua","white","gold","purple","dark_red","dark_green")
                    .filter { it.startsWith(args[4], true) }
            else -> emptyList()
        }
    }
}