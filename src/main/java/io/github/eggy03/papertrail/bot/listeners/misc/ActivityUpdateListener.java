package io.github.eggy03.papertrail.bot.listeners.misc;

import io.github.eggy03.papertrail.bot.service.handlers.misc.ActivityUpdateHandler;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@ApplicationScoped
@Slf4j
public final class ActivityUpdateListener extends ListenerAdapter {

    private final @NonNull ActivityUpdateHandler handler;

    public ActivityUpdateListener(@NonNull ActivityUpdateHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onReady(@NonNull ReadyEvent event) { // update on cold start
        handler.handleOnReady(event);
    }

    @Override
    public void onGuildJoin(@NonNull GuildJoinEvent event) { // update on guild join
        handler.handleOnGuildJoin(event);
    }

    @Override
    public void onGuildLeave(@NonNull GuildLeaveEvent event) { // update on guild leave
        handler.handleOnGuildLeave(event);
    }
}