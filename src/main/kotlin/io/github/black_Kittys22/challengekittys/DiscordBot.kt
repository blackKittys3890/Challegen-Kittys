package io.github.black_Kittys22.challengekittys

import io.github.black_Kittys22.challengekittys.Commands.LinkCommand
import io.github.black_Kittys22.challengekittys.RelayChallenge.DiscordVoiceManager
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy

object DiscordBot {
    private var jdaThread: Thread? = null
    private var jdaInstance: net.dv8tion.jda.api.JDA? = null

    fun start(token: String, linkCommand: LinkCommand, voiceManager: DiscordVoiceManager) {
        // ...
        jdaInstance = JDABuilder.createLight(token)
            .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .addEventListeners(DiscordLinkListener(linkCommand, voiceManager)) // ← NEU
            .build()
            .awaitReady()

        // Slash-Command registrieren (nur einmal nötig, aber idempotent)
        jdaInstance!!.upsertCommand(
            net.dv8tion.jda.api.interactions.commands.build.Commands.slash("link", "Minecraft-Account verknüpfen")
                .addOption(
                    net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
                    "code", "Dein Link-Code aus Minecraft", true
                )
        ).queue()
        // ...
    }

    fun stop() {
        jdaInstance?.shutdownNow()
        jdaThread?.interrupt()
        jdaThread = null
        jdaInstance = null
        println("[DiscordBot] 🛑 Bot gestoppt.")
    }

    // Optional: Gib die JDA-Instanz für andere Klassen frei (z. B. für Voice-Channel-Steuerung)
    fun getJDA(): net.dv8tion.jda.api.JDA? = jdaInstance
}