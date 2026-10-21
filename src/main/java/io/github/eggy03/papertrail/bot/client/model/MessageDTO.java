package io.github.eggy03.papertrail.bot.client.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.NonNull;

@RegisterForReflection
public record MessageDTO(
        @NonNull Long messageId,
        @NonNull String messageContent,
        @NonNull Long authorId
) {
}