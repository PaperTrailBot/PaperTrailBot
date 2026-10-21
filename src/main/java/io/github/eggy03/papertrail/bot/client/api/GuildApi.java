package io.github.eggy03.papertrail.bot.client.api;

import io.github.eggy03.papertrail.bot.client.exception.ApiException;
import io.github.eggy03.papertrail.bot.client.model.ApiError;
import io.github.eggy03.papertrail.bot.client.model.GuildDTO;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jspecify.annotations.NonNull;

import java.time.Instant;

@Path("api/v2/guild")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "guild-api")
public interface GuildApi {

    @ClientExceptionMapper
    @Blocking
    static @NonNull ApiException toException(Response response) {

        if (response == null)
            return new ApiException(999, new ApiError(999, "NullResponse", "jakarta.ws.rs.core.Response was null", Instant.now(), "N/A"));

        ApiError apiError = response.hasEntity()
                ? response.readEntity(ApiError.class)
                : new ApiError(response.getStatus(), "ApiErrorCaptureFailure", "API Error could not be deserialized", Instant.now(), "N/A");

        return new ApiException(response.getStatus(), apiError);
    }

    @POST
    void saveGuild(GuildDTO dto);

    @GET
    @Path("/{guildId}")
    GuildDTO getGuild(@PathParam("guildId") Long guildId);

    @PATCH
    void updateGuild(GuildDTO dto);

    @DELETE
    @Path("/{guildId}")
    void deleteGuild(@PathParam("guildId") Long guildId);
}
