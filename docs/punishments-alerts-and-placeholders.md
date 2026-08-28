# Punishments, Alerts, and Placeholders

## Violation → Punishment Threshold Flow

Per-player per-check counters live in `player/violation/PlayerViolations.java:10` as an `AtomicIntegerArray` indexed by `CheckName.ordinal()`. Increments and reads are thread-safe. All flags originate from a `LACViolationEvent` fired by a check; `ViolationHandler` (`util/violation/ViolationHandler.java:34`) is the sole consumer that turns flags into state, alerts, logs, webhooks, setbacks, and punishments.

### 1. Flag emitted

A check calls `Bukkit.getPluginManager().callEvent(new LACViolationEvent(checkSetting, player, lacPlayer, cancellable))`. `LACViolationEvent` (`api/event/LACViolationEvent.java:11`) carries `CheckSetting`, `Player`, `LACPlayer`, and a nullable `Cancellable` (the Bukkit event that can be cancelled for setback). It implements `Cancellable` itself — external plugins may `setCancelled(true)` to suppress handling when `api.enabled=true`.

### 2. `onFlag` — `ViolationHandler.java:83` (priority `HIGHEST`)

Guards:
- If player offline or `lacPlayer.leaveTime != 0`, ignored.
- If `ConfigManager.Config.Api.enabled && event.isCancelled()` → return (external cancellation honored only when API enabled; when `api.enabled=false` cancellations are ignored).

Threshold logic:

```java
if (checkSetting.punishmentVio == lacPlayer.violations.getViolations(checkSetting.name)) {
    callEvent(new LACPunishmentEvent(event)); // already at threshold → punish directly
    return;
}
if (violations < punishmentVio) increaseViolations(checkName, 1);
// ... alerts / logs / discord / setback ...
if (punishmentVio == violations) callEvent(new LACPunishmentEvent(event)); // just reached → punish
```

So violations increment by 1 per flag until exactly `punishment-vio`; the flag that causes the counter to equal `punishment-vio` also fires `LACPunishmentEvent`. A player already at the threshold who flags again is punished without an extra increment (covers concurrent flags).

### 3. `LACPunishmentEvent` — `api/event/LACPunishmentEvent.java:11`

Carries the same `CheckSetting`/`Player`/`LACPlayer`/`Cancellable`. Has two constructors: the check-mirroring one and `LACPunishmentEvent(LACViolationEvent)` (`LACPunishmentEvent.java:29`) which copies fields from the violation event. Also `Cancellable` and gated by `api.enabled` in `ViolationHandler.onPunishment` (`ViolationHandler.java:189`).

### 4. `onPunishment` — `ViolationHandler.java:186` (priority `HIGHEST`)

Same offline/API guards. Then, in order:

1. File log (`PUNISHMENT_LOG` channel) if `log.enabled && log.log-punishments.enabled` and `tryAcquire(PUNISHMENT_LOG, now, cooldown)` succeeds.
2. Alert broadcast (`PUNISHMENT_ALERT`) if `alerts.broadcast-punishments.enabled` and cooldown acquired.
3. Discord (`PUNISHMENT_DISCORD`) if `discord-webhook.enabled && send-punishments.enabled` and cooldown acquired. `Logger.logDiscord` (`Logger.java:87`) requires the URL to start with `https://discord.com/api/webhooks/`; `429` responses are suppressed.
4. Punishment commands if `checkSetting.punishable && punishmentCommands non-empty`. Two-phase runtime:
   - **Preparation** — runs in the punishment event's player/server-owned execution context (`ViolationHandler.onPunishment` itself, which is the thread that fired `LACPunishmentEvent` via `onFlag`): live placeholders are resolved before violation reset. Each template is rendered by `PlaceholderConvertor.renderPunishmentCommand` (`PlaceholderConvertor.java:133`) — `swapAll` + `colorize(true)` + `normalizeLegacyVanillaKickTarget` + `normalizeBukkitDispatchCommandLine` — then empty results are skipped with `Skipped empty punishment command for <name> (<Title>)`. Only non-empty immutable strings are retained (`ViolationHandler.java:228`).
   - **Dispatch** — the retained strings are handed to `RuntimeCommandDispatcher.dispatchConsoleBatch` (`RuntimeCommandDispatcher.java:17`) which schedules via `Scheduler.globalThread` (`Scheduler.java:82`): on Bukkit/Spigot/classic Paper the primary server thread (`BukkitScheduler.java:12` / `Bukkit.getScheduler().runTask`), on Folia the global region scheduler (`FoliaScheduler.java:11` → `FoliaUtil.runTask` / `FoliaUtil.java:87`). The dispatch task uses `Bukkit.getConsoleSender()` as sender, iterates in YAML list order, and performs no live player reads — all `%name%`/`%ping%`/`%world%` etc. were already frozen in preparation.
   - `Bukkit.dispatchCommand(console, rendered)` return value is checked: `false` (`dispatchCommand=false`) logs `Punishment command was not handled: '<rendered>' for <name> (<Title>)` as a controlled failure and continues (`RuntimeCommandDispatcher.java:35`). `CommandException` is contained/logged as `Failed to execute punishment command '<rendered>' for <name> (<Title>): <msg>`, and the next command continues (`RuntimeCommandDispatcher.java:39`). Custom command semantics (whether `kick`/`ban`/`tempban`/`say` exists or what it accepts) remain server-defined.
