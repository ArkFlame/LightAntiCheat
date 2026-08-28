# Configuration

Source of truth: `src/main/resources/config.yml` is the canonical default bundled in the JAR and copied on first run via `Main.saveDefaultConfig()`. All key paths, types, and defaults below reflect that file at the current commit. `recommended-config.yml` at the repository root is a non-canonical, stricter tuning reference — see comparison section.

## Canonical vs Recommended

| Aspect | `src/main/resources/config.yml` (canonical) | `recommended-config.yml` |
|---|---|---|
| Purpose | Shipped default; permissive, lets operators tighten gradually | Opinionated production tuning offered by the project |
| Alert/file cooldowns | `alerts.*.cooldown=0`, `log.log-violations.cooldown=1000`, `log-punishments=0`, `discord.send-violations=2000` | `alerts` both `1000`, same log values, same discord |
| Permissions | `permission.per-check-bypass-permission=false` | `true` |
| Violation windows | `reset-interval=180`, `cache-duration=180` | `240` / `120` |
| Lag protection | `tick-threshold=1000`, `ignore-time-on-join=0`, `ignore-time-on-teleport=0` | `750` / `3000` / `3000` |
| Geyser | `geyser-hook.bedrock-only=true` present | absent (implicit `false`) — per-check `detection.bedrock=false` instead |
| Detection thresholds | `min-tps=5.0`, `max-ping=10000` (except `autotool_a=400`), `java=true bedrock=true` | `min-tps=18.0`, `max-ping=300` (`autotool_a=400`), `java=true bedrock=false`; `punishment-vio`/`setback-vio` ~1.5x higher |
| Update checker | all `false` | identical |

Use the canonical file as the merge base. If you want the recommended profile, apply its deltas manually; do not replace `config.yml` wholesale on a live server without re-checking `geyser-hook.bedrock-only` semantics.

## Config Updater (high level)

`ConfigManager.loadConfig()` — `ConfigManager.java:202` — calls `instance.saveDefaultConfig()` then `ConfigUpdater.update(instance, "config.yml", new File(dataFolder,"config.yml"), emptyList)` (`ConfigManager.java:207`). `com.tchristofferson.configupdater` merges missing keys from the bundled resource into the on-disk file while preserving user values and comments. Failures log `config.yml is invalid! Something went wrong while updating the file!` and loading continues with whatever `FileConfiguration` Bukkit provides. Field-to-path mapping is reflective: `ConfigManager.Config.Inner.fieldName` becomes `kebab-case` path by inserting `-` before each post-dot uppercase letter (`ConfigManager.java:219`).

## Reload and Transactional `listener-mode`

`/light reload` calls `ConfigManager.reloadConfig()` (`ConfigManager.java:253`):

1. `instance.reloadConfig()` + `loadConfig()` re-reads YAML and repopulates `Config.*` statics.
2. `LACInputMode.parse(Config.listenerMode)` (`input/model/LACInputMode.java:10`) — accepts only `packet` or `nms` (case-insensitive, trimmed). Any other value is invalid.
3. If `parse` is empty, the engine is left untouched and an error is logged: `Invalid listener-mode: '<raw>'! Keeping previous mode '<before>'.` (`ConfigManager.java:269`).
4. If valid and different from `engine.getActiveMode()`, `engine.reconfigure(target)` (`input/LACInputEngine.java:113`) is called. It eagerly starts the target provider (`ensurePacketProvider` / `ensureNmsProvider`) before bumping `inputEpoch` and swapping `activeMode`. If startup throws `Exception` or `LinkageError`, the exception is caught, logged as `Failed to reconfigure listener-mode to '<target>': <msg> Keeping previous mode '<before>'.`, and the previous mode remains active. `reconfigure` is transactional: the old provider is never stopped unless the new one started successfully. Only `NMS->PACKET` closes the NMS provider; `PACKET->NMS` keeps the packet provider warm.
5. Per-check sections are reloaded via `loadCheck` and each listener is re-registered (`Check.registerListener`).

Caveat: editing `listener-mode` to an invalid value or to `nms` on an environment missing NMS support will fail closed — no restart required, but check the console error.

## Top-level Domains

### `enabled` — `boolean`, default `true`

Master kill-switch. When `false`, the plugin disables itself (commands respond `This plugin is disabled!`). All checks, alerts, logs, and hooks are skipped.

### `listener-mode` — `string`, default `"packet"`

