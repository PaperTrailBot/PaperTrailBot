# Deployment guide for the bot

> [!IMPORTANT]
>
> This guide assumes you already have a working PostgreSQL and Redis/Valkey instance or know how to set up one.

## Step 1: Create a bot in the Discord Dev Portal

Log on to the [Discord Developer Portal](https://discord.com/developers/applications) and create an application.

The application can have any name, avatar, banner and description but the following scopes, permissions and intents are
needed for it to work properly:

**Installation Contexts**

1) Guild Install

**Scopes**

1) applications.commands
2) bot

**Permissions**

1) Manage Server
2) Read Message History
3) Send Messages
4) Send Messages In Threads
5) View Audit Log
6) View Channels

**Privileged Gateway Intents**

1) Presence Intent
2) Server Members Intent
3) Message Content Intent

Note down the `BOT TOKEN` since it will be shown only once and will be required in the later steps.

## Step 2: Deploying the API and the Bot

### 2.1: Get Required Variables

| Variable      | Description/Format                                                   | Default Value    | Optional |
|---------------|----------------------------------------------------------------------|------------------|----------|
| `DB_URL`      | jdbc:postgresql://<DATABASE_HOST>:<DATABASE_PORT>/<DATABASE_NAME>    | No Default Value | No       |
| `DB_USERNAME` | Database Username                                                    | No Default Value | No       |
| `DB_PASSWORD` | Database Password                                                    | No Default Value | No       |
| `REDIS_URL`   | rediss://<REDIS_USERNAME>:<REDIS_PASSWORD>@<REDIS_HOST>:<REDIS_PORT> | No Default Value | No       |
| `TOKEN`       | Get from the dev portal                                              | No Default Value | No       |

### 2.2: Deployment

Use the following docker compose setup to deploy both the bot and the API

```yaml
networks:
  papertrail-network:

services:
  api:
    container_name: papertrail-api
    image: ghcr.io/papertrailbot/api-native:latest
    environment:
      DB_URL: ${DB_URL}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_URL: ${REDIS_URL}
    networks:
      - papertrail-network
    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:9000/q/health" ]
      interval: 10s
      timeout: 5s
      retries: 3

  bot:
    container_name: papertrail-bot
    image: ghcr.io/papertrailbot/bot-native:latest
    depends_on:
      api:
        condition: service_healthy
    environment:
      TOKEN: ${TOKEN}
      API_URL: http://papertrail-api:8080
    networks:
      - papertrail-network
    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:9000/q/health" ]
      interval: 10s
      timeout: 5s
      retries: 3
```

If you want to build from source and then deploy individually,
you can point your builder to this [and the API] repository's
[Dockerfile](/Dockerfile) or [Dockerfile Native (GraalVM Native Image)](/Dockerfile.native).

# Customization Options

The following environment variables have defaults, but you can change them to allow for more customization:

## Bot

| Environment Variable          | Description                                                | Default Value            | Optional |
|-------------------------------|------------------------------------------------------------|--------------------------|----------|
| `APP_NAME`                    | Changes the application name used internally.              | `PaperTrailBot`          | Yes      |
| `APP_ACTIVITY`                | Changes the activity displayed by the bot in Discord.      | `/help <latest_version>` | Yes      |
| `APP_LOG_LEVEL`               | Changes the application log level.                         | `INFO`                   | Yes      |
| `PORT`                        | Changes the port used by the application.                  | `8080`                   | Yes      |
| `MANAGEMENT_PORT`             | Changes the management port used by Quarkus health checks. | `9000`                   | Yes      |
| `EMBED_SUCCESS_COLOR_INT`     | Changes the color of embeds for **creation events**.       | `GREEN (712458)`         | Yes      |
| `EMBED_WARNING_COLOR_INT`     | Changes the color of embeds for **update events**.         | `YELLOW (16776960)`      | Yes      |
| `EMBED_DESTRUCTIVE_COLOR_INT` | Changes the color of embeds for **deletion events**.       | `RED (16711680)`         | Yes      |

## API

| Environment Variable | Description                                                | Default Value | Optional |
|----------------------|------------------------------------------------------------|---------------|----------|
| `LOG_LEVEL`          | Changes the application log level.                         | `INFO`        | Yes      |
| `PORT`               | Changes the port used by the application.                  | `8080`        | Yes      |
| `MANAGEMENT_PORT`    | Changes the management port used by Quarkus health checks. | `9000`        | Yes      |
