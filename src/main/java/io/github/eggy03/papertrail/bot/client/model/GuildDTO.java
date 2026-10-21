package io.github.eggy03.papertrail.bot.client.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;

@RegisterForReflection
public record GuildDTO(
        @NonNull Long guildId,
        @Nullable Long guildEventChannelId,
        @Nullable Long memberEventChannelId,
        @Nullable Long messageEventChannelId
) {
}