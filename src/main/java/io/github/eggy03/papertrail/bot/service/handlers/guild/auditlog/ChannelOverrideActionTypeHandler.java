package io.github.eggy03.papertrail.bot.service.handlers.guild.auditlog;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;
import io.github.eggy03.papertrail.bot.utils.auditlog.ChannelUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
@Slf4j
@SuppressWarnings("java:S1192")
public final class ChannelOverrideActionTypeHandler extends AbstractGuildAuditLogEntryCreateEventActionTypeHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public ChannelOverrideActionTypeHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
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
    public void onChannelOverrideCreate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        GuildChannel targetChannel = event.getGuild().getGuildChannelById(ale.getTargetId());
        String targetChannelMention = (targetChannel != null ? targetChannel.getAsMention() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Channel Override Create");
        eb.setDescription(MarkdownUtil.quoteBlock("Override Created By: " + mentionableExecutor + "\nTarget Channel: " + targetChannelMention));
        eb.setColor(environment.embedColor().successColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "type" ->
                        eb.addField(MarkdownUtil.underline("Override Type"), "╰┈➤" + ChannelUtils.resolveChannelOverrideTargetType(newValue), false);

                case "deny" ->
                        eb.addField(MarkdownUtil.underline("Denied Permissions"), ChannelUtils.resolveChannelOverridePermissions(newValue, "❌"), false);
                case "allow" ->
                        eb.addField(MarkdownUtil.underline("Allowed Permissions"), ChannelUtils.resolveChannelOverridePermissions(newValue, "✅"), false);

                // id exposes the member/role id which for which the channel permissions are overridden
                // only one member/role permission id is fetched per loop
                case "id" ->
                        eb.addField(MarkdownUtil.underline("Target Member/Role"), "╰┈➤" + ChannelUtils.autoResolveMemberOrRole(newValue, event), false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key for Channel Override Create: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());


        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onChannelOverrideUpdate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        GuildChannel targetChannel = event.getGuild().getGuildChannelById(ale.getTargetId());
        String targetChannelMention = (targetChannel != null ? targetChannel.getAsMention() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Channel Override Update");
        eb.setDescription(MarkdownUtil.quoteBlock("Override Updated By: " + mentionableExecutor + "\nTarget Channel: " + targetChannelMention));
        eb.setColor(environment.embedColor().warningColor());

        eb.addField(MarkdownUtil.underline("Override Type"), "╰┈➤" + ChannelUtils.resolveChannelOverrideTargetType(ale.getOptionByName("type")), false);
        eb.addField(MarkdownUtil.underline("Permissions Overridden For"), "╰┈➤" + getTargetRoleOrMember(ale, event), false);

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "deny" ->
                        eb.addField(MarkdownUtil.underline("Denied Permissions"), ChannelUtils.resolveChannelOverridePermissions(newValue, "❌"), false);
                case "allow" ->
                        eb.addField(MarkdownUtil.underline("Allowed Permissions"), ChannelUtils.resolveChannelOverridePermissions(newValue, "✅"), false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key for Channel Override Update: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }

            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onChannelOverrideDelete(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserIdLong());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        GuildChannel targetChannel = event.getGuild().getGuildChannelById(ale.getTargetId());
        String targetChannelMention = (targetChannel != null ? targetChannel.getAsMention() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Channel Override Delete");

        eb.setDescription(MarkdownUtil.quoteBlock("Override Deleted By: " + mentionableExecutor + "\nTarget Channel: " + targetChannelMention));
        eb.setColor(environment.embedColor().destructiveColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "type" ->
                        eb.addField(MarkdownUtil.underline("Override Type"), "╰┈➤" + ChannelUtils.resolveChannelOverrideTargetType(oldValue), false);

                case "deny" ->
                        eb.addField(MarkdownUtil.underline("Denied Permissions"), ChannelUtils.resolveChannelOverridePermissions(oldValue, "❌"), false);
                case "allow" ->
                        eb.addField(MarkdownUtil.underline("Allowed Permissions"), ChannelUtils.resolveChannelOverridePermissions(oldValue, "✅"), false);

                // id exposes the member/role id which for which the channel permissions are overridden
                // only one member/role permission id is fetched per loop
                case "id" ->
                        eb.addField(MarkdownUtil.underline("Target Member/Role"), "╰┈➤" + ChannelUtils.autoResolveMemberOrRole(oldValue, event), false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key for Channel Override Delete: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    // ALE changes do not expose the id and type keys in case of override updates
    // therefore, this hacky method is required
    // IDK why channel override updates don't show this info
    @NonNull
    private String getTargetRoleOrMember(@NonNull AuditLogEntry ale, @NonNull GuildAuditLogEntryCreateEvent event) {

        String overriddenId = ale.getOptionByName("id"); // id of the member or role
        String overriddenType = ale.getOptionByName("type"); // type that determines if the ID is that of a role or a member

        if (overriddenId == null)
            return "Member or Role ID is null";

        return switch (overriddenType) {

            case "0" -> {
                // It’s a role
                Role role = event.getGuild().getRoleById(overriddenId);
                yield role != null ? role.getAsMention() : overriddenId;
            }

            case "1" -> {
                // It's a member
                Member member = event.getGuild().getMemberById(overriddenId);
                yield member != null ? member.getAsMention() : overriddenId;
            }

            case null -> "Override Type is null";
            default -> "Unimplemented Override Type: " + overriddenType;

        };
    }
}
