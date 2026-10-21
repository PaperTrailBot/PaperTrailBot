package io.github.eggy03.papertrail.bot.service.handlers.guild.auditlog;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;
import io.github.eggy03.papertrail.bot.utils.BooleanUtils;
import io.github.eggy03.papertrail.bot.utils.DurationUtils;
import io.github.eggy03.papertrail.bot.utils.auditlog.MemberUtils;

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
public final class MemberUpdateActionTypeHandler extends AbstractGuildAuditLogEntryCreateEventActionTypeHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public MemberUpdateActionTypeHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
        this.guildApi = guildApi;
        this.environment = environment;
        this.embedCheckingService = embedCheckingService;
    }

    @Nullable
    private Long getRegisteredChannelId(@NonNull Long guildId) {
        try {
            return guildApi.getGuild(guildId).memberEventChannelId();
        } catch (ApiException e) {
            if (e.getStatusCode() >= 500)
                log.warn("API Error while attempting to fetch Guild", e);
            return null;
        }

    }


    @Override
    public void onMemberUpdate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        User targetUser = ale.getJDA().getUserById(ale.getTargetIdLong());
        String mentionableTargetUser = (targetUser != null ? targetUser.getAsMention() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Member Update Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Executor: " + mentionableExecutor + "\nTarget: " + mentionableTargetUser));
        eb.setColor(environment.embedColor().warningColor());

        if (targetUser != null)
            eb.setThumbnail(targetUser.getEffectiveAvatarUrl());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "communication_disabled_until" -> {
                    if (newValue == null) {
                        eb.setColor(environment.embedColor().successColor());
                        eb.addField(MarkdownUtil.underline("Timeout Lifted"), "╰┈➤ Timeout has been removed", false);
                    } else {
                        eb.setColor(environment.embedColor().warningColor());
                        eb.addField(MarkdownUtil.underline("Timeout Received"), "╰┈➤ Member has received a timeout", false);
                        eb.addField(MarkdownUtil.underline("Timeout Ends On"), "╰┈➤" + DurationUtils.isoToLocalTimeCounter(newValue), false);
                        eb.addField(MarkdownUtil.underline("Timeout Reason"), "╰┈➤" + (ale.getReason() != null ? ale.getReason() : "No Reason Provided"), false);
                    }
                }
                case "nick" ->
                        eb.addField(MarkdownUtil.underline("Nickname Update"), MemberUtils.resolveNickNameChanges(oldValue, newValue), false);

                case "mute" ->
                        eb.addField(MarkdownUtil.underline("Is Muted in VC"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), false);

                case "deaf" ->
                        eb.addField(MarkdownUtil.underline("Is Deafened in VC"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), false);

                case "bypasses_verification" ->
                        eb.addField(MarkdownUtil.underline("Bypass Verification"), "╰┈➤" + BooleanUtils.formatToEnabledOrDisabled(newValue), false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Member Update: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onMemberRoleUpdate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        User targetUser = ale.getJDA().getUserById(ale.getTargetIdLong());
        String mentionableTargetUser = (targetUser != null ? targetUser.getAsMention() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry  | Member Role Update");
        eb.setDescription(MarkdownUtil.quoteBlock("Executor: " + mentionableExecutor + "\nTarget: " + mentionableTargetUser));
        eb.setColor(environment.embedColor().warningColor());

        if (targetUser != null)
            eb.setThumbnail(targetUser.getEffectiveAvatarUrl());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {
                case "$add" ->
                        eb.addField(MarkdownUtil.underline("✅ Role(s) Added"), "╰┈➤" + MemberUtils.parseRoleListMap(event, newValue), false);

                case "$remove" ->
                        eb.addField(MarkdownUtil.underline("❌ Role(s) Removed"), "╰┈➤" + MemberUtils.parseRoleListMap(event, newValue), false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Member Role Update: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    // the audit log does not expose much information regarding member vc move and kick events
    // therefore GuildVoiceEventListener has been created to know about channels the target has been moved or kicked from
    @Override
    public void onMemberVoiceKick(@NonNull GuildAuditLogEntryCreateEvent event) {

        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserId());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Member Voice Kick Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Member Kicked From a Voice Channel\n By: " + mentionableExecutor));
        eb.setColor(environment.embedColor().warningColor());

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    // the audit log does not expose much information regarding member vc move and kick events
    // therefore GuildVoiceEventListener has been created to know about channels the target has been moved or kicked from
    @Override
    public void onMemberVoiceMove(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserId());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Member Voice Move Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Member Moved To A Different Voice Channel\n By: " + mentionableExecutor));
        eb.setColor(environment.embedColor().warningColor());

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }
}