Selects the input engine. Enum `LACInputMode` (`input/model/LACInputMode.java:6`): `PACKET` (PacketEvents) and `NMS` (net.minecraft.server injection). Values are case-insensitive; whitespace trimmed. Invalid values are rejected on reload as described above. On first start, `activateInitialMode` throws `IllegalStateException` if the requested provider cannot start.

### `messages` — section

| Key | Type | Default | Effect | Caveat |
|---|---|---|---|---|
| `messages.prefix` | string | `&8(&#FF450ELight&8)&f` | Interpolated wherever `%prefix%` appears | Supports `&#RRGGBB` hex when `hex-color-codes=true` and the platform supports it |
| `messages.hex-color-codes` | boolean | `true` | When `true`, `&#RRGGBB` is resolved via `ChatColor.of()` on 1.16+; otherwise mapped to nearest `ChatColor` (`ColorUtil.java:70`) | On 1.13-1.15 hex degrades to legacy palette; on console `logAlert` colorizes with `customColor=false` |
| `messages.command-messages.help.message` | string | `%prefix% LightAntiCheat %version% is running` | Fallback for unknown subcommand | Globals `%prefix% %version% %tps%` via `swapSome` |
| `messages.command-messages.reload.message` | string | `%prefix% Successfully reloaded the config &7(%time%ms)` | Sent after `/light reload` | Local `%time%` = wall ms for reload |
| `messages.command-messages.alerts.toggled-on-message` | string | `%prefix% Alerts are enabled!` | Toggle feedback | — |
| `messages.command-messages.alerts.toggled-off-message` | string | `%prefix% Alerts are disabled!` | Toggle feedback | — |
| `messages.command-messages.tps.message` | string | `%prefix% TPS from the last 30 seconds: &f%tps%` | `/light tps` | Local `%tps%` via `swapSome` |
| `messages.command-messages.ping.message` | string | `%prefix% %name%&7's ping is &f%ping% &7(&f%connection-stability%&7 stability)` | `/light ping <player>` | Globals plus stability-high/medium/low strings below |
| `messages.command-messages.ping.connection-stability.high` | string | `&ahigh&r` | Rendered for `%connection-stability%` when HIGH | — |
| `messages.command-messages.ping.connection-stability.medium` | string | `&emedium&r` | — | — |
| `messages.command-messages.ping.connection-stability.low` | string | `&clow&r` | — | — |
| `messages.command-messages.client.message` | string | `%prefix% %name%&7's client is &f%client-brand%` *(note: command handler `LACCommand.java:69` also replaces legacy placeholder `%client%` with the same value — both forms work here but prefer `%client-brand%`)* | `/light client <player>` | Uses `swapPlayer` for `%name% %ping% %uuid% %ip% %client-brand%` |
| `messages.command-messages.cps.message` | string | `%prefix% %name%&7's CPS is &f%cps%` | `/light cps <player>` | Local `%cps%` from `CPSListener.getCps` |
| `messages.command-messages.checks.message` | string | `%prefix% Checks: %checks%` | `/light checks` | Locals `%checks%` plus per-type `%movement_checks%` `%combat_checks%` `%interaction_checks%` `%inventory_checks%` `%packet_checks%` `%player_checks%` |
| `messages.error-messages.no-permission` | string | `%prefix% You don't have enough permissions` | Permission denial | — |
| `messages.error-messages.invalid-format` | string | `%prefix% Usage: &7%usage%` | Bad syntax | Local `%usage%` |

### `alerts` — section

| Key | Type | Default | Effect | Caveat |
|---|---|---|---|---|
| `alerts.broadcast-violations.enabled` | boolean | `true` | Whether violation alerts fire | Gated by `tryAcquire(VIOLATION_ALERT, ...)` cooldown |
| `alerts.broadcast-violations.message` | string | `%prefix% %name% was flagged for %check% (%vio%/%punishment-vio%)` | Chat line; hover/click via `ComponentUtil.generateLines` → `Logger.logAlert` | Colorized with `hexColorCodes`; sent to console + players with `lightanticheat.alerts.notify` or `lightanticheat.alerts` and `LACPlayer.cache.alerts==true` |
| `alerts.broadcast-violations.cooldown` | int (ms) | `0` | Per-player per-channel throttle | `0` means every flag broadcasts (noisy). Recommended `1000` |
| `alerts.broadcast-violations.on-hover` | string | `&7Check: &#FF450E%check% (%check-description%)%new-line%&7World: &#FF450E%world%%new-line%&7Cords: &#FF450E%x% %y% %z%%new-line%&7Client: &#FF450E%client-brand%` | Hover tooltip lines | `%new-line%` splits lines; empty string disables hover |
| `alerts.broadcast-violations.on-click` | string | `/light teleport %teleport-location%` | Click command | Local `%teleport-location%` = `world x y z yaw pitch`; empty disables |
| `alerts.broadcast-punishments.*` | same | `true` / `%prefix% %name% was punished for %check%` / `0` | Same for punishments, channel `PUNISHMENT_ALERT` | Same hover/click semantics |

