package io.github.eggy03.papertrail.bot.service.handlers.command;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.client.model.ApiError;
import io.github.eggy03.papertrail.bot.client.model.GuildDTO;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
@Slf4j
@SuppressWarnings("java:S1192")
public final class ConfigCommandHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;

    @Inject
    public ConfigCommandHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment) {
        this.guildApi = guildApi;
        this.environment = environment;
    }

    public void saveGuild(@NonNull SlashCommandInteractionEvent event) {

        Guild callerGuild = event.getGuild();
        if (callerGuild == null) {
            log.warn("An audit log set command may have been called outside of a guild. This should not happen.");
            return;
        }

        // acknowledge this interaction before calling the API
        event.deferReply().queue();

        OptionMapping guildEventChannelOption = event.getOption("guild_event_channel");
        OptionMapping memberEventChannelOption = event.getOption("member_event_channel");
        OptionMapping messageEventChannelOption = event.getOption("message_event_channel");

        Long guildEventChannelId = guildEventChannelOption == null ? null : guildEventChannelOption.getAsChannel().getIdLong();
        Long memberEventChannelId = memberEventChannelOption == null ? null : memberEventChannelOption.getAsChannel().getIdLong();
        Long messageEventChannelId = messageEventChannelOption == null ? null : messageEventChannelOption.getAsChannel().getIdLong();

        // start building embed
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Bot Setup Process");

        // Call the API to register the guild and the channel
        try {
            guildApi.saveGuild(new GuildDTO(callerGuild.getIdLong(), guildEventChannelId, memberEventChannelId, messageEventChannelId));
            eb.setColor(environment.embedColor().successColor());
            eb.addField(MarkdownUtil.underline("Setup Success"), MarkdownUtil.codeblock("Events will be logged in your set channels"), false);
        } catch (ApiException e) {
            eb.setColor(environment.embedColor().warningColor());

            ApiError apiError = e.getError();
            if (apiError.status() == 409) {
                eb.addField(MarkdownUtil.underline("Setup Warning"), MarkdownUtil.codeblock("A configuration already exists for this guild."), false);
            } else {
                eb.addField(MarkdownUtil.underline("Setup Failure"), MarkdownUtil.codeblock(apiError.message()), false);
            }

        }

        event.getHook().editOriginalEmbeds(eb.build()).queue();

    }

    public void viewGuild(@NonNull SlashCommandInteractionEvent event) {

        Guild callerGuild = event.getGuild();
        if (callerGuild == null) {
            log.warn("An audit log view command may have been called outside of a guild. This should not happen.");
            return;
        }

        // acknowledge this interaction before calling the API
        event.deferReply().queue();

        // start building the embed
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("View Existing Configuration");

        // Call the API to retrieve the registered channel
        try {
            GuildDTO dto = guildApi.getGuild(callerGuild.getIdLong());

            Long guildEventChannelId = dto.guildEventChannelId();
            Long memberEventChannelId = dto.memberEventChannelId();
            Long messageEventChannelId = dto.messageEventChannelId();

            GuildChannel guildEventChannel = guildEventChannelId == null ? null : event.getJDA().getGuildChannelById(guildEventChannelId);
            GuildChannel memberEventChannel = memberEventChannelId == null ? null : event.getJDA().getGuildChannelById(memberEventChannelId);
            GuildChannel messageEventChannel = messageEventChannelId == null ? null : event.getJDA().getGuildChannelById(messageEventChannelId);

            String guildEventChannelJumpUrl = guildEventChannel == null ? "Not Available" : guildEventChannel.getJumpUrl();
            String memberEventChannelJumpUrl = memberEventChannel == null ? "Not Available" : memberEventChannel.getJumpUrl();
            String messageEventChannelJumpUrl = messageEventChannel == null ? "Not Available" : messageEventChannel.getJumpUrl();

            eb.setColor(environment.embedColor().successColor());
            eb.addField(MarkdownUtil.underline("Guild Event Channel"), guildEventChannelJumpUrl, false);
            eb.addField(MarkdownUtil.underline("Member Event Channel"), memberEventChannelJumpUrl, false);
            eb.addField(MarkdownUtil.underline("Message Event Channel"), messageEventChannelJumpUrl, false);
        } catch (ApiException e) {
            eb.setColor(environment.embedColor().warningColor());

            ApiError apiError = e.getError();
            if (apiError.status() == 404) {
                eb.addField(MarkdownUtil.underline("View Warning"), MarkdownUtil.codeblock("No configuration is registered for this guild."), false);
            } else {
                eb.addField(MarkdownUtil.underline("View Failure"), MarkdownUtil.codeblock(apiError.message()), false);
            }
        }

        event.getHook().editOriginalEmbeds(eb.build()).queue();
    }

    public void updateGuild(@NonNull SlashCommandInteractionEvent event) {

        Guild callerGuild = event.getGuild();
        if (callerGuild == null) {
            log.warn("An audit log unset command may have been called outside of a guild. This should not happen.");
            return;
        }

        // acknowledge this interaction before calling the API
        event.deferReply().queue();

        OptionMapping guildEventChannelOption = event.getOption("guild_event_channel");
        OptionMapping memberEventChannelOption = event.getOption("member_event_channel");
        OptionMapping messageEventChannelOption = event.getOption("message_event_channel");

        // start building the embed
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Delete Existing Configuration");

        // if all the options are null, unregister the guild
        if (guildEventChannelOption == null && memberEventChannelOption == null && messageEventChannelOption == null) {

            try {
                guildApi.deleteGuild(callerGuild.getIdLong());

                eb.setColor(environment.embedColor().destructiveColor());
                eb.addField(MarkdownUtil.underline("Deletion Success"), MarkdownUtil.codeblock("Configuration Deleted"), false);
            } catch (ApiException e) {
                eb.setColor(environment.embedColor().warningColor());

                ApiError apiError = e.getError();
                if (apiError.status() == 404) {
                    eb.addField(MarkdownUtil.underline("Deletion Warning"), MarkdownUtil.codeblock("No configuration is registered for this guild."), false);
                } else {
                    eb.addField(MarkdownUtil.underline("Deletion Failure"), MarkdownUtil.codeblock(e.getError().message()), false);
                }
            }
        } else {
            // else update guild
            Long guildEventChannelId = guildEventChannelOption == null ? null : guildEventChannelOption.getAsChannel().getIdLong();
            Long memberEventChannelId = memberEventChannelOption == null ? null : memberEventChannelOption.getAsChannel().getIdLong();
            Long messageEventChannelId = messageEventChannelOption == null ? null : messageEventChannelOption.getAsChannel().getIdLong();

            // Call the API to update channel
            try {
                guildApi.updateGuild(new GuildDTO(callerGuild.getIdLong(), guildEventChannelId, memberEventChannelId, messageEventChannelId));

                eb.setColor(environment.embedColor().successColor());
                eb.addField(MarkdownUtil.underline("Update Success"), MarkdownUtil.codeblock("Configuration Has Been Updated"), false);
            } catch (ApiException e) {
                eb.setColor(environment.embedColor().warningColor());

                ApiError apiError = e.getError();
                if (apiError.status() == 404) {
                    eb.addField(MarkdownUtil.underline("Update Warning"), MarkdownUtil.codeblock("No configuration is registered for this guild."), false);
                } else {
                    eb.addField(MarkdownUtil.underline("Update Failure"), MarkdownUtil.codeblock(e.getError().message()), false);
                }

            }
        }

        event.getHook().editOriginalEmbeds(eb.build()).queue();

    }
}
