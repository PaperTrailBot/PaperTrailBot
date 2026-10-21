package io.github.eggy03.papertrail.bot.service.handlers.guild.auditlog;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;
import io.github.eggy03.papertrail.bot.utils.BooleanUtils;
import io.github.eggy03.papertrail.bot.utils.DurationUtils;
import io.github.eggy03.papertrail.bot.utils.auditlog.ChannelUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
@Slf4j
@SuppressWarnings("java:S1192")
public final class ChannelActionTypeHandler extends AbstractGuildAuditLogEntryCreateEventActionTypeHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public ChannelActionTypeHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
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
    public void onChannelCreate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        GuildChannel targetChannel = ale.getGuild().getGuildChannelById(ale.getTargetId());
        String targetChannelMention = (targetChannel != null ? targetChannel.getAsMention() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Channel Create Event");

        eb.setDescription(MarkdownUtil.quoteBlock("Channel Created By: " + mentionableExecutor + "\nTarget Channel: " + targetChannelMention));
        eb.setColor(environment.embedColor().successColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {
                case "user_limit" ->
                        eb.addField(MarkdownUtil.underline("User Limit"), "╰┈➤" + ChannelUtils.resolveVoiceChannelUserLimit(newValue), false);

                case "rate_limit_per_user" ->
                        eb.addField(MarkdownUtil.underline("Slow Mode"), "╰┈➤" + DurationUtils.formatSeconds(newValue), false);

                case "type" ->
                        eb.addField(MarkdownUtil.underline("Channel Type"), "╰┈➤" + ChannelUtils.resolveChannelType(newValue), false);

                case "nsfw" ->
                        eb.addField(MarkdownUtil.underline("Is NSFW"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), false);

                case "name" -> eb.addField(MarkdownUtil.underline("Channel Name"), "╰┈➤" + newValue, false);

                case "bitrate" ->
                        eb.addField(MarkdownUtil.underline("Voice Channel Bitrate"), "╰┈➤" + ChannelUtils.resolveVoiceChannelBitrate(newValue), false);

                case "permission_overwrites", "flags", "template", "available_tags" -> {
                    // the first two are for all types of channels and may stay empty during creation events
                    // the second two are forum only cases which stay empty during creation
                }

                case "topic" -> eb.addField(MarkdownUtil.underline("Channel Topic"), "╰┈➤" + newValue, false);

                case "rtc_region" -> eb.addField(MarkdownUtil.underline("RTC Region"), "╰┈➤" + newValue, false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key for Channel Create: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onChannelUpdate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        GuildChannel targetChannel = event.getGuild().getGuildChannelById(ale.getTargetId());
        String targetChannelMention = (targetChannel != null ? targetChannel.getAsMention() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Channel Update Event");

        eb.setDescription(MarkdownUtil.quoteBlock("Channel Updated By: " + mentionableExecutor + "\nTarget Channel: " + targetChannelMention));
        eb.setColor(environment.embedColor().warningColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "user_limit" -> {
                    eb.addField(MarkdownUtil.underline("Old User Limit"), "╰┈➤" + ChannelUtils.resolveVoiceChannelUserLimit(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New User Limit"), "╰┈➤" + ChannelUtils.resolveVoiceChannelUserLimit(newValue), true);
                    eb.addBlankField(true);
                }

                case "rate_limit_per_user" -> {
                    eb.addField(MarkdownUtil.underline("Old Slow mode Value"), "╰┈➤" + DurationUtils.formatSeconds(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Slow mode Value"), "╰┈➤" + DurationUtils.formatSeconds(newValue), true);
                    eb.addBlankField(true);
                }

                case "default_thread_rate_limit_per_user" -> {
                    eb.addField(MarkdownUtil.underline("Old Thread Slow mode Value"), "╰┈➤" + DurationUtils.formatSeconds(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Thread Slow mode Value"), "╰┈➤" + DurationUtils.formatSeconds(newValue), true);
                    eb.addBlankField(true);
                }

                case "nsfw" -> {
                    eb.addField(MarkdownUtil.underline("Was NSFW"), "╰┈➤" + BooleanUtils.formatToYesOrNo(oldValue), true);
                    eb.addField(MarkdownUtil.underline("Is NSFW"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), true);
                    eb.addBlankField(true);
                }

                case "video_quality_mode" -> {
                    eb.addField(MarkdownUtil.underline("Old Video Quality Mode"), "╰┈➤" + ChannelUtils.resolveVoiceChannelVideoQuality(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Video Quality Mode"), "╰┈➤" + ChannelUtils.resolveVoiceChannelVideoQuality(newValue), true);
                    eb.addBlankField(true);
                }

                case "name" -> {
                    eb.addField(MarkdownUtil.underline("Old Channel Name"), "╰┈➤" + oldValue, true);
                    eb.addField(MarkdownUtil.underline("New Channel Name"), "╰┈➤" + newValue, true);
                    eb.addBlankField(true);
                }

                case "bitrate" -> {
                    eb.addField(MarkdownUtil.underline("Old Voice Channel Bitrate"), "╰┈➤" + ChannelUtils.resolveVoiceChannelBitrate(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Voice Channel Bitrate"), "╰┈➤" + ChannelUtils.resolveVoiceChannelBitrate(newValue), true);
                    eb.addBlankField(true);
                }

                case "rtc_region" -> {
                    eb.addField(MarkdownUtil.underline("Old Region"), "╰┈➤" + oldValue, true);
                    eb.addField(MarkdownUtil.underline("New Region"), "╰┈➤" + newValue, true);
                    eb.addBlankField(true);
                }

                case "topic" -> {
                    eb.addField(MarkdownUtil.underline("Old Topic"), "╰┈➤" + oldValue, true);
                    eb.addField(MarkdownUtil.underline("New topic"), "╰┈➤" + newValue, true);
                    eb.addBlankField(true);
                }

                case "default_auto_archive_duration" -> {
                    eb.addField(MarkdownUtil.underline("Old Hide After Inactivity Timer"), "╰┈➤" + DurationUtils.formatMinutes(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Hide After Inactivity Timer"), "╰┈➤" + DurationUtils.formatMinutes(newValue), true);
                    eb.addBlankField(true);
                }

                case "type" -> {
                    eb.addField(MarkdownUtil.underline("Old Type"), ChannelUtils.resolveChannelType(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Type"), ChannelUtils.resolveChannelType(newValue), true);
                    eb.addBlankField(true);
                }

                case "available_tags" ->
                        eb.addField(MarkdownUtil.underline("Tags"), "╰┈➤The channel's tags were updated", false);
                case "default_reaction_emoji" ->
                        eb.addField(MarkdownUtil.underline("Reaction Emoji"), "╰┈➤Default reaction emoji was updated", false);
                case "flags" ->
                        eb.addField(MarkdownUtil.underline("Flags"), "╰┈➤The channel's flags were updated", false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key for Channel Update: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }

            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onChannelDelete(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Channel Delete Event");

        eb.setDescription(MarkdownUtil.quoteBlock("Channel Deleted By: " + mentionableExecutor + "\nTarget Channel ID: " + ale.getTargetId()));
        eb.setColor(environment.embedColor().destructiveColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();

            switch (changeKey) {
                case "name" -> eb.addField(MarkdownUtil.underline("Name"), "╰┈➤" + oldValue, false);

                case "type" ->
                        eb.addField(MarkdownUtil.underline("Type"), "╰┈➤" + ChannelUtils.resolveChannelType(oldValue), false);

                default -> {
                    // omit all other fields
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }
}