### `log` — section

| Key | Type | Default | Effect | Caveat |
|---|---|---|---|---|
| `log.enabled` | boolean | `true` | Master file-log gate | When `false`, neither violations nor punishments are written |
| `log.file` | string (path) | `logs/%date-day%.log` | Daily-rotated file under `plugins/LightAntiCheat/` | `%date-day%` (and other `%date-*%`) via `swapSome(DATA)` at write time; directories created on demand |
| `log.log-violations.enabled` | boolean | `true` | Per-event gate | Channel `VIOLATION_LOG` |
| `log.log-violations.message` | string | `(%date-sec%) %name% was flagged [Check: %check% (%check-description%)] [Violations: %vio%/%punishment-vio%] [TPS: %tps%] [Ping: %ping%ms (%connection-stability% stability)] [Client: %client-brand%] [Location: %world% %x% %y% %z%]` | One line per throttled flag | `Logger.logFile` strips colors and appends async |
| `log.log-violations.cooldown` | int (ms) | `1000` | Throttle | — |
| `log.log-punishments.enabled` | boolean | `true` | — | Channel `PUNISHMENT_LOG` |
| `log.log-punishments.message` | string | same with `was punished` | — | — |
| `log.log-punishments.cooldown` | int (ms) | `0` | — | — |

### `discord-webhook` — section

| Key | Type | Default | Effect | Caveat |
|---|---|---|---|---|
| `discord-webhook.enabled` | boolean | `false` | Master webhook gate | Outer check in `ViolationHandler` before per-type checks |
| `discord-webhook.send-violations.enabled` | boolean | `false` | Whether violation posts | Channel `VIOLATION_DISCORD` |
| `discord-webhook.send-violations.webhook-url` | string | `""` | Discord webhook URL | Must start with `https://discord.com/api/webhooks/` or `Logger.logDiscord` silently drops (`Logger.java:90`) |
| `discord-webhook.send-violations.message` | string | `` ```%name% was flagged for %check% (%check-description%) (%vio%/%punishment-vio%)``` `` | JSON `content` | Colors stripped |
| `discord-webhook.send-violations.cooldown` | int (ms) | `2000` | Throttle per player | `429` errors from Discord are suppressed |
| `discord-webhook.send-punishments.*` | same | `false` / `""` / `` ```%name% was punished for %check% (%check-description%)``` `` / `0` | Same for punishments | Distinct URL allowed |

### `permission` — section

| Key | Type | Default | Effect | Caveat |
|---|---|---|---|---|
| `permission.per-check-bypass-permission` | boolean | `false` | When `true`, `lightanticheat.bypass.<check>` (e.g. `lightanticheat.bypass.flight_a`, derived in `CheckSetting.java:11`) is honored | Recommended enables this |
| `permission.disable-all-bypass-permissions` | boolean | `false` | When `true`, all bypass checks are ignored regardless of the above | Takes precedence |

### `violation` — section

| Key | Type | Default | Effect | Caveat |
|---|---|---|---|---|
| `violation.reset.reset-interval` | int (seconds) | `180` | Global periodic reset of per-player violation counters | Higher = less forgiving |
| `violation.cache.enabled` | boolean | `true` | Keep violations in memory after quit | — |
| `violation.cache.cache-duration` | int (seconds) | `180` | How long after quit to retain; `0` means wipe on quit | — |

### `lag-protection` — section

| Key | Type | Default | Effect | Caveat |
|---|---|---|---|---|
| `lag-protection.tick-threshold` | int (ms) | `1000` | If server hasn't ticked within this window, checks are skipped | Canonical `1000` is lenient; recommended `750` |
| `lag-protection.ignore-time-on-join` | int (ms) | `0` | Suppress checks for this long after join | Recommended `3000` |
| `lag-protection.ignore-time-on-teleport` | int (ms) | `0` | Suppress after teleport into unloaded chunk | Recommended `3000` |
| `lag-protection.prevent-entering-into-unloaded-chucks` | boolean | `true` | Cancels gliding/riptiding entry into unloaded chunks | Typo in key (`chucks`) is intentional — do not rename |
| `lag-protection.prioritize-accuracy` | boolean | `true` | Extra correctness work when laggy (slightly more expensive) | — |

