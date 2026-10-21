package io.github.eggy03.papertrail.bot.service.handlers.guild;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@ApplicationScoped
@Slf4j
public final class GuildVoiceEventHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public GuildVoiceEventHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
        this.guildApi = guildApi;
        this.environment = environment;
        this.embedCheckingService = embedCheckingService;
    }

    private @Nullable Long getRegisteredChannelId(@NonNull Long guildId) {
        try {
            return guildApi.getGuild(guildId).memberEventChannelId();
        } catch (ApiException e) {
            if (e.getStatusCode() >= 500)
                log.warn("API Error while attempting to fetch Guild", e);
            return null;
        }
    }

    public void handleVoiceUpdateEvent(@NonNull GuildVoiceUpdateEvent event) {

        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        Member member = event.getMember();
        AudioChannel left = event.getOldValue(); // can be null if user joined for first time
        AudioChannel joined = event.getNewValue(); // can be null if user left

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Voice Activity Sub-Entry");

        if (left == null && joined != null) {
            // User has joined a vc
            eb.setDescription("A Member Has Joined A Voice Channel");
            eb.setColor(environment.embedColor().successColor());
            eb.addField(
                    MarkdownUtil.underline("Connection Event Metadata"),
                    MarkdownUtil.quoteBlock("Connected Member: " + member.getAsMention() + "\nJoined Channel: " + joined.getAsMention()),
                    false)
            ;
        }

        if (left != null && joined != null) {
            // Moved from one channel to another
            eb.setDescription("A Member Has Switched Voice Channels");
            eb.setColor(environment.embedColor().warningColor());
            eb.addField(
                    MarkdownUtil.underline("Switch Event Metadata"),
                    MarkdownUtil.quoteBlock("Switched Member: " + member.getAsMention() + "\nLeft Channel: " + left.getAsMention() + "\nJoined Channel: " + joined.getAsMention()),
                    false);
        }

        if (left != null && joined == null) {
            // User disconnected voluntarily (or was disconnected by a moderator)
            eb.setDescription("A Member Has Left A Voice Channel");
            eb.setColor(environment.embedColor().destructiveColor());
            eb.addField(
                    MarkdownUtil.underline("Disconnection Event Metadata"),
                    MarkdownUtil.quoteBlock("Disconnected Member: " + member.getAsMention() + "\nLeft Channel: " + left.getAsMention()),
                    false
            );
        }

        eb.setFooter(event.getGuild().getName());
        eb.setTimestamp(Instant.now());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }
}
