# LightAntiCheat Troubleshooting

## PacketEvents in `packet` mode

`listener-mode` (`config.yml: listener-mode`) selects the input provider. Set `listener-mode: "packet"` to use `PacketEventsInputProvider` (`src/main/java/me/vekster/lightanticheat/input/provider/packetevents/PacketEventsInputProvider.java:66`), `listener-mode: "nms"` to use the built-in NMS provider via `LACInputEngine` (`src/main/java/me/vekster/lightanticheat/input/LACInputEngine.java:51`).

### When you see these errors (packet mode)

These are thrown from `PacketEventsInputProvider.start()` (`src/main/java/me/vekster/lightanticheat/input/provider/packetevents/PacketEventsInputProvider.java:69`):

- `PacketEvents plugin not found.` — No plugin named `packetevents` or `PacketEvents` is installed. Install PacketEvents or switch to `nms`.
- `PacketEvents plugin is installed but not enabled.` — The dependency exists but is not enabled. Check startup order and that PacketEvents enables without error before LightAntiCheat.
- `PacketEvents API is null after PacketEvents plugin enable.` — `PacketEvents.getAPI()` returned null even though the plugin is enabled. Usually means version mismatch or PacketEvents failed to initialize internally.
- `PacketEvents API is not loaded.` — `api.isLoaded() == false`.
- `PacketEvents API is not initialized.` — `api.isInitialized() == false`. PacketEvents is present but not initialized yet. Ensure it loads early and its own config is valid.
- `PacketEvents API is terminated.` — `api.isTerminated() == true`. PacketEvents was terminated/reloaded and must be restarted with the server process.
- `Failed to register LightAntiCheat PacketEvents listener: <reason>` — `api.getEventManager().registerListener(...)` threw. `started` stays `false` and the provider is left unregistered (`src/main/java/me/vekster/lightanticheat/input/provider/packetevents/PacketEventsInputProvider.java:125`).

Log-only (during provider reconfiguration/close):

- `(LightAntiCheat-Plus) Failed to unregister stale PacketEvents listener: <reason>` (`src/main/java/me/vekster/lightanticheat/input/provider/packetevents/PacketEventsInputProvider.java:95`)
- `(LightAntiCheat-Plus) Failed to unregister PacketEvents listener: <reason>` (`src/main/java/me/vekster/lightanticheat/input/provider/packetevents/PacketEventsInputProvider.java:152`)

Startup wrapper in `Main.onEnable()` (`src/main/java/me/vekster/lightanticheat/Main.java:102`) and `LACInputEngine.activateInitialMode()` (`src/main/java/me/vekster/lightanticheat/input/LACInputEngine.java:72`):

- `(LightAntiCheat) Failed to start listener-mode 'packet': <cause>` — the engine wraps any exception/LinkageError from provider `start()`, logs it, and disables the plugin. Fix the underlying PacketEvents error above and restart.

### Choosing `nms` mode

Use `nms` when you cannot or do not want to run PacketEvents:

- Set in `config.yml`:

  ```yaml
  listener-mode: "nms"
  ```

- Valid values are exactly `packet` or `nms` (case-insensitive, trimmed) — see `LACInputMode.parse()` (`src/main/java/me/vekster/lightanticheat/input/model/LACInputMode.java:10`).
- `nms` does not require the `packetevents` plugin. If `nms` fails to start, the same `Failed to start listener-mode 'nms': <cause>` path applies.
- After editing `listener-mode`, do a full server restart for a clean diagnostic baseline (see hot-reload caveat below). `/lac reload` can switch modes transactionally at runtime but restart is the authoritative check.

## Invalid `listener-mode` handling

On first enable (`src/main/java/me/vekster/lightanticheat/Main.java:96`):

```
(LightAntiCheat) Invalid listener-mode: '<value>'! Accepted values: [packet, nms]. Disabling plugin.
```