### `geyser-hook` — section

Detects Bedrock players via Floodgate, UUID prefix `000000`, or name prefix `.`.

| Key | Type | Default | Effect | Caveat |
|---|---|---|---|---|
| `geyser-hook.enabled` | boolean | `true` | Whether any Geyser detection runs | When `false`, all detection helpers return `false` → everyone treated as Java |
| `geyser-hook.bedrock-only` | boolean | `true` (canonical only) | When `true`, the detection layer globally skips Java players and only checks Bedrock/Geyser players, leaving per-check `detection.java`/`detection.bedrock` intact — avoids editing every check | If your server has no filtering, set `false` or switch to per-check `bedrock=false`. Recommended profile omits this key and relies on per-check `bedrock=false` instead. |
| `geyser-hook.floodgate.enabled` | boolean | `true` | Treat Floodgate API 2.0+ Bedrock players as Bedrock | Requires Floodgate presence |
| `geyser-hook.uuid.enabled` | boolean | `true` | UUID starting with `000000` counts as Bedrock | — |
| `geyser-hook.prefix.enabled` | boolean | `true` | Name starting with prefix counts as Bedrock | — |
| `geyser-hook.prefix.prefix-string` | string | `"."` | The prefix | — |

Note: `bedrock-only` advice in `config.yml:137-143` is canonical. Keep it unless external linking uses a local DB.

### `update-checker` — section

| Key | Type | Default | Effect | Caveat |
|---|---|---|---|---|
| `update-checker.enabled` | boolean | `false` | Enables background fetch of latest version from Spigot API | Polls every 30 min (`Updater.java:35`) |
| `update-checker.notification.console.enabled` | boolean | `false` | Log to console when newer version exists | Local `%latest-version%` |
| `update-checker.notification.console.message` | string | `%prefix% LightAntiCheat %latest-version% is available!` | — | — |
| `update-checker.notification.on-join.enabled` | boolean | `false` | Notify joiners | — |
| `update-checker.notification.on-join.message` | string | same | — | — |
| `update-checker.notification.on-join.require-permission` | boolean | `true` | Only notify players with `lightanticheat.alerts.notify` | — |

### `bstats` — section

| Key | Type | Default | Effect | Caveat |
|---|---|---|---|---|
| `bstats.enabled` | boolean | `true` | Anonymous metrics via bStats (`Metrics` id) | No gameplay effect; disable only if policy requires |

### `api` — section

| Key | Type | Default | Effect | Caveat |
|---|---|---|---|---|
| `api.enabled` | boolean | `true` | Whether the developer API events are cancellable by external plugins | When `false`, `LACViolationEvent`/`LACPunishmentEvent` cancellations are ignored (`ViolationHandler.java:87,189`). Leave `true` unless debugging plugin conflicts. |

### `checks` — section

One subtree per `CheckName` (`check/CheckName.java:3`). The path convention is:

```
checks.<type>.<group>.<group>_<letter>.enabled
checks.<type>.<group>.<group>_<letter>.punishment.punishable
checks.<type>.<group>.<group>_<letter>.punishment.punishment-vio
checks.<type>.<group>.<group>_<letter>.punishment.commands
checks.<type>.<group>.<group>_<letter>.setback.setback
checks.<type>.<group>.<group>_<letter>.setback.setback-vio
checks.<type>.<group>.<group>_<letter>.detection.min-tps
checks.<type>.<group>.<group>_<letter>.detection.max-ping
checks.<type>.<group>.<group>_<letter>.detection.java
checks.<type>.<group>.<group>_<letter>.detection.bedrock
```

`<type>` is `CheckName.CheckType` lowercased (`movement`, `combat`, `interaction`, `packet`, `inventory`, `player`). `<group>` is the title prefix before `_` lowercased (`flight`, `speed`, `killaura`, `fastbreak`, ...). `<letter>` is the suffix lowercased (`a`, `b`, `c`, ...). Example: `FLIGHT_A` → `checks.movement.flight.flight_a.*`; `KILLAURA_C` → `checks.combat.killaura.killaura_c.*`. Loading is in `ConfigManager.loadCheck` (`ConfigManager.java:297`).

