package io.github.eggy03.papertrail.bot.service.handlers.misc;

import io.github.eggy03.papertrail.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.concurrent.ThreadFactory;

@ApplicationScoped
public class SelfKickHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull ThreadFactory virtualThreadFactory;

    @Inject
    public SelfKickHandler(@NonNull @RestClient GuildApi guildApi, @NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory) {
        this.guildApi = guildApi;
        this.virtualThreadFactory = virtualThreadFactory;
    }

    public void handleGuildLeave(@NonNull GuildLeaveEvent event) {
        Guild leftGuild = event.getGuild();
        virtualThreadFactory
                .newThread(() -> guildApi.deleteGuild(leftGuild.getIdLong()))
                .start();

    }
}
