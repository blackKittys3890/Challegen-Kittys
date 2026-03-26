package io.github.black_Kittys22.challengekittys.Timer

import io.github.black_Kittys22.challengekittys.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitRunnable
import kotlin.math.sin

enum class TimerColorTheme(
    val displayName: String,
    val color1: TextColor,
    val color2: TextColor
) {
    BLACK_GRAY(
        "Schwarz/Grau",
        TextColor.color(228, 227, 231), // Hellweiß
        TextColor.color(88, 86, 85)     // Anthrazit
    ),
    GREEN(
        "Grün",
        TextColor.color(0, 255, 128),   // Hellgrün
        TextColor.color(0, 100, 40)     // Dunkelgrün
    ),
    PURPLE(
        "Lila",
        TextColor.color(200, 100, 255), // Hellviolett
        TextColor.color(80, 0, 160)     // Dunkelviolett
    )
}

class Timer(private val plugin: Main) {
    var timeSeconds: Int = 0
    var paused: Boolean = true
    var maxTime: Int = 0
    var colorTheme: TimerColorTheme = TimerColorTheme.BLACK_GRAY
    private var countdownWarningsShown = mutableSetOf<Int>()

    // Counter, um die Sekunden vom 1-Tick-Animations-Task zu trennen
    private var tickCounter: Int = 0

    init {
        run()
    }

    private fun run() {
        object : BukkitRunnable() {
            override fun run() {
                // Logik-Teil (läuft nur alle 20 Ticks = 1 Sekunde)
                if (!paused) {
                    tickCounter++
                    if (tickCounter >= 20) {
                        tickCounter = 0
                        if (maxTime > 0 || plugin.bedrockChallenge.isActive) {
                            if (timeSeconds > 0) {
                                timeSeconds--
                                checkWarnings()
                            } else {
                                handleTimeUp()
                            }
                        } else {
                            timeSeconds++
                        }
                    }
                }

                // Anzeige-Teil (läuft JEDEN Tick für flüssige Animation)
                sendActionBar()
            }
        }.runTaskTimer(plugin, 1L, 1L)
    }

    private fun handleTimeUp() {
        timeSeconds = 0
        paused = true
        maxTime = 0
        countdownWarningsShown.clear()

        // MobForceBattle abschließen (TP + Rangliste), sonst normales Ende
        if (plugin.mobForceBattleManager.isActive) {
            plugin.mobForceBattleManager.onTimeUp()
        } else {
            broadcastTimeUp()
            if (plugin.bedrockChallenge.isActive) plugin.bedrockChallenge.isActive = false
            plugin.structureBattleManager.stopAndTeleportToZero()
            Bukkit.getOnlinePlayers().forEach { it.playSound(it.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f) }
        }
    }

    fun startCountdown(seconds: Int) {
        timeSeconds = seconds
        maxTime = seconds
        paused = false
        tickCounter = 0
        countdownWarningsShown.clear()
    }

    private fun checkWarnings() {
        val warnings = listOf(300, 180, 60, 30, 10)
        if (timeSeconds in warnings && !countdownWarningsShown.contains(timeSeconds)) {
            val message = when(timeSeconds) {
                300 -> "Noch 5 Minuten!"
                180 -> "Noch 3 Minuten!"
                60 -> "Noch 1 Minute!"
                30 -> "Noch 30 Sekunden!"
                10 -> "Noch 10 Sekunden!"
                else -> ""
            }
            Bukkit.broadcast(Component.text(message, if (timeSeconds <= 60) NamedTextColor.RED else NamedTextColor.GOLD, TextDecoration.BOLD))
            countdownWarningsShown.add(timeSeconds)
            Bukkit.getOnlinePlayers().forEach { it.playSound(it.location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f) }
        }
    }

    private fun broadcastTimeUp() {
        Bukkit.broadcast(Component.text("===================================", NamedTextColor.DARK_RED))
        Bukkit.broadcast(Component.text("ZEIT ABGELAUFEN!", NamedTextColor.DARK_RED, TextDecoration.BOLD))
        Bukkit.broadcast(Component.text("===================================", NamedTextColor.DARK_RED))
    }

    private fun sendActionBar() {
        val timeComp = getTimeComponent()
        Bukkit.getOnlinePlayers().forEach { player ->
            // MobForceBattle: Mob-Ziel rechts neben dem Timer anzeigen
            val mobDisplay = plugin.mobForceBattleManager.getMobDisplayForPlayer(player.uniqueId)
            if (mobDisplay != null) {
                val full = timeComp
                    .append(Component.text("  ⚔ ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(mobDisplay, NamedTextColor.RED, TextDecoration.BOLD))
                player.sendActionBar(full)
            } else {
                player.sendActionBar(timeComp)
            }
        }
    }

    private fun getTimeComponent(): Component {
        val timeStr = formatTime(timeSeconds)

        val phase = System.currentTimeMillis() * 0.0015

        if (paused) {
            return applyGradient("PAUSIERT ($timeStr)", colorTheme.color1, colorTheme.color2, phase)
        }

        val isLowTime = (maxTime > 0 || plugin.bedrockChallenge.isActive) && timeSeconds <= 60
        val isCriticalTime = (maxTime > 0 || plugin.bedrockChallenge.isActive) && timeSeconds <= 10

        if (plugin.bedrockChallenge.isActive) plugin.bedrockChallenge.tick()

        return when {
            isCriticalTime -> {
                // Blink-Effekt für die letzten 10 Sekunden
                if ((System.currentTimeMillis() / 250) % 2 == 0L)
                    Component.text(timeStr, NamedTextColor.RED, TextDecoration.BOLD)
                else
                    Component.text(timeStr, TextColor.color(100, 0, 0), TextDecoration.BOLD)
            }
            isLowTime -> Component.text(timeStr, NamedTextColor.GOLD, TextDecoration.BOLD)
            else -> applyGradient(timeStr, colorTheme.color1, colorTheme.color2, phase)
        }
    }

    private fun applyGradient(text: String, c1: TextColor, c2: TextColor, phase: Double): Component {
        val builder = Component.text()
        for (i in text.indices) {
            val ratio = (sin(phase + i * 0.12) + 1.0) / 2.0
            val color = interpolate(c1, c2, ratio)
            builder.append(Component.text(text[i].toString(), color, TextDecoration.BOLD))
        }
        return builder.build()
    }

    private fun interpolate(c1: TextColor, c2: TextColor, ratio: Double): TextColor {
        val r = (c1.red() + (c2.red() - c1.red()) * ratio).toInt().coerceIn(0, 255)
        val g = (c1.green() + (c2.green() - c1.green()) * ratio).toInt().coerceIn(0, 255)
        val b = (c1.blue() + (c2.blue() - c1.blue()) * ratio).toInt().coerceIn(0, 255)
        return TextColor.color(r, g, b)
    }

    // Zeigt Tage an, sobald >= 86400 Sekunden erreicht werden
    // Beispiele: "01:12:30:00" (mit Tag), "12:30:00" (mit Stunden), "04:32" (nur Minuten)
    private fun formatTime(seconds: Int): String {
        val d = seconds / 86400
        val h = (seconds % 86400) / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            d > 0 -> String.format("%02d:%02d:%02d:%02d", d, h, m, s)
            h > 0 -> String.format("%02d:%02d:%02d", h, m, s)
            else  -> String.format("%02d:%02d", m, s)
        }
    }
}