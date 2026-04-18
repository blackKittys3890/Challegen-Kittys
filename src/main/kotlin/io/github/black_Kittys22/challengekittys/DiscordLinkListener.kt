package io.github.black_Kittys22.challengekittys

import io.github.black_Kittys22.challengekittys.Commands.LinkCommand
import io.github.black_Kittys22.challengekittys.RelayChallenge.DiscordVoiceManager
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class DiscordLinkListener(
    private val linkCommand: LinkCommand,
    private val voiceManager: DiscordVoiceManager
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "link") return

        val code = event.getOption("code")?.asString ?: run {
            event.reply("❌ Kein Code angegeben.").setEphemeral(true).queue()
            return
        }

        val uuid = linkCommand.consumeCode(code) ?: run {
            event.reply("❌ Ungültiger oder abgelaufener Code.").setEphemeral(true).queue()
            return
        }

        val discordId = event.user.id
        voiceManager.linkPlayer(uuid, discordId)
        event.reply("✅ Erfolgreich verknüpft!").setEphemeral(true).queue()
    }
}