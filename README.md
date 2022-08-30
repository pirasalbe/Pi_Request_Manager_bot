# Pi_Request_Manager_bot

Telegram bot to manage requests.

## Disclaimer

The bot's goal is to allow admins to manage requests from groups.

This repo is an example of a request manager for ebooks/audiobooks.

Being an open-source repo, you can download and use the bot however you like, but I'm not responsible for any illicit use.

## Commands

- `/help`: shows available commands.

# Features

# Instructions

## Build

The following command will create an executable jar.

```
./gradlew bootJar
```

Use the following to generate a Raspberry executable jar.

```
./gradlew -Ppi bootJar
```

### Build and Run

The following command runs build the project and runs it.

```
./gradlew bootRun
```

## Check health

```
curl localhost:8080/actuator/health
```

### Variables

#### Required Variables

- `bot.token`: create a bot using [@BotFather](https://telegram.dog/BotFather), and get the Telegram API token.
- `bot.username`: bot username (without @).
- `bot.number`: user bot number with country code.
- `bot.api-id`: Telegram API ID for the user bot.
- `bot.api-hash`: Telegram API Hash for the user bot.
- `bot.backup-chat`: chat id where the bot will send the backups.
- `bot.log-chat`: chat id where the bot will the logs when errors happen.
- `spring.datasource.url`: url to connect to the PostgreSQL instance.
- `spring.datasource.username`: username to authenticate to the PostgreSQL instance.
- `spring.datasource.password`: password to authenticate to the PostgreSQL instance.

#### Optional Variables

- `bot.request-generators`: list of bots' username, comma separated, whose messages are always valid if link is provided.
- `error.incomplete-request`: custom message for incomplete requests.
- `error.non-bot-request`: custom message for invalid requests.