5. Violations reset: `lacPlayer.violations = new PlayerViolations()` (`ViolationHandler.java:240`) — wipes all checks for that player, not just the punished one. Preparation (above) resolves live placeholders before this reset, so `%vio%` reflects the pre-reset value. The periodic `violation.reset.reset-interval` and `cache` logic (see configuration doc) applies independently.

### Cancellability summary

- When `api.enabled=true`, external plugins may cancel either `LACViolationEvent` or `LACPunishmentEvent`; `ViolationHandler` honors the cancellation and skips all downstream handling for that event. When `api.enabled=false`, `isCancelled()` is ignored and handling proceeds regardless.
- The Bukkit `Cancellable` inside the event (e.g. the original `PlayerMoveEvent`) is only touched by the setback path — `event.getCancellable().setCancelled(true)` — and only when `setback=true` and the threshold is met.

## Cooldown Channels (throttling)

`PlayerViolations.NotificationChannel` (`PlayerViolations.java:11`):

- `VIOLATION_LOG` — `log.log-violations.cooldown`
- `PUNISHMENT_LOG` — `log.log-punishments.cooldown`
- `VIOLATION_ALERT` — `alerts.broadcast-violations.cooldown`
- `PUNISHMENT_ALERT` — `alerts.broadcast-punishments.cooldown`
- `VIOLATION_DISCORD` — `discord-webhook.send-violations.cooldown`
- `PUNISHMENT_DISCORD` — `discord-webhook.send-punishments.cooldown`

`tryAcquire(channel, currentTimeMillis, cooldownMillis)` (`PlayerViolations.java:38`) CAS-loops on an `AtomicLongArray` of last-fire timestamps. Returns `true` only if `elapsed > cooldownMillis` and the CAS succeeds, then records `currentTimeMillis`. `0` means never throttled (every event fires). Cool-downs are per-player, per-check? Actually per-player per-channel (not per-check) — one array of 6 longs per `PlayerViolations` instance, so throttling applies across all checks for that player on that channel. In `ViolationHandler` each channel is acquired separately after the same `currentTime = System.currentTimeMillis()` snapshot.

## Alert Broadcast

Enabled by `alerts.broadcast-violations.enabled` / `broadcast-punishments.enabled`. On acquisition:

- `Logger.logAlert(message, checkSetting, violator, lacPlayer)` (`Logger.java:43`) colorizes with `swapAll` and sends to console, then fan-outs to online players who have `lightanticheat.alerts.notify` or `lightanticheat.alerts` and whose `LACPlayer.cache.alerts` toggle is `true` (toggled by `/light alerts`).
- The rendered chat line is `alerts.*.message`.
- Hover and click are rendered per-event in `ComponentUtil.generateLines` (`util/logger/text/ComponentUtil.java:18`):
  - Hover text = `alerts.(broadcast-violations|broadcast-punishments).on-hover`, rendered with `swapCoordinates(hover, "#0")` then `swapAll`. Empty hover disables the tooltip. Config uses `%new-line%` to split hover into multiple lines.
  - Click command = `alerts.*.on-click`, rendered by replacing `%teleport-location%` with `world x y z yaw pitch` then `swapPlayer`. Empty disables click.
  - Delivery is `VerPlayer.sendHoverMessage(player, lines, hexColorCodes)` (version-specific JSON component).

