# Pi_Request_Manager_bot

Telegram bot to manage requests.

## Disclaimer

The bot's goal is to allow admins to manage requests from groups.

This repo is an example of a request manager for ebooks/audiobooks.

Being an open-source repo, you can download and use the bot however you like, but I'm not responsible for any illicit use.

## Commands

Only admins can execute commands.

- `/start` and `/alive`: replies if the bot is alive.
- `/help`: shows available commands.

# Features

# Instructions

## Build

The following command will create an executable jar.

```
./gradlew bootJar
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

- `bot.token`: Create a bot using [@BotFather](https://telegram.dog/BotFather), and get the Telegram API token.

#### Optional Variables
