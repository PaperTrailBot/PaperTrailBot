package io.github.eggy03.papertrail.bot.service.handlers.guild.auditlog;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;
import io.github.eggy03.papertrail.bot.utils.auditlog.ScheduledEventUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
@Slf4j
@SuppressWarnings("java:S1192")
public final class ScheduledEventActionTypeHandler extends AbstractGuildAuditLogEntryCreateEventActionTypeHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public ScheduledEventActionTypeHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
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


    @Override
    public void onScheduledEventCreate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Scheduled Event Create Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Scheduled Event Created By: " + mentionableExecutor));
        eb.setColor(environment.embedColor().successColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {
                case "entity_type" ->
                        eb.addField(MarkdownUtil.underline("Event Type"), "╰┈➤" + ScheduledEventUtils.resolveEventType(newValue), false);
                case "name" -> eb.addField(MarkdownUtil.underline("Event Name"), "╰┈➤" + newValue, false);
                case "description" -> eb.addField(MarkdownUtil.underline("Event Description"), "╰┈➤" + newValue, false);
                case "status" ->
                        eb.addField(MarkdownUtil.underline("Event Status"), "╰┈➤" + ScheduledEventUtils.resolveStatusType(newValue), false);
                case "location" -> eb.addField(MarkdownUtil.underline("Event Location"), "╰┈➤" + newValue, false);
                case "scheduled_start_time" ->
                        eb.addField(MarkdownUtil.underline("Event Start Time"), "╰┈➤" + ScheduledEventUtils.convertISOTimeToDiscordTimeStamp(newValue), false);
                case "scheduled_end_time" ->
                        eb.addField("Event End Time", "╰┈➤" + ScheduledEventUtils.convertISOTimeToDiscordTimeStamp(newValue), false);
                case "privacy_level", "image_hash", "channel_id", "recurrence_rule" -> {
                    // skip
                }
                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Scheduled Event Create: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());


        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onScheduledEventUpdate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Scheduled Event Update Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Scheduled Event Updated By: " + mentionableExecutor));
        eb.setColor(environment.embedColor().warningColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {
                case "entity_type" -> {
                    eb.addField(MarkdownUtil.underline("Old Event Type"), "╰┈➤" + ScheduledEventUtils.resolveEventType(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Event Type"), "╰┈➤" + ScheduledEventUtils.resolveEventType(newValue), true);
                    eb.addBlankField(true);
                }
                case "name" -> {
                    eb.addField(MarkdownUtil.underline("Old Event Name"), "╰┈➤" + oldValue, true);
                    eb.addField(MarkdownUtil.underline("New Event Name"), "╰┈➤" + newValue, true);
                    eb.addBlankField(true);
                }
                case "description" -> {
                    eb.addField(MarkdownUtil.underline("Old Event Description"), "╰┈➤" + oldValue, true);
                    eb.addField(MarkdownUtil.underline("New Event Description"), "╰┈➤" + newValue, true);
                    eb.addBlankField(true);
                }
                case "status" -> {
                    eb.addField(MarkdownUtil.underline("Old Event Status"), "╰┈➤" + ScheduledEventUtils.resolveStatusType(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Event Status"), "╰┈➤" + ScheduledEventUtils.resolveStatusType(newValue), true);
                    eb.addBlankField(true);
                }
                case "location" -> {
                    eb.addField(MarkdownUtil.underline("Old Event Location"), "╰┈➤" + oldValue, true);
                    eb.addField(MarkdownUtil.underline("New Event Location"), "╰┈➤" + newValue, true);
                    eb.addBlankField(true);
                }

                case "scheduled_start_time" -> {
                    eb.addField(MarkdownUtil.underline("Old Event Start Time"), "╰┈➤" + ScheduledEventUtils.convertISOTimeToDiscordTimeStamp(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Event Start Time"), "╰┈➤" + ScheduledEventUtils.convertISOTimeToDiscordTimeStamp(newValue), true);
                    eb.addBlankField(true);
                }
                case "scheduled_end_time" -> {
                    eb.addField("Old Event End Time", "╰┈➤" + ScheduledEventUtils.convertISOTimeToDiscordTimeStamp(oldValue), true);
                    eb.addField("New Event End Time", "╰┈➤" + ScheduledEventUtils.convertISOTimeToDiscordTimeStamp(newValue), true);
                    eb.addBlankField(true);
                }

                case "privacy_level" ->
                        eb.addField(MarkdownUtil.underline("Privacy"), "╰┈➤Event privacy has been updated", false);
                case "image_hash" ->
                        eb.addField(MarkdownUtil.underline("Image"), "╰┈➤Event Image has been updated", false);
                case "channel_id" ->
                        eb.addField(MarkdownUtil.underline("Channel"), "╰┈➤Event Channel has been updated", false);
                case "recurrence_rule" ->
                        eb.addField(MarkdownUtil.underline("Recurrence Rule"), "╰┈➤Recurrence Rule has been updated", false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Scheduled Event Update: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());


        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onScheduledEventDelete(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Scheduled Event Delete Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Scheduled Event Deleted By: " + mentionableExecutor));
        eb.setColor(environment.embedColor().destructiveColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {
                case "entity_type" ->
                        eb.addField(MarkdownUtil.underline("Event Type"), "╰┈➤" + ScheduledEventUtils.resolveEventType(oldValue), false);
                case "name" -> eb.addField(MarkdownUtil.underline("Event Name"), "╰┈➤" + oldValue, false);
                case "description" -> eb.addField(MarkdownUtil.underline("Event Description"), "╰┈➤" + oldValue, false);
                case "status" ->
                        eb.addField(MarkdownUtil.underline("Event Status"), "╰┈➤" + ScheduledEventUtils.resolveStatusType(oldValue), false);
                case "location" -> eb.addField(MarkdownUtil.underline("Event Location"), "╰┈➤" + oldValue, false);
                case "scheduled_start_time" ->
                        eb.addField(MarkdownUtil.underline("Event Start Time"), "╰┈➤" + ScheduledEventUtils.convertISOTimeToDiscordTimeStamp(oldValue), false);
                case "scheduled_end_time" ->
                        eb.addField("Event End Time", "╰┈➤" + ScheduledEventUtils.convertISOTimeToDiscordTimeStamp(oldValue), false);
                case "privacy_level", "image_hash", "channel_id", "recurrence_rule" -> {
                    // skip
                }
                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Scheduled Event Delete: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }
}