## Discord Webhook

Outer gate `discord-webhook.enabled`; inner `send-violations.enabled` / `send-punishments.enabled`. Message rendered with `swapAll`, colors stripped, wrapped as `{"content":"<message>"}` and POSTed async (`Logger.java:87`). Invalid or empty `webhook-url` is dropped silently unless it starts with `https://discord.com/api/webhooks/`. `429 Too Many Requests` errors are suppressed; other IO errors log as `WARN`/`ERROR`.

## Setback

Governed by `checks....setback.setback` (boolean) and `setback-vio` (int). Evaluated in `onFlag` only (`ViolationHandler.java:133`):

- Requires `checkSetting.setback && violations >= setbackVio && event.getCancellable() != null`.
- Movement checks have special vertical handling: `FLIGHT_A/B/C` and `SPEED_A/B/C`+`JUMP_A/B` (sets `VERTICAL_SETBACK_CHECKS` / `POST_VERTICAL_SETBACK_CHECKS`, `ViolationHandler.java:36`) when the player is airborne (`isOnGround` false, `HistoryElement.FIRST.onGround` false). In that case the handler attempts a safe vertical teleport search (up to 25 blocks down) rather than a simple `setCancelled(true)`; if inside a solid block it tries block-top and slab adjustments before falling back to stepwise descent via `FoliaUtil.teleportPlayer`. Non-vertical setbacks simply `event.getCancellable().setCancelled(true)` (freezes the action).
- Default `setback=false` for every check in the canonical config — the project does not recommend enabling it (see `config.yml` comment: it notifies cheaters and a kick is more effective).

## Punishment Commands (configurable)

Path: `checks.<type>.<group>.<group>_<letter>.punishment.commands` — `List<String>` (`ConfigManager.java:318`), `CheckSetting.punishmentCommands` (`CheckSetting.java:24`). `punishable` (`punishment.punishable`) gates execution; `punishment-vio` sets the threshold.

### Dispatch

Exact two-phase runtime (`ViolationHandler.java:227` + `RuntimeCommandDispatcher.java:17`):

- **Preparation** — runs in the punishment event's player/server-owned execution context (the thread executing `ViolationHandler.onPunishment`, i.e. the caller of `LACPunishmentEvent`). `PlaceholderConvertor.renderPunishmentCommand` (`PlaceholderConvertor.java:133`) resolves live placeholders (`swapAll`/`swapCoordinates`/`VerPlayer.getPing`/world/location) before violation reset, then `colorize(true)` + `normalizeLegacyVanillaKickTarget` + `normalizeBukkitDispatchCommandLine` (`PlaceholderConvertor.java:145` — optional leading `/` accepted and stripped once, surrounding whitespace trimmed). Null/empty/whitespace-only results are skipped with `Skipped empty punishment command for <name> (<Title>)` (`ViolationHandler.java:232`); only non-empty immutable strings survive to dispatch, so `%vio%` reflects the pre-reset counter.
- **Dispatch** — `RuntimeCommandDispatcher.dispatchConsoleBatch` schedules a single `Scheduler.globalThread` task (`Scheduler.java:82`) carrying only the prepared immutable strings (no live `Player` reads inside the task):
  - Bukkit/Spigot/classic Paper → primary server thread (`BukkitScheduler.java:12` → `Bukkit.getScheduler().runTask`).
  - Folia → global region scheduler (`FoliaScheduler.java:11` → `FoliaUtil.runTask` → `FoliaUtil.java:87` / `invokeGlobal` `globalRunMethod`).