The plugin disables itself. Fix `config.yml: listener-mode` and restart the process.

On `/lac reload` (`src/main/java/me/vekster/lightanticheat/util/config/ConfigManager.java:253`):

```
(LightAntiCheat-Plus) Invalid listener-mode: '<value>'! Keeping previous mode '<before>'.
```

`<before>` is the engine's active mode at reload time (`LACInputEngine.getActiveMode()`). The old healthy provider keeps running; no switch occurs.

## Config reload transactional provider switch

`ConfigManager.reloadConfig()` reconfigures the input engine before re-registering checks (`src/main/java/me/vekster/lightanticheat/util/config/ConfigManager.java:257`):

```java
engine.reconfigure(target);
```

- If `target == before`, no action.
- If `target != before` and the new provider fails to start, this is logged:

  ```
  (LightAntiCheat-Plus) Failed to reconfigure listener-mode to '<target>': <reason> Keeping previous mode '<before>'.
  ```

  The previous provider (packet or NMS) remains active. The reload does not leave the engine without a provider. Fix the underlying failure (e.g., PacketEvents not initialized — see above), then retry `/lac reload` or restart.

## Vanilla `kick` — `Invalid name or UUID` and legacy `*%name%`

### Failure you may see

Vanilla `kick` dispatched as:

```
kick *Sinsajox FlightB
```

can fail with:

```
Invalid name or UUID
```

`*Sinsajox` is not a valid player selector/username for the vanilla `kick` command. On some versions/implementations the `*`-prefixed name triggers `CommandException` inside `Bukkit.dispatchCommand`, which LightAntiCheat logs as a punishment failure (see next section). The symptom is that detections fire and alerts/log entries appear but the kick does not land.

### What LightAntiCheat does

`PlaceholderConvertor.renderPunishmentCommand()` (`src/main/java/me/vekster/lightanticheat/util/config/placeholder/PlaceholderConvertor.java:133`) calls `normalizeLegacyVanillaKickTarget()` (`src/main/java/me/vekster/lightanticheat/util/config/placeholder/PlaceholderConvertor.java:144`):

- Applies only when the first token is `kick` or `minecraft:kick` (case-insensitive) and the first argument is exactly `*<playerName>`.
- Rewrites `*PlayerName` to `PlayerName`, preserving the rest of the command:

  ```
  "kick *%name% %check%" -> rendered -> "kick Sinsajox FlightB" -> normalized -> "kick Sinsajox FlightB"
  ```

  The check (`src/main/java/me/vekster/lightanticheat/util/config/placeholder/PlaceholderConvertor.java:181`) is intentionally narrow; commands like `ban`, `tempban`, or `kick "Player With Spaces"` are not rewritten.

### Recommended fix

Do not rely on normalization. Update every check's punishment commands in `config.yml` to the supported form:

```yaml
punishment:
  commands:
    - "kick %name% %check%"
```

Replace any occurrence of:

```yaml
- "kick *%name% %check%"
- "minecraft:kick *%name% %check%"
- "kick *%name%"
```

with the form without `*`. Any command that still uses `*%name%` will be silently rewritten for `kick`/`minecraft:kick` only; other commands with `*` will be dispatched as-is and may fail.

## Punishment command failure log

Punishments are prepared and dispatched in `ViolationHandler.onPunishment()` (`src/main/java/me/vekster/lightanticheat/util/violation/ViolationHandler.java:186`) via `RuntimeCommandDispatcher.dispatchConsoleBatch()` (`src/main/java/me/vekster/lightanticheat/util/command/RuntimeCommandDispatcher.java:17`) and `Scheduler.globalThread()` (`src/main/java/me/vekster/lightanticheat/util/scheduler/Scheduler.java:82`):

