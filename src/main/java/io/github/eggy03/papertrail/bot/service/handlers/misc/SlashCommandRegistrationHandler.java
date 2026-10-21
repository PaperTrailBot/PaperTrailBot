package io.github.eggy03.papertrail.bot.service.handlers.misc;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

@ApplicationScoped
public class SlashCommandRegistrationHandler {

    public void registerCommands(@NonNull ReadyEvent event) {

        // main commands
        CommandData config = Commands
                .slash("config", "Configures the bot in your guild")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                .addSubcommands(
                        new SubcommandData("save", "Creates a configuration for your guild")
                                .addOption(OptionType.CHANNEL, "guild_event_channel", "Set channel for guild events", false)
                                .addOption(OptionType.CHANNEL, "member_event_channel", "Set channel for member events", false)
                                .addOption(OptionType.CHANNEL, "message_event_channel", "Set channel for message events", false),

                        new SubcommandData("view", "Shows the current configuration for your guild"),

                        new SubcommandData("update", "Updates the current configuration")
                                .addOption(OptionType.CHANNEL, "guild_event_channel", "Set channel for guild events", false)
                                .addOption(OptionType.CHANNEL, "member_event_channel", "Set channel for member events", false)
                                .addOption(OptionType.CHANNEL, "message_event_channel", "Set channel for message events", false)
                );

        // general commands
        CommandData serverStats = Commands
                .slash("stats", "Provides Server Statistics")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED);

        CommandData help = Commands
                .slash("help", "Provides a guide on setting up the bot")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED);

        CommandData debug = Commands
                .slash("debug", "Provides standard debug info for troubleshooting")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED);

        event.getJDA().updateCommands()
                .addCommands(config, serverStats, help, debug)
                .queue();
    }
}
