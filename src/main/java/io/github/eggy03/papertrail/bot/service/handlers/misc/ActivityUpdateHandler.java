package io.github.eggy03.papertrail.bot.service.handlers.misc;

import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;

@ApplicationScoped
@Slf4j
public class ActivityUpdateHandler {

    private final @NonNull PaperTrailEnvironment paperTrailEnvironment;

    @Inject
    public ActivityUpdateHandler(@NonNull PaperTrailEnvironment paperTrailEnvironment) {
        this.paperTrailEnvironment = paperTrailEnvironment;
    }

    public void handleOnReady(@NonNull ReadyEvent event) { // update on cold start

        String customActivity = paperTrailEnvironment.general().appActivity();
        if (customActivity.isBlank()) {
            event.getJDA().getPresence().setActivity(Activity.customStatus("/help | v" + paperTrailEnvironment.general().appVersion()));
        } else
            event.getJDA().getPresence().setActivity(Activity.customStatus(customActivity));

    }

    public void handleOnGuildJoin(@NonNull GuildJoinEvent event) {
        log.info("Bot Added To [Guild={}, ID={}]", event.getGuild().getName(), event.getGuild().getId());
    }

    public void handleOnGuildLeave(@NonNull GuildLeaveEvent event) {
        log.info("Bot Removed From [Guild={}, ID={}]", event.getGuild().getName(), event.getGuild().getId());
    }
}
