package io.github.eggy03.papertrail.bot.service.handlers.guild.auditlog;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;
import io.github.eggy03.papertrail.bot.utils.BooleanUtils;
import io.github.eggy03.papertrail.bot.utils.DurationUtils;
import io.github.eggy03.papertrail.bot.utils.auditlog.InviteUtils;

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
public final class InviteActionTypeHandler extends AbstractGuildAuditLogEntryCreateEventActionTypeHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public InviteActionTypeHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
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
    public void onInviteCreate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Invite Create Event");

        eb.setDescription(MarkdownUtil.quoteBlock("Invite Created By: " + mentionableExecutor));
        eb.setColor(environment.embedColor().successColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "code" -> eb.addField(MarkdownUtil.underline("Invite Code"), "╰┈➤" + newValue, false);

                case "inviter_id" ->
                        eb.addField(MarkdownUtil.underline("Invite Created By"), "╰┈➤" + InviteUtils.resolveInviter(newValue, event), false);

                case "temporary" ->
                        eb.addField(MarkdownUtil.underline("Is Temporary"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), false);

                case "max_uses" ->
                        eb.addField(MarkdownUtil.underline("Max Uses"), "╰┈➤" + InviteUtils.resolveMaxUses(newValue), false);

                case "uses", "flags" -> {
                    // ignore
                }

                case "role_ids" ->
                        eb.addField(MarkdownUtil.underline("Roles Attached"), "╰┈➤" + InviteUtils.resolveInviteRoleList(event, newValue), false);

                case "max_age" ->
                        eb.addField(MarkdownUtil.underline("Expires After"), "╰┈➤" + DurationUtils.formatSeconds(newValue), false);

                case "channel_id" ->
                        eb.addField(MarkdownUtil.underline("Invite Channel"), "╰┈➤" + InviteUtils.resolveInviteChannel(newValue, event), false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Invite Create: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onInviteUpdate(@NonNull GuildAuditLogEntryCreateEvent event) {
        log.warn("Invite Update Event Detected. Implement this sometime later\n{}", event.getEntry().getChanges());

        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Invite Update Event");
        eb.setColor(environment.embedColor().warningColor());

        String implementationNotice = "We do not have sufficient data to log the changes in an INVITE_UPDATE Event"
                .concat(" because invites seem to be immutable at the moment, which causes this event to not be fired.")
                .concat(" A proper implementation might happen in future releases if such an event is fired consistently.");

        eb.addField("Implementation Notice", MarkdownUtil.codeblock(implementationNotice), false);

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onInviteDelete(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Invite Delete Event");

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        eb.setDescription(MarkdownUtil.quoteBlock("Invite Deleted By: " + mentionableExecutor));
        eb.setColor(environment.embedColor().destructiveColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "code" -> eb.addField(MarkdownUtil.underline("Deleted Invite Code"), "╰┈➤" + oldValue, false);

                case "inviter_id" ->
                        eb.addField(MarkdownUtil.underline("Invite Deleted By"), "╰┈➤" + InviteUtils.resolveInviter(oldValue, event), false);

                case "temporary" ->
                        eb.addField(MarkdownUtil.underline("Was Temporary"), "╰┈➤" + BooleanUtils.formatToYesOrNo(oldValue), false);

                case "max_uses", "flags", "max_age" -> {
                    // ignore
                }

                case "role_ids" ->
                        eb.addField(MarkdownUtil.underline("Roles Attached"), "╰┈➤" + InviteUtils.resolveInviteRoleList(event, oldValue), false);

                case "uses" -> eb.addField(MarkdownUtil.underline("Use Count"), "╰┈➤" + oldValue, false);

                case "channel_id" ->
                        eb.addField(MarkdownUtil.underline("Invite Channel"), "╰┈➤" + InviteUtils.resolveInviteChannel(oldValue, event), false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Invite Delete: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }

            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }
}