- **Preparation** runs in the punishment event's player/server-owned execution context (the thread executing `onPunishment` itself). Each template is rendered by `PlaceholderConvertor.renderPunishmentCommand()` (`src/main/java/me/vekster/lightanticheat/util/config/placeholder/PlaceholderConvertor.java:133` — `swapAll` + `colorize(true)` + `normalizeLegacyVanillaKickTarget` + `normalizeBukkitDispatchCommandLine`). An optional leading `/` is accepted and stripped once with surrounding whitespace trimmed (`PlaceholderConvertor.java:145`); legacy `kick *%name%` compatibility remains via `normalizeLegacyVanillaKickTarget`. Live placeholders are resolved before violation reset, so `%vio%` reflects the pre-reset counter. Empty/null/whitespace-only results are skipped with `Skipped empty punishment command for <player> (<CheckTitle>)` (`ViolationHandler.java:232` and the dispatch guard at `RuntimeCommandDispatcher.java:29`) and the next command continues. Custom command semantics (whether `kick`/`ban`/etc. exists or what it accepts) remain server-defined.
- **Dispatch** carries only the prepared immutable strings — no live `Player` reads in the dispatch task. `Scheduler.globalThread` routes to `Bukkit/Spigot/classic Paper -> primary server thread` (`BukkitScheduler.java:12` / `Bukkit.getScheduler().runTask`) or `Folia -> global region scheduler` (`FoliaScheduler.java:11` -> `FoliaUtil.runTask` / `FoliaUtil.java:87`). The console sender `Bukkit.getConsoleSender()` executes `Bukkit.dispatchCommand(console, rendered)` in YAML list order.

Three outcomes are emitted via `Logger.logConsole(LogType.ERROR, ...)` (`RuntimeCommandDispatcher.java:29`):

- Empty rendered command (after `swapAll`/`colorize`/`normalizeLegacyVanillaKickTarget`/`normalizeBukkitDispatchCommandLine`):

  ```
  (LightAntiCheat-Plus) Skipped empty punishment command for <player> (<CheckTitle>)
  ```

- `dispatchCommand=false` (controlled failure — server had no handler for that command):

  ```
  (LightAntiCheat-Plus) Punishment command was not handled: '<rendered>' for <player> (<CheckTitle>).
  ```

  The next command continues (`RuntimeCommandDispatcher.java:35`).

- `CommandException` from `Bukkit.dispatchCommand()` (contained/logged, next command continues):

  ```
  (LightAntiCheat-Plus) Failed to execute punishment command '<rendered>' for <player> (<CheckTitle>): <exception message>
  ```

  Example:

  ```
  (LightAntiCheat-Plus) Failed to execute punishment command 'kick *Sinsajox FlightB' for Sinsajox (FlightB): <message>
  ```

Diagnostics for this path:

- Check `config.yml: checks.<type>.<group>.<check>.punishment.commands` for the rendered form — expand `%name%`, `%check%`, `%uuid%`, etc. (`PlaceholderConvertor.swapAll()` at `src/main/java/me/vekster/lightanticheat/util/config/placeholder/PlaceholderConvertor.java:43`). Remember a single leading `/` is stripped (`/kick %name%` -> `kick %name%`); legacy `*%name%` on `kick`/`minecraft:kick` is normalized but do not author new commands with `*`.
- Run the rendered command manually from console. If it fails there, fix the command or the target selector. If it logs `was not handled`, the server has no registered handler for that command label — this is server-defined, not an LAC bug.
- If the failure is `Invalid name or UUID`, see the previous section.

## Alerts / log file / webhook — no output

All three channels are gated by config and per-player/cooldown state. The checks run in `ViolationHandler.onFlag()` and `onPunishment()` (`src/main/java/me/vekster/lightanticheat/util/violation/ViolationHandler.java:83`):

### Alerts (in-game)

Keys (`src/main/java/me/vekster/lightanticheat/util/config/ConfigManager.java:80`):

