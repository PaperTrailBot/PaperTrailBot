package io.github.eggy03.papertrail.bot.environment;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import lombok.NonNull;

@ConfigMapping(prefix = "papertrail")
public interface PaperTrailEnvironment {

    General general();

    EmbedColor embedColor();
    Discord discord();

    interface General {

        @WithName("app.name")
        @NonNull
        String appName();

        @WithName("app.version")
        @NonNull
        String appVersion();

        @WithName("app.activity")
        @NonNull
        String appActivity();

        @WithName("github.issue.link")
        @NonNull
        String githubIssueLink();

    }

    interface EmbedColor {

        @WithName("success.color.integer")
        int successColor();

        @WithName("warning.color.integer")
        int warningColor();

        @WithName("destructive.color.integer")
        int destructiveColor();
    }

    interface Discord {

        @WithName("token")
        @NonNull
        String token();
    }

}
