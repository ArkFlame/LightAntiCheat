# Getting Started

## Requirements

| Dependency | Version / Value | Source |
|---|---|---|
| Java | `8` (`maven.compiler.release=8`) | `pom.xml:18` |
| Spigot API (compile) | `1.20.2-R0.1-SNAPSHOT` (`scope=provided`) | `pom.xml:19,52-58` |
| `api-version` | `1.13` | `src/main/resources/plugin.yml:4` |
| PacketEvents | `2.13.0` (`scope=provided`, not bundled) | `pom.xml:93-98` — `softdepend: packetevents` in `plugin.yml:7` |

> PacketEvents is **not shaded** (`maven-shade-plugin:163-175` relocates only `com.fren_gor`, `me.vekster.multiversion`, `com.tchristofferson`). You must install it separately if you use `listener-mode: packet`.

## Installation

1. Build or download `LightAntiCheat-Plus-2.0.1.jar` (`pom.xml:8-9`, `finalName: pom.xml:114`).
2. Place the jar in `plugins/` on the server.
3. Install PacketEvents separately if you intend to use the default mode (see below). No auto-install is performed.
4. **Full restart** the server — do not rely on `/reload` or plugin hot-swap for packet integration. `Main.onEnable:91-201` validates `listener-mode` and constructs `LACInputEngine`; on invalid mode or failed provider start the plugin disables itself (`Main.java:98-108`). A full restart is the authoritative way to (re)bind packet listeners.
5. First start generates config via `Main.getInstance().saveDefaultConfig()` and `ConfigUpdater` (`src/main/java/me/vekster/lightanticheat/util/config/ConfigManager.java:202-215`). Edit `plugins/LightAntiCheat-Plus/config.yml` (folder derived from `plugin.yml:1` `name: LightAntiCheat-Plus` via `JavaPlugin.getDataFolder()`).

## Listener Modes (`config.yml:2`)

```yaml
listener-mode: "packet"  # default
```

Parsed by `src/main/java/me/vekster/lightanticheat/input/model/LACInputMode.java:10-26` — accepted values are `packet` and `nms` (case-insensitive, trimmed).

| Mode | Provider | PacketEvents required | How it works |
|---|---|---|---|
| `packet` (default) | `PacketEventsInputProvider` (`src/main/java/me/vekster/lightanticheat/input/provider/packetevents/PacketEventsInputProvider.java`) | **Yes** — plugin must be present and enabled, API loaded/initialized/not terminated (`PacketEventsInputProvider.java:68-87`). Uses reflection-loaded provider via `LACInputEngine.java:23-29`. | Listens via `PacketEvents` `PacketListenerAbstract` at `LOWEST` priority, enqueues `LACPacketFrame` into `LACInputEngine`. |
| `nms` | `NmsInputProvider` (`src/main/java/me/vekster/lightanticheat/input/provider/nms/NmsInputProvider.java`) — extends `LightInjector` (`com.fren_gor.lightInjector.LightInjector`) | **No** | NMS channel injector (`LightInjector`), intercepts packets on the Netty channel directly. `LACInputEngine.java:32-35` constructs it eagerly. |

Switching at runtime: `ConfigManager.reloadConfig:253-283` parses the new value and calls `LACInputEngine.reconfigure(target)` (`LACInputEngine.java:113-148`). If the target equals current mode, no-op. If the new provider fails to start, the engine throws `IllegalStateException` and the previous mode is kept (error logged).

If `listener-mode` is invalid at startup, `Main.java:97-100` logs `Invalid listener-mode` and disables the plugin. If the provider fails to start, `Main.java:102-108` logs `Failed to start listener-mode` and disables.

## Plugin Aliases

Defined in `src/main/resources/plugin.yml:22-24`:

```
/lightanticheat  — primary
/light, /lac, /anticheat, /ac  — aliases
```

Stored as `lightanticheat: aliases: [light, lac, anticheat, ac]`.

## Startup / Disable Lifecycle (`src/main/java/me/vekster/lightanticheat/Main.java`)

### `onEnable:91-201`

1. `FoliaUtil.loadFoliaUtil()` (`Main.java:93`) — detects Folia by probing `io.papermc.paper.threadedregions.RegionizedServer` (`FoliaUtil.java:44-45`).
2. `ConfigManager.loadConfig()` (`Main.java:94`) — `saveDefaultConfig()` + `ConfigUpdater.update()` + reflection load into `ConfigManager.Config`.
3. Parse `listener-mode` via `LACInputMode.parse` (`Main.java:96`); disable on empty/invalid.
4. `new LACInputEngine(this, parsedMode)` (`Main.java:103`); disable on `IllegalStateException`/`LinkageError`.
5. `Buffer.loadBufferCleaner`, `TPSCalculator.loadTPSCalculator`, `Logger.logFile("")` (`Main.java:110-112`) — creates log file path `getDataFolder()/logs/%date-day%.log` (`Logger.java:59-60`, `config.yml:70`).
6. `ApiUtil.setApiInstance`, `LACPlayerListener`, `ExternalNPCUtil`, `ViolationHandler`, `UnloadedChunkListener`, `InvalidPingListener`, CPS/ConnectionStability calculators (`Main.java:113-132`).
7. Command registration (`Main.java:134-138`): `getCommand("lightanticheat").setExecutor(new LACCommand())` + tab completer.
8. Update checker (`Updater.loadUpdateChecker`, listener) and all check listeners via `registerCheckListener` (`Main.java:140-200`).