- `alerts.broadcast-violations.enabled` and `alerts.broadcast-punishments.enabled`
- Message: `alerts.broadcast-violations.message`, `alerts.broadcast-punishments.message`
- Cooldown: `alerts.broadcast-violations.cooldown`, `alerts.broadcast-punishments.cooldown` (per `PlayerViolations.NotificationChannel`)

Dispatch (`src/main/java/me/vekster/lightanticheat/util/logger/Logger.java:43`):

- Console always receives the rendered line (`Bukkit.getConsoleSender().sendMessage(...)`).
- Players receive only if they have `lightanticheat.alerts.notify` or `lightanticheat.alerts` and have alerts toggled on (`LACPlayer.cache.alerts`, toggled via `/lac alerts`).

Check:

- Verify `enabled: true`, and that your test account has the permission and alerts toggled on.
- Set `cooldown: 0` when testing; otherwise `tryAcquire(..., cooldown)` suppresses repeats within the window.
- Look at console output — if console shows the alert but players do not, it is a permission/toggle/cooldown issue.

### Log file

Keys (`src/main/java/me/vekster/lightanticheat/util/config/ConfigManager.java:99`):

- `log.enabled` (global gate)
- `log.file` (e.g., `logs/%date-day%.log`, placeholders `%date-day%`/`%date-hrs%` etc. via `PlaceholderConvertor.swapSome()` at `src/main/java/me/vekster/lightanticheat/util/logger/Logger.java:58`)
- `log.log-violations.enabled`, `log.log-punishments.enabled`, `log.log-violations.message`, `cooldown`

Dispatch is async (`Scheduler.runTaskAsynchronously` at `src/main/java/me/vekster/lightanticheat/util/logger/Logger.java:63`). File is created under `plugins/LightAntiCheat/` relative to `Main.getDataFolder()`.

Check:

- Ensure `log.enabled` and the specific `log-*-violations/punishments.enabled` are `true`.
- Verify the resolved file path is writable; I/O errors are logged as:

  ```
  (LightAntiCheat-Plus) <IOException message>
  ```

- Cooldown suppresses writes just like alerts.

### Discord webhook

Keys (`src/main/java/me/vekster/lightanticheat/util/config/ConfigManager.java:116`):

- `discord-webhook.enabled` (global gate)
- `discord-webhook.send-violations.enabled`, `send-punishments.enabled`
- `webhook-url`, `message`, `cooldown`

Dispatch (`src/main/java/me/vekster/lightanticheat/util/logger/Logger.java:87`):

- Silently dropped if the URL does not start with `https://discord.com/api/webhooks/`.
- Posted async via `HttpsURLConnection`; HTTP 429 (rate-limit) is swallowed, all other `IOException`s are logged:

  ```
  (LightAntiCheat-Plus) <IOException message>
  ```

Check:

- Set all three `enabled: true` (global + per-type).
- Confirm the URL prefix exactly.
- Test with `cooldown: 0`.
- Watch console for the `IOException` line; 429s will not appear.

## Geyser / Floodgate classification

Bedrock vs Java classification controls whether checks fire for a player (`FloodgateHook`, `src/main/java/me/vekster/lightanticheat/util/hook/plugin/FloodgateHook.java:40`). Other code uses `FloodgateHook.isBedrockPlayer(player)` to swap `%edition%` and gate detection per check.

Config section `geyser-hook` (`src/main/resources/config.yml:139`, `src/main/java/me/vekster/lightanticheat/util/config/ConfigManager.java:158`):

```yaml
geyser-hook:
  enabled: true
  bedrock-only: true
  floodgate:
    enabled: true
  uuid:
    enabled: true
  prefix:
    enabled: true
    prefix-string: "."
```

Classification order in `isBedrockPlayerWithoutCache()` (`src/main/java/me/vekster/lightanticheat/util/hook/plugin/FloodgateHook.java:40`):

