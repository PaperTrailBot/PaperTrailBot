package io.github.eggy03.papertrail.bot.service.handlers.guild.auditlog;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;
import io.github.eggy03.papertrail.bot.utils.auditlog.StickerUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.sticker.GuildSticker;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
@Slf4j
@SuppressWarnings("java:S1192")
public final class StickerActionTypeHandler extends AbstractGuildAuditLogEntryCreateEventActionTypeHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public StickerActionTypeHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
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
    public void onStickerCreate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        GuildSticker sticker = event.getGuild().getStickerById(ale.getTargetId());
        String mentionableSticker = (sticker != null ? sticker.getName() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Sticker Create Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Sticker Created By: " + mentionableExecutor + "\nCreated Sticker: " + mentionableSticker));
        eb.setColor(environment.embedColor().successColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {
                case "format_type", "type", "asset", "available", "guild_id" -> {
                    // skip
                }
                case "id" -> {
                    eb.addField(MarkdownUtil.underline("Sticker ID"), "╰┈➤" + newValue, false);
                    eb.addField(MarkdownUtil.underline("Sticker Link"), "╰┈➤" + StickerUtils.resolveStickerUrl(event, newValue), false);
                }
                case "tags" ->
                        eb.addField(MarkdownUtil.underline("Related Emoji"), "╰┈➤" + StickerUtils.resolveRelatedEmoji(event, newValue), false);
                case "description" -> eb.addField(MarkdownUtil.underline("Description"), "╰┈➤" + newValue, false);
                case "name" -> eb.addField(MarkdownUtil.underline("Sticker Name"), "╰┈➤" + newValue, false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Sticker Create: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onStickerUpdate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        GuildSticker sticker = event.getGuild().getStickerById(ale.getTargetId());
        String mentionableSticker = (sticker != null ? sticker.getName() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Sticker Update Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Sticker Updated By: " + mentionableExecutor + "\nUpdated Sticker: " + mentionableSticker));
        eb.setColor(environment.embedColor().warningColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {
                case "format_type", "type", "asset", "available", "guild_id", "id" -> {
                    // skip
                }
                case "tags" -> {
                    eb.addField(MarkdownUtil.underline("Old Related Emoji"), "╰┈➤" + StickerUtils.resolveRelatedEmoji(event, oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Related Emoji"), "╰┈➤" + StickerUtils.resolveRelatedEmoji(event, newValue), true);
                    eb.addBlankField(true);
                }
                case "description" -> {
                    eb.addField(MarkdownUtil.underline("Old Description"), "╰┈➤" + oldValue, true);
                    eb.addField(MarkdownUtil.underline("New Description"), "╰┈➤" + newValue, true);
                    eb.addBlankField(true);
                }
                case "name" -> {
                    eb.addField(MarkdownUtil.underline("Old Sticker Name"), "╰┈➤" + oldValue, true);
                    eb.addField(MarkdownUtil.underline("New Sticker Name"), "╰┈➤" + newValue, true);
                    eb.addBlankField(true);
                }
                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Sticker Update: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onStickerDelete(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        GuildSticker sticker = event.getGuild().getStickerById(ale.getTargetId());
        String mentionableSticker = (sticker != null ? sticker.getName() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Sticker Delete Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Sticker Deleted By: " + mentionableExecutor + "\nDeleted Sticker ID: " + mentionableSticker));
        eb.setColor(environment.embedColor().destructiveColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {
                case "format_type", "type", "asset", "available", "guild_id" -> {
                    // skip
                }
                case "id" -> {
                    eb.addField(MarkdownUtil.underline("Sticker ID"), "╰┈➤" + oldValue, false);
                    eb.addField(MarkdownUtil.underline("Sticker Link"), "╰┈➤" + StickerUtils.resolveStickerUrl(event, oldValue), false);
                }
                case "tags" ->
                        eb.addField(MarkdownUtil.underline("Related Emoji"), "╰┈➤" + StickerUtils.resolveRelatedEmoji(event, oldValue), false);
                case "description" -> eb.addField(MarkdownUtil.underline("Description"), "╰┈➤" + oldValue, false);
                case "name" -> eb.addField(MarkdownUtil.underline("Sticker Name"), "╰┈➤" + oldValue, false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Sticker Delete: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }
}