### `onDisable:203-213`

- `inputEngine.close()` (`Main.java:205-207`) — unregisters PacketEvents listener (`PacketEventsInputProvider.java:140-156`) and NMS injector, clears queues.
- `LACEventBus.unregisterAll()`, `BlockMaterialCache.clear()`, `Updater.shutdownUpdateChecker()`, `Scheduler.cancelTimer()`.

## Folia Support

- `plugin.yml:5` declares `folia-supported: true`.
- `FoliaUtil.java` reflects on `getGlobalRegionScheduler`, `getAsyncScheduler`, `getRegionScheduler`, `Entity.getScheduler`, `Entity.teleportAsync`, and ownership checks (`FoliaUtil.java:51-71`). If any reflection fails, falls back to Bukkit scheduler bridge (`FoliaUtil.java:72-76`).
- Scheduling helpers: `runTask`, `runTaskLater`, `runTaskTimer`, async variants — all delegate to global/region/async/entity schedulers when on Folia, otherwise to `Scheduler`/`Bukkit` bridge.
- Teleport (`LACCommand.java:242-244`): `FoliaUtil.teleportPlayer(player, location)` — uses `Entity.teleportAsync` on Folia, `player.teleport` otherwise (`FoliaUtil.java:135-147`).
- `plugin.yml` soft-depends ensure load-order flexibility; no hard `depend` on Folia.

## Geyser / Floodgate (Optional)

- `plugin.yml:8-9` `softdepend: Geyser-Spigot, floodgate` — absent plugins are silently ignored.
- `config.yml:139-154` `geyser-hook`:
  - `enabled: true` (`ConfigManager.java:158-173`)
  - `bedrock-only: true` — when true, non-Bedrock players are skipped at check-entry (`CheckUtil.java:64-65`).
  - `floodgate.enabled: true`, `uuid.enabled: true` (UUID prefix `000000`), `prefix.enabled: true` + `prefix-string: "."`.
- Detection in `FloodgateHook.java:40-64`: checks UUID/prefix first, then reflective `org.geysermc.floodgate.api.FloodgateApi.isFloodgatePlayer(UUID)` (`FloodgateHook.java:157-161`). Cached per-player via `CooldownUtil.isBedrockPlayer`.
- Per-check `detection.java` / `detection.bedrock` gates still apply (`config.yml:215-217` etc., `CheckUtil.java:66-67`).

Other soft-depends (`plugin.yml:10-16`) — `ViaVersion`, `GSit`, `mcMMO`, `ValhallaMMO`, `VeinMiner`, `AureliumSkills`, `ExecutableItems`, `EnchantsSquared` — are optional integrations; absence does not prevent startup.

## Config Reload

```
/lightanticheat reload  (aliases: /lac reload etc.)
```

Handled in `LACCommand.java:100-113` — requires `lightanticheat.reload` (or `lightanticheat.*`), calls `ConfigManager.reloadConfig()` and reports elapsed ms via `messages.command-messages.reload.message` (`config.yml:15-17`).

`ConfigManager.reloadConfig:253-283` reloads `config.yml`, re-parses `listener-mode`, reconfigures `LACInputEngine` transactionally, then reloads each `CheckSetting` (`CheckManager.loadCheck`) and re-registers listeners.

## Generated Files

All paths are relative to the plugin data folder `plugins/LightAntiCheat-Plus/` (Bukkit `JavaPlugin.getDataFolder()` derived from `plugin.yml:1` `name: LightAntiCheat-Plus`):

| File | Path | Source |
|---|---|---|
| Config | `plugins/LightAntiCheat-Plus/config.yml` | `ConfigManager.java:204` `saveDefaultConfig()` + `new File(getDataFolder(), "config.yml")` (`ConfigManager.java:207-208`) |
| Logs | `plugins/LightAntiCheat-Plus/logs/%date-day%.log` | `config.yml:70` `log.file: "logs/%date-day%.log"` joined with `getDataFolder().getPath() + "/" + Config.Log.file` (`Logger.java:59-60`); created async with `mkdirs()` (`Logger.java:63-65`) |

No other files are generated by default. Discord webhook, update checker, and bStats are opt-in via config.

## Java / API Compatibility

- Compiled for Java 8 (`maven.compiler.release=8`, `maven-compiler-plugin:3.13.0` `pom.xml:132-150`). Runs on Java 8+ runtimes.
- `api-version: 1.13` (`plugin.yml:4`) — explicit 1.13+ API, no legacy material name fallbacks beyond `Spigot` abstraction.
- Tested compile target `spigot-api 1.20.2-R0.1-SNAPSHOT` (`pom.xml:19`); `multiversion` vendor lib shaded for NMS compatibility (`pom.xml:84-87`).