- Sender: `Bukkit.getConsoleSender()` — commands run as console (`RuntimeCommandDispatcher.java:34`).
- Ordering: iterated in YAML list order; dispatch iterates the frozen list only.
- Empty/null guard in dispatch (`RuntimeCommandDispatcher.java:29`) also skips with the same `Skipped empty…` log and continues.
- `Bukkit.dispatchCommand` return `false` (`dispatchCommand=false`) is a controlled failure: logged as `Punishment command was not handled: '<rendered>' for <name> (<Title>).` and the next command continues (`RuntimeCommandDispatcher.java:35`). This is server-defined handler rejection, not an exception.
- `CommandException` is contained/logged as `Failed to execute punishment command '<rendered>' for <name> (<Title>): <msg>` and the next command continues (`RuntimeCommandDispatcher.java:39`).
- Custom command semantics remain server-defined: whether `kick`/`ban`/`tempban`/`say`/`minecraft:kick` exists, what arguments it accepts, and whether it targets online players is decided by the server/plugins, not by LAC. Legacy `kick *%name%` compatibility remains via `normalizeLegacyVanillaKickTarget` (see below) — do not author new commands with `*`.

### Current recommended command

All checks ship with a single command:

```
kick %name% %check%
```

Placeholders are rendered by `renderPunishmentCommand` (`PlaceholderConvertor.java:133`), so `%name%`, `%check%`, and every global placeholder are available (see table below). Add multiple commands to chain actions, e.g.:

```yaml
commands:
  - "kick %name% %check%"
  - "ban %name% 7d Cheating (%check%)"
  - "say %name% was removed for %check% (%check-description%)"
```

Ordering matters — the first command that kicks/bans may make later commands moot if the player is already offline, but they are still attempted.

### Legacy compatibility: `kick *%name%`

Older configs used `kick *%name% %check%`; the leading `*` was the 1.7-1.12 vanilla `kick` selector. At execution `normalizeLegacyVanillaKickTarget` (`PlaceholderConvertor.java:144`) detects `kick` or `minecraft:kick` whose first argument is exactly `"*" + playerName` and rewrites it to `kick <playerName> ...` (preserving the original spacing and trailing args). This is a compatibility shim, not a feature — do not write new commands with `*`:

```yaml
# bad (legacy — will be normalized, but don't rely on it)
commands: ["kick *%name% %check%"]
# good
commands: ["kick %name% %check%"]
```

Empty or whitespace-only results after rendering are skipped and logged; fix the template rather than suppressing the log.

## Placeholders

No PlaceholderAPI integration is provided. Placeholders are replaced by `PlaceholderConvertor` and a few command-specific call sites. Color codes (`&` and `&#RRGGBB`) are applied by `ColorUtil.colorize` after substitution where relevant.

### Global placeholders

Handled by `PlaceholderConvertor.swapAll` (`PlaceholderConvertor.java:43`) plus the two helpers it calls — `swapConnectionStability` and `swapCoordinates`. Available everywhere alerts, logs, Discord messages, and punishment commands are rendered, and in most command responses via `swapPlayer` / `swapSome`.

| Placeholder | Source / Type | Example | Notes |
|---|---|---|---|
| `%prefix%` | `messages.prefix` | `&8(&#FF450ELight&8)&f` | — |
| `%version%` | `plugin.yml` version | `2.5.1` | — |
| `%check%` | `CheckName.title` (no underscores) | `FlightA` | Display title |
| `%check-type%` | `CheckName.CheckType` title-cased | `Movement` | `Movement/Combat/Interaction/Packet/Inventory/Player` |
| `%check-description%` | `CheckName.description` | `Acceleration` | Per-check subtitle |
| `%vio%` | `PlayerViolations.getViolations(check)` | `21` | Current counter for that check |
| `%punishment-vio%` | `CheckSetting.punishmentVio` | `40` | Threshold that triggers punishment |
| `%setback-vio%` | `CheckSetting.setbackVio` | `21` | Threshold that triggers setback |
| `%tps%` | `TPSCalculator.getTPS()` capped 20, format `#0.00` | `19.84` | Server TPS |
| `%ping%` | `VerPlayer.getPing(player)` | `42` | Resolved via reflection / `Player.getPing` |
| `%edition%` | `FloodgateHook.isBedrockPlayer` | `Java` or `Bedrock` | Edition label |
| `%name%` | `Player.getName()` | `Notch` | — |
| `%uuid%` | `Player.getUniqueId()` | `...` | — |
| `%ip%` | `player.getAddress()` without leading `/`, or `none` | `127.0.0.1` | — |
| `%client-brand%` | `ClientBrandRecognizer.getClientBrand` | `vanilla` | Custom payload brand |
| `%connection-stability%` | `ConnectionStabilityListener.getConnectionStability` | `&ahigh&r` etc. | Expands to the matching `messages.command-messages.ping.connection-stability.{high,medium,low}` string, so it may itself contain color codes |
| `%world%` | `player.getWorld().getName()` (via `AsyncUtil.getWorld` fallback) | `world` | From `swapCoordinates` |
| `%x%` | `location.getX()` format `#0.00` | `123.46` | From `swapCoordinates` |
| `%y%` | `location.getY()` format `#0.00` | `64.00` | — |
| `%z%` | `location.getZ()` format `#0.00` | `-45.12` | — |
| `%date-sec%` | `yyyy-MM-dd HH:mm:ss` | `2026-05-11 14:03:22` | Wall clock at render time |
| `%date-min%` | `yyyy-MM-dd HH:mm` | `2026-05-11 14:03` | — |
| `%date-hrs%` | `yyyy-MM-dd HH` | `2026-05-11 14` | — |
| `%date-day%` | `yyyy-MM-dd` | `2026-05-11` | Also used in `log.file` path (`swapSome(DATA)`) |

