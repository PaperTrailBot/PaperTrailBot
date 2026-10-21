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

// for events not defined by JDA
@ApplicationScoped
@Slf4j
@SuppressWarnings("java:S1192")
public final class UnknownActionTypeHandler extends AbstractGuildAuditLogEntryCreateEventActionTypeHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public UnknownActionTypeHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
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
    public void onUnknownActionType(@NonNull GuildAuditLogEntryCreateEvent event) {

        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = executor != null ? executor.getAsMention() : ale.getUserId();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Generic Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Executor: " + mentionableExecutor));
        eb.setColor(environment.embedColor().warningColor());

        eb.addField(MarkdownUtil.underline("Action Type"), MarkdownUtil.codeblock(ale.getType().toString()), true);
        eb.addField(MarkdownUtil.underline("Target Type"), MarkdownUtil.codeblock(ale.getTargetType().toString()), true);

        ale.getChanges().forEach((changeKey, changeValue) ->
                eb.addField(
                        MarkdownUtil.underline(changeKey),
                        MarkdownUtil.codeblock("OLD VALUE\n" + changeValue.getOldValue() + "\nNEW VALUE\n" + changeValue.getNewValue()),
                        false
                )
        );

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }
}
