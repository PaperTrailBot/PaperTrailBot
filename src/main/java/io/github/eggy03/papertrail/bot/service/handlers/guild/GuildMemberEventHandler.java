package io.github.eggy03.papertrail.bot.service.handlers.guild;

import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;
import io.github.eggy03.papertrail.bot.utils.BooleanUtils;
import io.github.eggy03.papertrail.bot.utils.DurationUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@ApplicationScoped
@Slf4j
public final class GuildMemberEventHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public GuildMemberEventHandler(@NonNull @RestClient GuildApi guildApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
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

    public void handleGuildMemberJoin(@NonNull GuildMemberJoinEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        Guild guild = event.getGuild();
        User user = event.getUser();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Member Join Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Member Joined: " + user.getName() + "\nGuild: " + guild.getName()));
        eb.setThumbnail(user.getEffectiveAvatarUrl());
        eb.setColor(environment.embedColor().successColor());

        eb.addField(MarkdownUtil.underline("Member Name"), "╰┈➤" + user.getName(), false);
        eb.addField(MarkdownUtil.underline("Member Mention"), "╰┈➤" + user.getAsMention(), false);
        eb.addField(MarkdownUtil.underline("Member ID"), "╰┈➤" + user.getId(), false);
        eb.addField(MarkdownUtil.underline("Account Created"), "╰┈➤" + DurationUtils.isoToLocalTimeCounter(user.getTimeCreated()), false);
        eb.addField(MarkdownUtil.underline("Bot Account"), "╰┈➤" + BooleanUtils.formatToYesOrNo(user.isBot()), false);

        eb.setFooter(event.getGuild().getName());
        eb.setTimestamp(Instant.now());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    public void handleGuildMemberRemove(@NonNull GuildMemberRemoveEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        Guild guild = event.getGuild();
        User user = event.getUser();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Audit Log Entry | Member Leave Event");
        eb.setDescription(MarkdownUtil.quoteBlock("Member Left: " + user.getName() + "\nGuild: " + guild.getName()));
        eb.setThumbnail(user.getEffectiveAvatarUrl());
        eb.setColor(environment.embedColor().destructiveColor());

        eb.addField(MarkdownUtil.underline("Member Name"), "╰┈➤" + user.getName(), false);
        eb.addField(MarkdownUtil.underline("Member ID"), "╰┈➤" + user.getId(), false);
        eb.addField(MarkdownUtil.underline("Member Account Created"), "╰┈➤" + DurationUtils.isoToLocalTimeCounter(user.getTimeCreated()), false);
        eb.addField(MarkdownUtil.underline("Member Joined The Server On"), "╰┈➤" + getMemberJoinDate(event), false);
        eb.addField(MarkdownUtil.underline("Member Left The Server On"), "╰┈➤" + getMemberLeaveDate(), false);

        eb.setFooter(event.getGuild().getName());
        eb.setTimestamp(Instant.now());

        embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);
    }

    @NonNull
    private String getMemberJoinDate(@NonNull GuildMemberRemoveEvent event) {

        Member member = event.getMember();
        if (member == null)
            return "Member not cached";

        if (member.hasTimeJoined())
            return TimeFormat.DATE_TIME_LONG.format(member.getTimeJoined());

        return "Unavailable";
    }

    @NonNull
    private String getMemberLeaveDate() {
        return TimeFormat.DATE_TIME_LONG.format(Instant.now());
    }
}
