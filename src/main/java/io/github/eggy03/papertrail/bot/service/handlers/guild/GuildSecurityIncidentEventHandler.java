package io.github.eggy03.papertrail.bot.service.handlers.guild;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;
import io.github.eggy03.papertrail.bot.utils.DurationUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.guild.SecurityIncidentActions;
import net.dv8tion.jda.api.entities.guild.SecurityIncidentDetections;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateSecurityIncidentActionsEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateSecurityIncidentDetectionsEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.OffsetDateTime;

@ApplicationScoped
@Slf4j
public final class GuildSecurityIncidentEventHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public GuildSecurityIncidentEventHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
        this.guildApi = guildApi;
        this.environment = environment;
        this.embedCheckingService = embedCheckingService;
    }

    @Nullable
    private Long getRegisteredChannelId(@NonNull Long guildId) {
        try {
            return guildApi.getGuild(guildId).guildEventChannelId();
        } catch (ApiException e) {
            if (e.getStatusCode() >= 500)
                log.warn("API Error while attempting to fetch Guild", e);
            return null;
        }
    }

    public void handleGuildUpdateSecurityIncidentDetections(@NonNull GuildUpdateSecurityIncidentDetectionsEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        SecurityIncidentDetections oldSID = event.getOldSecurityIncidentDetections();
        SecurityIncidentDetections newSID = event.getNewSecurityIncidentDetections();

        OffsetDateTime oldDMSpamDetectedAt = oldSID.getTimeDetectedDmSpam();
        OffsetDateTime newDMSpamDetectedAt = newSID.getTimeDetectedDmSpam();

        OffsetDateTime oldRaidDetectedAt = oldSID.getTimeDetectedRaid();
        OffsetDateTime newRaidDetectedAt = newSID.getTimeDetectedRaid();

        if (oldDMSpamDetectedAt != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Audit Log Entry | Security Incident Resolved");
            eb.setDescription(MarkdownUtil.quoteBlock("Incident Type: DM Spam\nStatus: Resolved"));
            eb.setColor(environment.embedColor().successColor());
            eb.addField(MarkdownUtil.underline("DM Spam Started At"), DurationUtils.isoToLocalTimeCounter(oldDMSpamDetectedAt), false);
            eb.setFooter(event.getGuild().getName());
            eb.setTimestamp(Instant.now());

            embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
        }

        if (newDMSpamDetectedAt != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Audit Log Entry | Security Incident Detected");
            eb.setDescription(MarkdownUtil.quoteBlock("Incident Type: DM Spam\nStatus: Detected"));
            eb.setColor(environment.embedColor().warningColor());
            eb.addField(MarkdownUtil.underline("DM Spam Detected At"), DurationUtils.isoToLocalTimeCounter(newDMSpamDetectedAt), false);
            eb.setFooter(event.getGuild().getName());
            eb.setTimestamp(Instant.now());

            embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
        }

        if (oldRaidDetectedAt != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Audit Log Entry | Security Incident Resolved");
            eb.setDescription(MarkdownUtil.quoteBlock("Incident Type: RAID\nStatus: Resolved"));
            eb.setColor(environment.embedColor().successColor());
            eb.addField(MarkdownUtil.underline("Raid Was Detected At"), DurationUtils.isoToLocalTimeCounter(oldRaidDetectedAt), false);
            eb.setFooter(event.getGuild().getName());
            eb.setTimestamp(Instant.now());

            embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
        }

        if (newRaidDetectedAt != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Audit Log Entry | Security Incident Detected");
            eb.setDescription(MarkdownUtil.quoteBlock("Incident Type: RAID\nStatus: Detected"));
            eb.setColor(environment.embedColor().warningColor());
            eb.addField(MarkdownUtil.underline("Raid Detected At"), DurationUtils.isoToLocalTimeCounter(newRaidDetectedAt), false);
            eb.setFooter(event.getGuild().getName());
            eb.setTimestamp(Instant.now());

            embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
        }
    }

    public void handleGuildUpdateSecurityIncidentActions(@NonNull GuildUpdateSecurityIncidentActionsEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        SecurityIncidentActions oldSIA = event.getOldSecurityIncidentActions();
        SecurityIncidentActions newSIA = event.getNewSecurityIncidentActions();

        OffsetDateTime oldDMDisabledUntil = oldSIA.getDirectMessagesDisabledUntil();
        OffsetDateTime newDMDisabledUntil = newSIA.getDirectMessagesDisabledUntil();

        OffsetDateTime oldInvitesPausedUntil = oldSIA.getInvitesDisabledUntil();
        OffsetDateTime newInvitesPausedUntil = newSIA.getInvitesDisabledUntil();

        if (oldDMDisabledUntil != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Audit Log Entry | Security Action Disabled");
            eb.setDescription(MarkdownUtil.quoteBlock("Action Type: DM Pause\nStatus: Disabled"));
            eb.setColor(environment.embedColor().successColor());
            eb.addField(MarkdownUtil.underline("DMs Were Paused Until"), DurationUtils.isoToLocalTimeCounter(oldDMDisabledUntil), false);
            eb.setFooter(event.getGuild().getName());
            eb.setTimestamp(Instant.now());

            embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
        }

        if (newDMDisabledUntil != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Audit Log Entry | Security Action Enabled");
            eb.setDescription(MarkdownUtil.quoteBlock("Action Type: DM Pause\nStatus: Enabled"));
            eb.setColor(environment.embedColor().warningColor());
            eb.addField(MarkdownUtil.underline("DMs Paused Until"), DurationUtils.isoToLocalTimeCounter(newDMDisabledUntil), false);
            eb.setFooter(event.getGuild().getName());
            eb.setTimestamp(Instant.now());

            embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
        }

        if (oldInvitesPausedUntil != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Audit Log Entry | Security Action Disabled");
            eb.setDescription(MarkdownUtil.quoteBlock("Action Type: Invite Pause\nStatus: Disabled"));
            eb.setColor(environment.embedColor().successColor());
            eb.addField(MarkdownUtil.underline("Invites Were Paused Until"), DurationUtils.isoToLocalTimeCounter(oldInvitesPausedUntil), false);
            eb.setFooter(event.getGuild().getName());
            eb.setTimestamp(Instant.now());

            embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
        }

        if (newInvitesPausedUntil != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Audit Log Entry | Security Action Enabled");
            eb.setDescription(MarkdownUtil.quoteBlock("Action Type: Invite Pause\nStatus: Enabled"));
            eb.setColor(environment.embedColor().warningColor());
            eb.addField(MarkdownUtil.underline("Invites Paused Until"), DurationUtils.isoToLocalTimeCounter(newInvitesPausedUntil), false);
            eb.setFooter(event.getGuild().getName());
            eb.setTimestamp(Instant.now());

            embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
        }
    }
}
