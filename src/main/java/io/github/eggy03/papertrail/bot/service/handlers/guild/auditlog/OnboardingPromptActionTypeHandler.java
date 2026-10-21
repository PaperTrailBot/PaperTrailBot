package io.github.eggy03.papertrail.bot.service.handlers.guild.auditlog;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;
import io.github.eggy03.papertrail.bot.utils.BooleanUtils;

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
public final class OnboardingPromptActionTypeHandler extends AbstractGuildAuditLogEntryCreateEventActionTypeHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public OnboardingPromptActionTypeHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
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
    public void onOnboardingPromptCreate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Onboarding Prompt Create Event");

        User executor = ale.getJDA().getUserById(ale.getUserId());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getTargetId());

        eb.setDescription(MarkdownUtil.quoteBlock("Onboarding Prompt Created By: " + mentionableExecutor));
        eb.setColor(environment.embedColor().successColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {
                case "single_select" ->
                        eb.addField(MarkdownUtil.underline("Single Selection Mode"), BooleanUtils.formatToYesOrNo(newValue), false);

                case "required" ->
                        eb.addField(MarkdownUtil.underline("Is Required"), BooleanUtils.formatToYesOrNo(newValue), false);

                case "type", "id" -> {
                    // skip
                }

                case "title" -> eb.addField(MarkdownUtil.underline("Question Title"), String.valueOf(newValue), false);

                case "options" ->
                        eb.addField(MarkdownUtil.underline("Question Options"), "Review the options in Onboarding Settings", false);

                case "in_onboarding" ->
                        eb.addField(MarkdownUtil.underline("Is a Pre-Join Question"), BooleanUtils.formatToYesOrNo(newValue), false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Onboarding Prompt Create: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }

        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onOnboardingPromptUpdate(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Onboarding Prompt Update Event");

        User executor = ale.getJDA().getUserById(ale.getUserId());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getTargetId());

        eb.setDescription(MarkdownUtil.quoteBlock("Onboarding Prompt Updated By: " + mentionableExecutor));
        eb.setColor(environment.embedColor().warningColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {
                case "single_select" -> {
                    eb.addField(MarkdownUtil.underline("Old Single Selection Mode"), BooleanUtils.formatToYesOrNo(oldValue), false);
                    eb.addField(MarkdownUtil.underline("New Single Selection Mode"), BooleanUtils.formatToYesOrNo(newValue), false);
                }

                case "required" -> {
                    eb.addField(MarkdownUtil.underline("Was Required"), BooleanUtils.formatToYesOrNo(oldValue), false);
                    eb.addField(MarkdownUtil.underline("Is Required"), BooleanUtils.formatToYesOrNo(newValue), false);
                }

                case "type", "id" -> {
                    // skip
                }

                case "title" -> {
                    eb.addField(MarkdownUtil.underline("Old Question Title"), String.valueOf(oldValue), false);
                    eb.addField(MarkdownUtil.underline("New Question Title"), String.valueOf(newValue), false);
                }

                case "options" ->
                        eb.addField(MarkdownUtil.underline("Question Options"), "Review the changed options in Onboarding Settings", false);

                case "in_onboarding" -> {
                    eb.addField(MarkdownUtil.underline("Was a Pre-Join Question"), BooleanUtils.formatToYesOrNo(oldValue), false);
                    eb.addField(MarkdownUtil.underline("Is a Pre-Join Question"), BooleanUtils.formatToYesOrNo(newValue), false);
                }

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Onboarding Prompt Update: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @Override
    public void onOnboardingPromptDelete(@NonNull GuildAuditLogEntryCreateEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        AuditLogEntry ale = event.getEntry();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Onboarding Prompt Delete Event");

        User executor = ale.getJDA().getUserById(ale.getUserId());
        String mentionableExecutor = (executor != null ? executor.getAsMention() : ale.getTargetId());

        eb.setDescription(MarkdownUtil.quoteBlock("Onboarding Prompt Deleted By: " + mentionableExecutor));
        eb.setColor(environment.embedColor().destructiveColor());

        ale.getChanges().forEach((changeKey, changeValue) -> {

            Object oldValue = changeValue.getOldValue();
            Object newValue = changeValue.getNewValue();

            switch (changeKey) {

                case "single_select" ->
                        eb.addField(MarkdownUtil.underline("Single Selection Mode"), BooleanUtils.formatToYesOrNo(oldValue), false);

                case "required" ->
                        eb.addField(MarkdownUtil.underline("Was Required"), BooleanUtils.formatToYesOrNo(oldValue), false);

                case "type", "id", "options" -> {
                    // skip
                }
                case "title" -> eb.addField(MarkdownUtil.underline("Question Title"), String.valueOf(oldValue), false);

                case "in_onboarding" ->
                        eb.addField(MarkdownUtil.underline("Was a Pre-Join Question"), BooleanUtils.formatToYesOrNo(oldValue), false);

                default -> {
                    eb.addField("Unimplemented Change Key", changeKey, false);
                    log.info("Unimplemented Change Key on Onboarding Prompt Delete: {}\nOLD_VALUE: {}\nNEW_VALUE: {}", changeKey, oldValue, newValue);
                }
            }
        });

        eb.setFooter("Audit Log Entry ID: " + ale.getId());
        eb.setTimestamp(ale.getTimeCreated());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }
}