1. If `geyser-hook.enabled` is `false`, never Bedrock.
2. If `geyser-hook.uuid.enabled` and the player's UUID string starts with `000000`, Bedrock.
3. If `geyser-hook.prefix.enabled` and `prefix-string` is non-empty and the name starts with it (default `"."`), Bedrock.
4. If `geyser-hook.floodgate.enabled` is false or the `floodgate` plugin is absent, not Bedrock (unless matched above).
5. Otherwise, reflect into `org.geysermc.floodgate.api.FloodgateApi.isFloodgatePlayer(UUID)`. If reflection fails, LightAntiCheat logs:

   ```
   Floodgate plugin is installed but API reflection failed. Falling back to prefix/UUID detection.
   ```

   or at call time:

   ```
   Floodgate reflection hook failed. Falling back to prefix/UUID detection.
   ```

   and falls back to the prefix check.

Pocket Edition special-casing (`isProbablyPocketEditionPlayer()`, `src/main/java/me/vekster/lightanticheat/util/hook/plugin/FloodgateHook.java:76`) additionally checks `FloodgateApi.getPlayer(UUID).getDeviceOs()` against `UNKNOWN/GOOGLE/IOS/AMAZON/GEARVR/TVOS/PS4/NX/XBOX/WINDOWS_PHONE`. Failures log `Floodgate device lookup failed. Falling back to UNKNOWN.` and conservatively return pocket.

`bedrock-only` (`src/main/resources/config.yml:143`): when `true`, the plugin globally treats Java players as exempt from all checks; per-check `detection.java`/`detection.bedrock` remain as configured but the easier way to switch between Java-only vs Bedrock-only testing is this flag.

Per-check (`src/main/resources/config.yml:209` for example):

```yaml
detection:
  java: true
  bedrock: true
```

If a Bedrock player is not being checked (or vice versa), verify the three classifiers above and the per-check flags.

## Folia — ownership / scheduler exceptions

LightAntiCheat detects Folia via `FoliaUtil.loadFoliaUtil()` (`src/main/java/me/vekster/lightanticheat/util/hook/server/folia/FoliaUtil.java:44`) called from `Main.onEnable()`. Detection is `Class.forName("io.papermc.paper.threadedregions.RegionizedServer")`.

### What to collect

Do not delete or edit the stack trace. Collect the full log block including the exception type, message, and the first 20-30 lines of stack. Useful log lines to include:

- `Folia detected but scheduler API is unavailable. Falling back to Bukkit scheduler bridge.` (`src/main/java/me/vekster/lightanticheat/util/hook/server/folia/FoliaUtil.java:75`) — Folia was detected but reflected scheduler methods were missing. The plugin falls back to the Bukkit scheduler bridge.
- `Failed to invoke Folia global scheduler method <name>.` (`src/main/java/me/vekster/lightanticheat/util/hook/server/folia/FoliaUtil.java:213`)
- `Failed to invoke Folia async scheduler method <name>.` (`src/main/java/me/vekster/lightanticheat/util/hook/server/folia/FoliaUtil.java:221`)
- `Failed to invoke Folia entity scheduler method <name> for entity <uuid>.` (`src/main/java/me/vekster/lightanticheat/util/hook/server/folia/FoliaUtil.java:233`)
- `Failed to invoke Folia teleportAsync for <player>.` (`src/main/java/me/vekster/lightanticheat/util/hook/server/folia/FoliaUtil.java:145`)
- `Failed to invoke Folia ownership check method <name>.` (`src/main/java/me/vekster/lightanticheat/util/hook/server/folia/FoliaUtil.java:197`)

Also capture any origination stack from the server (e.g., `IllegalStateException: Cannot access ... async`, `Region not owned`, etc.) — the LightAntiCheat `isOwnedByCurrentRegion(...)` wrappers (`src/main/java/me/vekster/lightanticheat/util/hook/server/folia/FoliaUtil.java:167`) are ownership checks, not fixes. The correct fix is to schedule work on the owning region/entity scheduler (`FoliaUtil.runTask(...)`, `FoliaUtil.teleportPlayer(...)` etc.), as the plugin already does in `ViolationHandler` setbacks and `LACAsyncPlayerMoveEvent`.

