package io.github.eggy03.papertrail.bot.service.handlers.guild.auditlog;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;

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
public final class EmojiActionTypeHandler extends AbstractGuildAuditLogEntryCreateEventActionTypeHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public EmojiActionTypeHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
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
    public void onEmojiCreate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Emoji Create Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Emoji Created By: " + mentionableExecutor));
        eb.setColor(environment.embedColor().successColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            if (changeKey.equals("name")) {
                eb.addField(MarkdownUtil.underline("Emoji Name"), "╰┈➤" + newValue, false);
                eb.addField(MarkdownUtil.underline("Emoji"), "╰┈➤" + "<:" + newValue + ":" + ale.getTargetId() + ">", false); // ale's TargetID retrieves the ID of the created emoji
            } else {
                eb.addField("Unimplemented Change Key", changeKey, false);
                log.info("Unimplemented Change Key on Emoji Create: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onEmojiUpdate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());


        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Emoji Update Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Emoji Updated By: " + mentionableExecutor));
        eb.setColor(environment.embedColor().warningColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            if (changeKey.equals("name")) {
                eb.addField(MarkdownUtil.underline("Emoji Name Updated"), "╰┈➤" + "From " + oldValue + " to " + newValue, false);
                eb.addField(MarkdownUtil.underline("Target Emoji"), "╰┈➤" + "<:" + newValue + ":" + ale.getTargetId() + ">", false);
            } else {
                eb.addField("Unimplemented Change Key", changeKey, false);
                log.info("Unimplemented Change Key on Emoji Update: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onEmojiDelete(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Emoji Delete Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Emoji Deleted By: " + mentionableExecutor));
        eb.setColor(environment.embedColor().destructiveColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            if (changeKey.equals("name")) {
                eb.addField(MarkdownUtil.underline("Emoji Name"), "╰┈➤" + oldValue, false);
            } else {
                eb.addField("Unimplemented Change Key", changeKey, false);
                log.info("Unimplemented Change Key on Emoji Delete: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
            }
        });
        eb.addField("Deleted Emoji ID", "╰┈➤" + ale.getTargetId(), false);

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }
}
