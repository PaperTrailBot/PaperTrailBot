package io.github.eggy03.papertrail.bot.service.handlers.guild.auditlog;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;
import io.github.eggy03.papertrail.bot.utils.BooleanUtils;
import io.github.eggy03.papertrail.bot.utils.DurationUtils;
import io.github.eggy03.papertrail.bot.utils.auditlog.GuildUtils;

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
public final class GuildUpdateActionTypeHandler extends AbstractGuildAuditLogEntryCreateEventActionTypeHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public GuildUpdateActionTypeHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
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
    public void onGuildUpdate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Guild Update Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Guild Settings Updated By: " + mentionableExecutor + "\nTarget Guild: " + event.getGuild().getName()));
        eb.setColor(environment.embedColor().warningColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {
                case "description" -> {
                    eb.addField(MarkdownUtil.underline("Old Description"), "╰┈➤" + oldValue, false);
                    eb.addField(MarkdownUtil.underline("New Description"), "╰┈➤" + newValue, false);
                }

                case "icon_hash" ->
                        eb.addField(MarkdownUtil.underline("Icon Hash Change"), "╰┈➤Guild Icon has been updated", false);

                case "name" -> {
                    eb.addField(MarkdownUtil.underline("Old Guild Name"), "╰┈➤" + oldValue, false);
                    eb.addField(MarkdownUtil.underline("New Guild Name"), "╰┈➤" + newValue, false);
                }

                case "preferred_locale" ->
                        eb.addField(MarkdownUtil.underline("Preferred Locale Set To"), "╰┈➤" + newValue, false);

                case "afk_channel_id" ->
                        eb.addField(MarkdownUtil.underline("AFK Channel Changed To"), "╰┈➤" + GuildUtils.resolveMentionableChannel(newValue, event), false);

                case "default_message_notifications" ->
                        eb.addField(MarkdownUtil.underline("Default Message Notifications Update"), "╰┈➤" + GuildUtils.resolveGuildDefaultMessageNotificationLevel(newValue), false);

                case "afk_timeout" ->
                        eb.addField(MarkdownUtil.underline("AFK Channel Timeout Change"), "╰┈➤" + DurationUtils.formatSeconds(newValue), false);

                case "system_channel_id" ->
                        eb.addField(MarkdownUtil.underline("Community Updates Channel Changed To"), "╰┈➤" + GuildUtils.resolveMentionableChannel(newValue, event), false);

                case "widget_enabled" ->
                        eb.addField(MarkdownUtil.underline("Widget Enabled"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), false);

                case "widget_channel_id" ->
                        eb.addField(MarkdownUtil.underline("Widget Channel Changed To"), "╰┈➤" + GuildUtils.resolveMentionableChannel(newValue, event), false);

                case "premium_progress_bar_enabled" ->
                        eb.addField(MarkdownUtil.underline("Server Boost Progress Bar Enabled"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), false);

                case "mfa_level" ->
                        eb.addField(MarkdownUtil.underline("MFA Requirement Set To"), "╰┈➤" + GuildUtils.resolveGuildModActionMFALevel(newValue), false);

                case "verification_level" ->
                        eb.addField(MarkdownUtil.underline("Verification Level Set To"), "╰┈➤" + GuildUtils.resolveGuildVerificationLevel(newValue), false);

                case "owner_id" -> {
                    eb.addField(MarkdownUtil.underline("Old Owner"), "╰┈➤" + GuildUtils.resolveOwnerName(oldValue, event), false);
                    eb.addField(MarkdownUtil.underline("New Owner"), "╰┈➤" + GuildUtils.resolveOwnerName(newValue, event), false);
                }

                case "public_updates_channel_id" ->
                        eb.addField(MarkdownUtil.underline("Announcements Channel Changed To"), "╰┈➤" + GuildUtils.resolveMentionableChannel(newValue, event), false);

                case "rules_channel_id" ->
                        eb.addField(MarkdownUtil.underline("Rules Channel Changed To"), "╰┈➤" + GuildUtils.resolveMentionableChannel(newValue, event), false);

                case "system_channel_flags" ->
                        eb.addField(MarkdownUtil.underline("System Channel Flags"), "╰┈➤" + GuildUtils.resolveSystemChannelFlags(newValue), false);

                case "explicit_content_filter" ->
                        eb.addField(MarkdownUtil.underline("Explicit Content Filter"), "╰┈➤" + GuildUtils.resolveExplicitContentFilterLevel(newValue), false);

                case "theme" ->
                        eb.addField(MarkdownUtil.underline("Server Theme"), "╰┈➤ Server Theme has been updated", false);

                case "splash_hash" ->
                        eb.addField(MarkdownUtil.underline("Server Splash"), "╰┈➤ Server Splash image has been updated", false);

                case "banner_hash" ->
                        eb.addField(MarkdownUtil.underline("Server Banner"), "╰┈➤ Server Banner image has been updated", false);

                case "vanity_url_code" ->
                        eb.addField(MarkdownUtil.underline("Vanity URL Code"), "╰┈➤ Vanity URL code has been updated", false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key for Guild Update: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onGuildProfileUpdate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Guild Profile Update Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Guild Profile Updated By: " + mentionableExecutor + "\nTarget Guild: " + event.getGuild().getName()));
        eb.setColor(environment.embedColor().warningColor());

        ale.getChanges().keySet().forEach(key -> {
            switch (key) {
                case "traits" ->
                        eb.addField(MarkdownUtil.underline("Server Traits"), "╰┈➤Server Traits have been updated", false);
                case "visibility" ->
                        eb.addField(MarkdownUtil.underline("Visibility"), "╰┈➤Profile Visibility has been changed", false);
                case "brand_color_primary" ->
                        eb.addField(MarkdownUtil.underline("Banner Color"), "╰┈➤Banner Color has been updated", false);
                case "game_application_ids" ->
                        eb.addField(MarkdownUtil.underline("Games"), "╰┈➤Games have been updated", false);
                default -> eb.addField(MarkdownUtil.underline(key), "╰┈➤" + key + " has/have been updated", false);
            }
        });
        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }
}
