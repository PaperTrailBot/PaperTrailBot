package io.github.eggy03.papertrail.bot.listeners.misc;

import io.github.eggy03.papertrail.bot.service.handlers.misc.SlashCommandRegistrationHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/*
 * Registers all the slash commands places throughout the code on startup
 */
@ApplicationScoped
public final class SlashCommandRegistrationListener extends ListenerAdapter {

    private final @NonNull SlashCommandRegistrationHandler handler;

    @Inject
    public SlashCommandRegistrationListener(@NonNull SlashCommandRegistrationHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onReady(@NonNull ReadyEvent event) {
        handler.registerCommands(event);
    }
}
