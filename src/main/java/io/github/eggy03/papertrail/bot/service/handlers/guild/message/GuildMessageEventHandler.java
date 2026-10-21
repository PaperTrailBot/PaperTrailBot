package io.github.eggy03.papertrail.bot.service.handlers.guild.message;

import com.google.common.base.Splitter;
import io.github.eggy03.papertrail.bot.client.api.GuildApi;
import io.github.eggy03.papertrail.bot.client.api.MessageApi;
import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.client.model.MessageDTO;
import io.github.eggy03.papertrail.bot.environment.PaperTrailEnvironment;
import io.github.eggy03.papertrail.bot.service.EmbedCheckingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
@Slf4j
public final class GuildMessageEventHandler {

    private final @NonNull GuildApi guildApi;
    private final @NonNull MessageApi messageApi;
    private final @NonNull PaperTrailEnvironment environment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public GuildMessageEventHandler(@NonNull @RestClient GuildApi guildApi, @NonNull @RestClient MessageApi messageApi, @NonNull PaperTrailEnvironment environment, @NonNull EmbedCheckingService embedCheckingService) {
        this.guildApi = guildApi;
        this.messageApi = messageApi;
        this.environment = environment;
        this.embedCheckingService = embedCheckingService;
    }

    @Nullable
    private Long getRegisteredChannelId(@NonNull Long guildId) {
        try {
            return guildApi.getGuild(guildId).messageEventChannelId();
        } catch (ApiException e) {
            if (e.getStatusCode() >= 500)
                log.warn("API Error while attempting to fetch Guild", e);
            return null;
        }

    }

    public void handleMessageReceivedEvent(@NonNull MessageReceivedEvent event) {
        if (getRegisteredChannelId(event.getGuild().getIdLong()) == null)
            return;

        Long messageId = event.getMessageIdLong();
        String messageContent = event.getMessage().getContentDisplay();
        Long authorId = event.getAuthor().getIdLong();

        messageApi.saveMessage(new MessageDTO(messageId, messageContent, authorId));
    }

    public void handleMessageUpdateEvent(@NonNull MessageUpdateEvent event) {

        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        try {
            // fetch the old message DTO from the API
            MessageDTO oldMessageDTO = messageApi.getMessage(event.getMessageIdLong());

            // Fetch the old message content
            String oldMessage = oldMessageDTO.messageContent();
            // fetch the updated message and its author from the event
            String updatedMessage = event.getMessage().getContentDisplay();
            String updatedMessageAuthor = event.getAuthor().getAsMention();
            Long updatedMessageAuthorId = event.getAuthor().getIdLong();

            // Ignore events where the message content wasn't edited (e.g., pin, embed resolve, thread creates and updates)
            // This is required since MessageUpdateEvent is triggered in case of pins and embed resolves with no change to the text content
            if (updatedMessage.equals(oldMessage))
                return;

            // Prepare for sending the text diff

            // Splitting is required because each field in an embed can display only up-to 1024 characters
            // A full embed can display up-to 6000 characters
            List<String> oldMessageSplits = Splitter.fixedLength(1000).splitToList(oldMessage);
            List<String> updatedMessageSplits = Splitter.fixedLength(1000).splitToList(updatedMessage);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Message Edit Event");
            eb.setDescription(MarkdownUtil.quoteBlock("Author: " + updatedMessageAuthor + "\n" + "Channel: " + event.getChannel().getAsMention()));
            eb.setColor(environment.embedColor().warningColor());

            oldMessageSplits.forEach(split -> eb.addField(MarkdownUtil.underline("Old Message"), MarkdownUtil.codeblock(split), false));
            updatedMessageSplits.forEach(split -> eb.addField(MarkdownUtil.underline("New Message"), MarkdownUtil.codeblock(split), false));

            eb.setFooter(event.getGuild().getName());
            eb.setTimestamp(Instant.now());

            embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);

            // call the API to update the message
            messageApi.updateMessage(new MessageDTO(event.getMessageIdLong(), updatedMessage, updatedMessageAuthorId));

        } catch (ApiException e) {
            if (e.getStatusCode() >= 500)
                log.warn("API Error while saving updated message", e);
        }
    }

    public void handleMessageDeleteEvent(@NonNull MessageDeleteEvent event) {
        Long channelIdToSendTo = getRegisteredChannelId(event.getGuild().getIdLong());
        if (channelIdToSendTo == null) return;

        try {
            // fetch the message DTO from the API which was just deleted from discord
            MessageDTO deletedMessageDTO = messageApi.getMessage(event.getMessageIdLong());

            // Fetch the deleted message and it's author id
            String deletedMessage = deletedMessageDTO.messageContent();
            Long deletedMessageAuthorId = deletedMessageDTO.authorId();

            User author = event.getJDA().getUserById(deletedMessageAuthorId);
            String mentionableAuthor = (author != null ? author.getAsMention() : deletedMessageAuthorId.toString());

            // Splitting is required because each field in an embed can display only up-to 1024 characters
            List<String> deletedMessageSplits = Splitter.fixedLength(1000).splitToList(deletedMessage);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Message Delete Event");
            eb.setDescription(MarkdownUtil.quoteBlock("Author: " + mentionableAuthor + "\n" + "Channel: " + event.getChannel().getAsMention()));
            eb.setColor(environment.embedColor().destructiveColor());

            deletedMessageSplits.forEach(split -> eb.addField(MarkdownUtil.underline("Deleted Message"), MarkdownUtil.codeblock(split), false));

            eb.setFooter(event.getGuild().getName());
            eb.setTimestamp(Instant.now());

            embedCheckingService.checkAndSend(event, eb, channelIdToSendTo);

            // call the API to delete the message
            messageApi.deleteMessage(event.getMessageIdLong());

        } catch (ApiException e) {
            if (e.getStatusCode() >= 500)
                log.warn("API Error while deleting message", e);
        }
    }
}
