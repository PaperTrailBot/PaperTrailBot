package io.github.eggy03.papertrail.bot.service.handlers.guild.auditlog;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;
import io.github.eggy03.papertrail.bot.utils.BooleanUtils;
import io.github.eggy03.papertrail.bot.utils.auditlog.RoleUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
@Slf4j
@SuppressWarnings("java:S1192")
public final class RoleActionTypeHandler extends AbstractGuildAuditLogEntryCreateEventActionTypeHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public RoleActionTypeHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
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
    public void onRoleCreate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserId());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        Role targetRole = ale.getJDA().getRoleById(ale.getTargetId());
        String mentionableTargetRole = (targetRole != null ? targetRole.getAsMention() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Role Create Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Role Created By: " + mentionableExecutor + "\nTarget Role: " + mentionableTargetRole));
        eb.setColor(environment.embedColor().successColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "name" -> eb.addField(MarkdownUtil.underline("Role Name"), "╰┈➤" + newValue, false);

                case "colors", "hoist", "color", "permissions", "mentionable" -> {
                    // do nothing
                    /* discord for some reason shows the following to be default/null even
                     * when you set them during the creation of the role itself
                     * and delegates them to ROLE_UPDATE event
                     */
                }
                case "icon_hash", "unicode_emoji" -> {
                    // ignore
                }

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Role Create: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onRoleUpdate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserId());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        Role targetRole = ale.getJDA().getRoleById(ale.getTargetId());
        String mentionableTargetRole = (targetRole != null ? targetRole.getAsMention() : ale.getTargetId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Role Update Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Role Updated By: " + mentionableExecutor + "\nTarget Role: " + mentionableTargetRole));
        eb.setColor(environment.embedColor().warningColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "name" -> {
                    eb.addField(MarkdownUtil.underline("Old Role Name"), "╰┈➤" + oldValue, true);
                    eb.addField(MarkdownUtil.underline("New Role Name"), "╰┈➤" + newValue, true);
                    eb.addBlankField(true);
                }

                case "hoist" -> {
                    eb.addField(MarkdownUtil.underline("Old Display Separately"), "╰┈➤" + BooleanUtils.formatToYesOrNo(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Display Separately"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), true);
                    eb.addBlankField(true);
                }

                case "color" -> {
                    eb.addField(MarkdownUtil.underline("Old Color"), "╰┈➤" + RoleUtils.formatToHex(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Color"), "╰┈➤" + RoleUtils.formatToHex(newValue), true);
                    eb.addBlankField(true);
                }

                case "permissions" -> {
                    eb.addField(MarkdownUtil.underline("Old Role Permissions"), RoleUtils.resolveRolePermissions(oldValue, "✅"), true);
                    eb.addField(MarkdownUtil.underline("New Role Permissions"), RoleUtils.resolveRolePermissions(newValue, "✅"), true);
                    eb.addBlankField(true);
                }

                case "mentionable" -> {
                    eb.addField(MarkdownUtil.underline("Old Mentionable"), "╰┈➤" + BooleanUtils.formatToYesOrNo(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Mentionable"), "╰┈➤" + BooleanUtils.formatToYesOrNo(newValue), true);
                    eb.addBlankField(true);
                }

                case "colors" -> {
                    eb.addField(MarkdownUtil.underline("Old Gradient Color System"), RoleUtils.formatGradientToHex(oldValue), true);
                    eb.addField(MarkdownUtil.underline("New Gradient Color System"), RoleUtils.formatGradientToHex(newValue), true);
                    eb.addBlankField(true);
                }

                case "icon_hash" ->
                        eb.addField(MarkdownUtil.underline("Role Icon Update"), "╰┈➤Role Icon has been updated", false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Role Update: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }

            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onRoleDelete(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        User executor = ale.getJDA().getUserById(ale.getUserId());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getUserId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Role Delete Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Role Deleted By: " + mentionableExecutor + "\nTarget Role ID: " + ale.getTargetId()));
        eb.setColor(environment.embedColor().destructiveColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {
            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "name" -> eb.addField(MarkdownUtil.underline("Role Name"), "╰┈➤" + oldValue, false);

                case "hoist" ->
                        eb.addField(MarkdownUtil.underline("Display Separately"), "╰┈➤" + BooleanUtils.formatToYesOrNo(oldValue), false);

                case "color" ->
                        eb.addField(MarkdownUtil.underline("Color"), "╰┈➤" + RoleUtils.formatToHex(oldValue), false);

                case "permissions" ->
                        eb.addField(MarkdownUtil.underline("Role Permissions"), RoleUtils.resolveRolePermissions(oldValue, "✅"), false);

                case "mentionable" ->
                        eb.addField(MarkdownUtil.underline("Mentionable"), "╰┈➤" + BooleanUtils.formatToYesOrNo(oldValue), false);

                case "colors" ->
                        eb.addField(MarkdownUtil.underline("Gradient Color System"), RoleUtils.formatGradientToHex(oldValue), false);

                case "icon_hash", "unicode_emoji" -> {
                    // ignore
                }

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Role Delete: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }

        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }
}
