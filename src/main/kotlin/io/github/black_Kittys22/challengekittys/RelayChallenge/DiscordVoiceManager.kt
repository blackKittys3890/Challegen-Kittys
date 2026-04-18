package io.github.black_Kittys22.challengekittys.RelayChallenge

import io.github.black_Kittys22.challengekittys.Main
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import java.util.UUID

class DiscordVoiceManager(private val plugin: Main) {

    private var jda: JDA? = null
    private var guild: Guild? = null
    private val linkedPlayers = mutableMapOf<UUID, String>()

    fun init() {
        val token = plugin.config.getString("discord.token")
        if (token.isNullOrBlank() || token == "BOT_TOKEN_HIER") {
            plugin.logger.warning("[Discord] Kein Token konfiguriert – deaktiviert.")
            return
        }
        try {
            jda = JDABuilder.createLight(token).build().awaitReady()
            guild = jda!!.getGuildById(plugin.config.getString("discord.guild-id") ?: "") ?: run {
                plugin.logger.warning("[Discord] Guild nicht gefunden!")
                return
            }
            loadLinkedPlayers()
            plugin.logger.info("[Discord] Bereit. ${linkedPlayers.size} Spieler verknüpft.")
        } catch (ex: Exception) {
            plugin.logger.severe("[Discord] Fehler: ${ex.message}")
        }
    }

    fun shutdown() {
        jda?.shutdown()
        jda = null
    }

    fun linkPlayer(uuid: UUID, discordId: String) {
        linkedPlayers[uuid] = discordId
        plugin.config.set("discord.linked-players.$uuid", discordId)
        plugin.saveConfig()
    }

    fun onRotation(activeUUID: UUID, allUUIDs: List<UUID>) {
        val g = guild ?: return
        for (uuid in allUUIDs) {
            val member = getMember(uuid) ?: continue
            val isActive = uuid == activeUUID
            g.deafen(member, isActive).queue(
                { plugin.logger.info("[Discord] ${member.effectiveName} deafen=$isActive") },
                { err -> plugin.logger.warning("[Discord] Fehler bei ${member.effectiveName}: ${err.message}") }
            )
        }
    }

    fun resetAll(allUUIDs: List<UUID>) {
        val g = guild ?: return
        for (uuid in allUUIDs) {
            val member = getMember(uuid) ?: continue
            g.deafen(member, false).queue()
        }
    }

    private fun getMember(uuid: UUID) =
        linkedPlayers[uuid]?.let { guild?.getMemberById(it) }

    private fun loadLinkedPlayers() {
        val section = plugin.config.getConfigurationSection("discord.linked-players") ?: return
        for (key in section.getKeys(false)) {
            try {
                linkedPlayers[UUID.fromString(key)] = section.getString(key) ?: continue
            } catch (_: IllegalArgumentException) {
                plugin.logger.warning("[Discord] Ungültige UUID: $key")
            }
        }
    }
}