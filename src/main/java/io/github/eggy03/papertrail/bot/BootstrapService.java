package io.github.eggy03.papertrail.bot;

import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

@Slf4j
@ApplicationScoped
@Startup
public final class BootstrapService {

    private final @NonNull PaperTrailEnvironment paperTrailEnvironment;
    private final @NonNull Instance<ListenerAdapter> listeners;
    private final @NonNull ShardManager shardManager;

    @Inject
    public BootstrapService(@NonNull PaperTrailEnvironment paperTrailEnvironment, @NonNull Instance<ListenerAdapter> listeners) {
        this.paperTrailEnvironment = paperTrailEnvironment;
        this.listeners = listeners;
        this.shardManager = constructShardManager();
    }

    @NonNull
    ShardManager constructShardManager() {

        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(paperTrailEnvironment.discord().token());

        builder.enableIntents(
                GatewayIntent.SCHEDULED_EVENTS,
                GatewayIntent.AUTO_MODERATION_EXECUTION,
                GatewayIntent.AUTO_MODERATION_CONFIGURATION,
                GatewayIntent.MESSAGE_CONTENT, // priv
                GatewayIntent.GUILD_MEMBERS, // priv
                GatewayIntent.GUILD_MODERATION,
                GatewayIntent.GUILD_EXPRESSIONS,
                GatewayIntent.GUILD_WEBHOOKS,
                GatewayIntent.GUILD_INVITES,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.SCHEDULED_EVENTS
        );

        //cache all members
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);

        builder.enableCache(
                CacheFlag.MEMBER_OVERRIDES,
                CacheFlag.ROLE_TAGS,
                CacheFlag.FORUM_TAGS,
                CacheFlag.SCHEDULED_EVENTS,
                CacheFlag.VOICE_STATE
        );
        // chunk all guilds
        builder.setChunkingFilter(ChunkingFilter.ALL);

        // set status
        builder.setStatus(OnlineStatus.ONLINE);

        // add listeners
        listeners.forEach(listener -> {
            log.debug("Registering Listener: {}", listener.getClass().getSimpleName());
            builder.addEventListeners(listener);
        });

        // build shard manager and login
        return builder.build();
    }

    @Produces
    @ApplicationScoped
    @NonNull
    ShardManager getShardManager() {
        return shardManager;
    }

    @PreDestroy
    void shutdown() {
        log.info("Shutting down all shards...");
        shardManager.shutdown();
    }
}