`swapPlayer` (`PlaceholderConvertor.java:121`) provides `%ping% %name% %uuid% %ip% %client-brand% %connection-stability% %world% %x% %y% %z%` for command contexts that have a target player but no `CheckSetting`. `swapSome(DATA)` (`PlaceholderConvertor.java:84`) is the minimal set for non-check messages: `%prefix%` `%version%` `%tps%` `%date-sec%` `%date-min%` `%date-hrs%` `%date-day%`.

### Local placeholders

Only available in the specific message noted by the config comment and the call site that replaces them. Using them elsewhere leaves them literal.

| Placeholder | Where | Call site | Description |
|---|---|---|---|
| `%time%` | `messages.command-messages.reload.message` | `LACCommand.java:108` | Milliseconds took to reload (`System.currentTimeMillis()-startTime`) |
| `%cps%` | `messages.command-messages.cps.message` | `LACCommand.java:92` | Clicks per second from `CPSListener.getCps(player)` |
| `%checks%` | `messages.command-messages.checks.message` | `LACCommand.java:134` | Comma-separated `§a`/`§c Title` list of all checks (enabled green, disabled red) |
| `%movement_checks%` | same | `LACCommand.java:136` | Same filtered to type `MOVEMENT`; one placeholder per `CheckType` |
| `%combat_checks%` | same | — | `COMBAT` |
| `%interaction_checks%` | same | — | `INTERACTION` |
| `%packet_checks%` | same | — | `PACKET` |
| `%inventory_checks%` | same | — | `INVENTORY` |
| `%player_checks%` | same | — | `PLAYER` (covers `Autobot_A`, `SkinBlinker_A`) |
| `%latest-version%` | `update-checker.notification.console.message` and `on-join.message` | `Updater.java:55,110` | Version fetched from Spigot API |
| `%usage%` | `messages.error-messages.invalid-format` | `LACCommand.java:38` | Command usage string (`"/<label> <subcommand> <args>"`) |
| `%teleport-location%` | `alerts.broadcast-violations.on-click` and `broadcast-punishments.on-click` | `ComponentUtil.java:43` | `world x y z yaw pitch` (raw doubles/floats) — only valid in the click command template |
| `%new-line%` | `alerts.*.on-hover` | config literal, split by version-specific hover renderer (`VerUtil.multiVersion.sendHoverMessage`) | Line separator in hover tooltip; not replaced by `PlaceholderConvertor` — do not use in other messages |
| `%client%` | `messages.command-messages.client.message` | `LACCommand.java:69` | Legacy alias for `%client-brand%` — preserved for backward compatibility; prefer `%client-brand%` |

Additional local-ish placeholders supplied via `swapSome` / `swapPlayer` in command handlers:

- `%tps%`, `%prefix%`, `%version%` are also injected by `swapSome(PREFIX, VERSION, TPS)` into reload/help/tps/checks/error messages even when not listed as "local" in the config comment.
- `%name%`, `%ping%`, `%connection-stability%`, `%client-brand%` etc. are injected by `swapPlayer` into `ping` and `cps` responses.

Do not invent placeholders. In particular, PlaceholderAPI `%player_name%`-style expansions are not wired — only the `%…%` keys above are substituted.
