package io.github.eggy03.papertrail.bot.listeners.misc;

import io.github.eggy03.papertrail.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.concurrent.ThreadFactory;

/*
 * This class will have methods that unregister the log channels from the database after the bot has been kicked
 */
@ApplicationScoped
public final class SelfKickListener extends ListenerAdapter {

    private final @NonNull GuildApi guildApi;
    private final @NonNull ThreadFactory virtualThreadFactory;

    @Inject
    public SelfKickListener(@NonNull @RestClient GuildApi guildApi, @NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory) {
        this.guildApi = guildApi;
        this.virtualThreadFactory = virtualThreadFactory;
    }

    @Override
    public void onGuildLeave(@NonNull GuildLeaveEvent event) {
        Guild leftGuild = event.getGuild();
        virtualThreadFactory
                .newThread(() -> guildApi.deleteGuild(leftGuild.getIdLong()))
                .start();

    }
}
