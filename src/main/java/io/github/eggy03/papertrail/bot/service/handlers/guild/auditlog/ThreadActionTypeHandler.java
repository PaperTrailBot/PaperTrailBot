package io.github.eggy03.papertrail.bot.service.handlers.guild.auditlog;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;
import io.github.eggy03.papertrail.bot.utils.BooleanUtils;
import io.github.eggy03.papertrail.bot.utils.DurationUtils;
import io.github.eggy03.papertrail.bot.utils.auditlog.ChannelUtils;
import io.github.eggy03.papertrail.bot.utils.auditlog.ThreadUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
@Slf4j
@SuppressWarnings("java:S1192")
public final class ThreadActionTypeHandler extends AbstractGuildAuditLogEntryCreateEventActionTypeHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public ThreadActionTypeHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
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
    public void onThreadCreate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        ThreadChannel targetThread = event.getGuild().getThreadChannelById(ale.getTargetId());
        String mentionableTargetThread = (targetThread != null ? targetThread.getAsMention() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Thread Create Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Thread Created By: " + mentionableExecutor + "\nTarget Thread: " + mentionableTargetThread));
        eb.setColor(environment.embedColor().successColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "locked" ->
                        eb.addField(MarkdownUtil.underline("Is Locked"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), false);

                case "auto_archive_duration" ->
                        eb.addField(MarkdownUtil.underline("Auto Archive Duration"), "╰┈➤" + ThreadUtils.resolveAutoArchiveDuration(newValue), false);

                case "rate_limit_per_user" ->
                        eb.addField(MarkdownUtil.underline("Slow Mode Limit"), "╰┈➤" + DurationUtils.formatSeconds(newValue), false);

                case "type" ->
                        eb.addField(MarkdownUtil.underline("Thread Type"), "╰┈➤" + ChannelUtils.resolveChannelType(newValue), false);

                case "archived" ->
                        eb.addField(MarkdownUtil.underline("Archived"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), false);

                case "flags", "applied_tags" -> {
                    // skip
                }
                case "invitable" ->
                        eb.addField(MarkdownUtil.underline("Invitable"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), false);

                case "name" -> eb.addField(MarkdownUtil.underline("Thread Name"), "╰┈➤" + newValue, false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Thread Create: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onThreadUpdate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        ThreadChannel targetThread = event.getGuild().getThreadChannelById(ale.getTargetId());
        String mentionableTargetThread = (targetThread != null ? targetThread.getAsMention() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Thread Update Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Thread Updated By: " + mentionableExecutor + "\nTarget Thread: " + mentionableTargetThread));
        eb.setColor(environment.embedColor().warningColor());


        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "locked" -> {
                    eb.addField(MarkdownUtil.underline("Was Locked"), "╰┈➤" + BooleanUtils.formatToYesOrNo(oldValue), true);
                    eb.addField(MarkdownUtil.underline("Is Locked"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), true);
                    eb.addBlankField(true);
                }
                case "auto_archive_duration" -> {
                    eb.addField(MarkdownUtil.underline("Old Auto Archive Duration"), "╰┈➤" + ThreadUtils.resolveAutoArchiveDuration(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Auto Archive Duration"), "╰┈➤" + ThreadUtils.resolveAutoArchiveDuration(newValue), true);
                    eb.addBlankField(true);
                }
                case "rate_limit_per_user" -> {
                    eb.addField(MarkdownUtil.underline("Old Slow Mode Limit"), "╰┈➤" + DurationUtils.formatSeconds(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Slow Mode Limit"), "╰┈➤" + DurationUtils.formatSeconds(newValue), true);
                    eb.addBlankField(true);
                }
                case "type" -> {
                    eb.addField(MarkdownUtil.underline("Old Thread Type"), "╰┈➤" + ChannelUtils.resolveChannelType(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Thread Type"), "╰┈➤" + ChannelUtils.resolveChannelType(newValue), true);
                    eb.addBlankField(true);
                }
                case "archived" -> {
                    eb.addField(MarkdownUtil.underline("Old Archive Status"), "╰┈➤" + BooleanUtils.formatToEnabledOrDisabled(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Archive Status"), "╰┈➤" + BooleanUtils.formatToEnabledOrDisabled(newValue), true);
                    eb.addBlankField(true);
                }

                case "flags" -> eb.addField(MarkdownUtil.underline("Flags"), "╰┈➤Thread flags were updated", false);
                case "applied_tags" ->
                        eb.addField(MarkdownUtil.underline("Applied Tags"), "╰┈➤Applied Tags were updated", false);

                case "invitable" -> {
                    eb.addField(MarkdownUtil.underline("Was Invitable"), "╰┈➤" + BooleanUtils.formatToYesOrNo(oldValue), true);
                    eb.addField(MarkdownUtil.underline("Is Invitable"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), true);
                    eb.addBlankField(true);
                }
                case "name" -> {
                    eb.addField(MarkdownUtil.underline("Old Thread Name"), "╰┈➤" + oldValue, true);
                    eb.addField(MarkdownUtil.underline("New Thread Name"), "╰┈➤" + newValue, true);
                    eb.addBlankField(true);
                }
                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Thread Update: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onThreadDelete(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Thread Delete Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Thread Deleted By: " + mentionableExecutor + "\nTarget Thread ID: " + ale.getTargetId()));
        eb.setColor(environment.embedColor().destructiveColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "locked" ->
                        eb.addField(MarkdownUtil.underline("Was Locked"), "╰┈➤" + BooleanUtils.formatToYesOrNo(oldValue), false);

                case "auto_archive_duration" ->
                        eb.addField(MarkdownUtil.underline("Auto Archive Duration"), "╰┈➤" + ThreadUtils.resolveAutoArchiveDuration(oldValue), false);

                case "rate_limit_per_user" ->
                        eb.addField(MarkdownUtil.underline("Slow Mode Limit"), "╰┈➤" + DurationUtils.formatSeconds(oldValue), false);

                case "type" ->
                        eb.addField(MarkdownUtil.underline("Thread Type"), "╰┈➤" + ChannelUtils.resolveChannelType(oldValue), false);

                case "archived" ->
                        eb.addField(MarkdownUtil.underline("Archived"), "╰┈➤" + BooleanUtils.formatToYesOrNo(oldValue), false);

                case "flags", "applied_tags" -> {
                    // skip
                }

                case "invitable" ->
                        eb.addField(MarkdownUtil.underline("Invitable"), "╰┈➤" + BooleanUtils.formatToYesOrNo(oldValue), false);

                case "name" -> eb.addField(MarkdownUtil.underline("Thread Name"), "╰┈➤" + oldValue, false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Thread Delete: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }
}