Per-field schema:

| Suffix | Type | Canonical Default | Effect | Caveat |
|---|---|---|---|---|
| `enabled` | boolean | `true` (all checks) | Whether the check registers and runs | `CheckSetting.enabled` (`CheckSetting.java:15`) |
| `punishment.punishable` | boolean | `true` | Whether reaching `punishment-vio` dispatches commands | If `false`, punishments still broadcast/log but no commands run |
| `punishment.punishment-vio` | int | varies (see inventory below) | Violations threshold that triggers `LACPunishmentEvent` → console commands | Per-check tuning; recommended raises most by ~50% |
| `punishment.commands` | list<string> | `["kick %name% %check%"]` (all checks) | Commands dispatched via console on punishment, in order | See punishments doc for placeholders and `*name` normalization |
| `setback.setback` | boolean | `false` (all checks) | When `true`, the offending action is cancelled once `setback-vio` is reached | Not recommended: notifies cheaters and is less effective than a kick; see `ViolationHandler.java:133` |
| `setback.setback-vio` | int | roughly `punishment-vio * 0.5` (e.g. `flight_a 21/40`) | Violations at which to start cancelling | Ignored when `setback=false` |
| `detection.min-tps` | double | `5.0` | Below this TPS the check is skipped for that flag | Recommended `18.0` |
| `detection.max-ping` | int (0-10000) | `10000` (except `autotool_a 400`) | Above this ping the check is skipped | Recommended `300` (400 for autotool) |
| `detection.java` | boolean | `true` | Whether Java players are checked | Respects `geyser-hook.bedrock-only` global override |
| `detection.bedrock` | boolean | `true` (canonical) / `false` (recommended) | Whether Geyser/Bedrock players are checked | Pair with `geyser-hook.bedrock-only` discussion above |

Canonical per-check defaults (punishment-vio / setback-vio):

- Movement: `flight_{a,b,c} 40/21`, `speed_a 30/16` `speed_b 35/18` `speed_c 40/21` `speed_d 30/16` `speed_e 30/16` `speed_f 50/26`, `nofall_a 20/17` `nofall_b 25/22`, `jump_a 20/7` `jump_b 20/11`, `liquidwalk_{a,b} 40/21`, `fastclimb_a 25/13`, `noslow_a 25/13`, `step_a 3/4`, `boat_a 35/18`, `vehicle_a 35/18`, `elytra_{a,b} 40/21` `elytra_c 12/10`, `trident_a 3/7`
- Combat: `killaura_a 6/4` `killaura_b 8/5` `killaura_c 4/3` `killaura_d 5/1`, `reach_a 10/6` `reach_b 6/4`, `criticals_a 1/1` `criticals_b 3/1`, `autoclicker_{a,b} 3/2`, `velocity_a 12/7`
- Interaction: `airplace_a 3/2`, `fastplace_a 3/2`, `blockplace_a 10/6` `blockplace_b 5/3`, `ghostbreak_a 3/2`, `fastbreak_a 15/8`, `autotool_a 8/5`, `blockbreak_a 10/6` `blockbreak_b 5/3`, `scaffold_{a,b} 10/6`
- Packet: `morepackets_{a,b} 2/1`, `timer_{a,b} 3/2`, `badpackets_a 5/4` `badpackets_b 20/19` `badpackets_c 15/14` `badpackets_d 10/9`
- Inventory: `sorting_a 3/2`, `itemswap_a 4/3`
- Player: `autobot_a 3/2`, `skinblinker_a 2/1`

Do not paste the full 1100-line tree here; the defaults above are the complete canonical set — edit only the checks you intend to tune and leave the rest managed by the updater.

## Editing Guidance

- Prefer small, typed edits via your `config.yml` rather than bulk copy from `recommended-config.yml`. After any edit, run `/light reload` and check the console for the two transactional error paths noted above.
- Keep `log.file` under `plugins/LightAntiCheat/` to stay within the plugin data folder.
- If you enable Discord webhooks, set both `discord-webhook.enabled` and the per-type `send-*.enabled` plus a valid `webhook-url` prefixed `https://discord.com/api/webhooks/`. Throttle `cooldown` to avoid 429s.
- For Geyser servers, decide one strategy: either `geyser-hook.bedrock-only=true` (global) or per-check `detection.bedrock=false` (recommended). Using both is legal but the global flag wins.
