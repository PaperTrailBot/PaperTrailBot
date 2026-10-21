package io.github.eggy03.papertrail.bot.listeners.misc;

import io.github.eggy03.papertrail.bot.service.handlers.misc.SelfKickHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/*
 * This class will have methods that unregister the log channels from the database after the bot has been kicked
 */
@ApplicationScoped
public final class SelfKickListener extends ListenerAdapter {

    private final @NonNull SelfKickHandler handler;

    @Inject
    public SelfKickListener(@NonNull SelfKickHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onGuildLeave(@NonNull GuildLeaveEvent event) {
        handler.handleGuildLeave(event);
    }
}
