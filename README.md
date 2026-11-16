# CrazyCratesDiscord

Discord webhook addon for CrazyCrates 5.0, by DELUXECLANS / SAMU_MRR.

## Build
Requires Java 21 and Maven:
`mvn clean package`

The jar will be in `target/CrazyCratesDiscord-1.0.0.jar`.

## Install
1. Build the project.
2. Put the jar in the Paper server's `plugins` folder beside CrazyCrates 5.0.
3. Start once to generate `plugins/CrazyCratesDiscord/config.yml`.
4. Paste your Discord webhook URL into `webhook.url`.
5. Restart the server.

## Placeholders
- `%player%` — player name
- `%crate%` — crate name
- `%prize%` — prize name
- `%time%` — local server time
- `%date%` — local server date

The webhook URL is secret. Never publish it; regenerate it in Discord if exposed.

## Important
This is source code, not a prebuilt or live-tested jar. The listener expects the CrazyCrates 5.0 API event
`com.badbones69.crazycrates.paper.api.events.PlayerPrizeEvent`. Verify against your exact CrazyCrates build on a test server before production use.