### What not to do

Do not work around Folia ownership errors by blindly moving logic to raw async threads or by calling Bukkit region-sensitive APIs off the owning thread. Folia requires entity/region schedulers; raw async fixes will reintroduce ownership violations. If you are modifying the plugin, use `FoliaUtil` (`src/main/java/me/vekster/lightanticheat/util/hook/server/folia/FoliaUtil.java:18`) / `Scheduler` (`src/main/java/me/vekster/lightanticheat/util/scheduler/Scheduler.java:25`) as the existing call sites do, and guard access with `FoliaUtil.isOwnedByCurrentRegion(...)` where appropriate.

## Plugin hot reload caveat

Package managers and `/reload` invalidate the runtime state that packet interception and command dispatch depend on:

- Packet listener registration with PacketEvents (`PacketEventsInputProvider.start()/close()`) and with NMS is tied to the process lifecycle. A hot reload can leave a stale listener registered or a terminated `PacketEventsAPI` in place (see `API is terminated` above).
- `Bukkit.dispatchCommand` for punishments is scheduled via `Scheduler.runTask`; reload can cancel pending tasks or replace the plugin instance (`Main.instance`) mid-dispatch.
- Folia's `entity.getScheduler()` / ` teleportAsync` handles are per-process; a reload that replaces the classloader can cause `NoSuchMethod` / `ClassCast` / `isTerminated` noise.

A full process restart (stop the server, start it again — not `/reload` or a plugin manager reload) is the authoritative diagnostic baseline for any packet/command integration issue. Reproduce the problem after a clean restart before escalating. If the problem disappears after restart but recurs after `/reload`, treat it as a reload-coupling issue.

## What to collect when escalating

Provide the following verbatim (do not summarize or redact the error lines unless secrets are present):

1. Full startup log from the last clean restart — from `Loaded plugin LightAntiCheat` through the first occurrence of the error. Include:

   - Any `Invalid listener-mode: ...` line.
   - Any `Failed to start listener-mode '...': ...` line.
   - `PacketEvents plugin not found.` / `API is null/not loaded/not initialized/terminated` lines if in `packet` mode.
   - Folia lines if on Folia/Paper threaded regions.

2. Reload log if relevant — exact output of the reload command, including:

   - `Invalid listener-mode: ... Keeping previous mode ...`
   - `Failed to reconfigure listener-mode to ... Keeping previous mode ...`

3. Punishment failure — the full line:

   - `Failed to execute punishment command '...' for ... (...): ...` or `Skipped empty punishment command ...`

4. Effective `config.yml` — attach the file as loaded from `plugins/LightAntiCheat/config.yml` (not a template), or at minimum these sections:

   - `listener-mode`
   - `geyser-hook` (all subkeys)
   - `alerts`, `log`, `discord-webhook` (enabled/message/cooldown/webhook-url prefix)
   - Any `checks.<type>.<group>.<check>.punishment.commands` entries that are failing (show the literal `kick ...` lines).

5. Versions:

   - LightAntiCheat version and source (`plugins/LightAntiCheat/plugin.yml`, `/version LightAntiCheat`, or jar name).
   - Server version (`/version`, Paper/Spigot/Folia build).
   - PacketEvents version if `listener-mode: packet` (`/version PacketEvents`).
   - Floodgate/Geyser versions if `geyser-hook.enabled: true`.

6. Plugin list — output of `/plugins` or `plugins/` directory listing.

7. OS/Java if available — `java -version` and the launch line when the error reproduces.

For `kick Invalid name or UUID`, also include one rendered example: run `kick <exact name> <reason>` manually from console with the same name and report whether it succeeds. Note whether `config.yml` currently contains `*%name%` anywhere.

