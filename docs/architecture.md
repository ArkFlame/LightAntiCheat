# Architecture

## Input Pipeline

```
PacketEvents / NMS transport
  -> provider-neutral input models (src/main/java/me/vekster/lightanticheat/input/model/*:11)
  -> LACInputEngine (src/main/java/me/vekster/lightanticheat/input/LACInputEngine.java:16)
  -> per-player ordered queue (src/main/java/me/vekster/lightanticheat/input/LACPlayerInputQueue.java:15)
  -> owner-thread hydration
  -> LACInputDispatcher (src/main/java/me/vekster/lightanticheat/input/LACInputDispatcher.java:26)
  -> LACEventBus (src/main/java/me/vekster/lightanticheat/event/bus/LACEventBus.java:16)
  -> checks (src/main/java/me/vekster/lightanticheat/check/Check.java:21)
  -> violation event (LACViolationEvent)
  -> ViolationHandler -> alerts / setback / punishment (src/main/java/me/vekster/lightanticheat/util/violation/ViolationHandler.java:34)
```

Providers implement `LACInputProvider` (`src/main/java/me/vekster/lightanticheat/input/provider/LACInputProvider.java:5`):
- `LACInputMode getMode()`
- `void start()` / `boolean isStarted()` / `void close()`

Two transports:
- `PacketEventsInputProvider` (`src/main/java/me/vekster/lightanticheat/input/provider/packetevents/PacketEventsInputProvider.java:33`) — PacketEvents `PacketListenerAbstract` at `LOWEST`, `onPacketReceive` on Netty thread.
- `NmsInputProvider` (`src/main/java/me/vekster/lightanticheat/input/provider/nms/NmsInputProvider.java:21`) — `LightInjector` (`com.fren_gor.lightInjector`), `onPacketReceiveAsync` on Netty thread.

Providers map raw packets to `LACPacketType` (`src/main/java/me/vekster/lightanticheat/input/model/LACPacketType.java:3`) and build `LACPacketFrame` (`src/main/java/me/vekster/lightanticheat/input/model/LACPacketFrame.java:6`) + optional `LACMovementFrame` (`src/main/java/me/vekster/lightanticheat/input/model/LACMovementFrame.java:6`). Provider-neutral models carry `LACLocation` (`src/main/java/me/vekster/lightanticheat/input/model/LACLocation.java:6`) and `LACPlayerSession` (`src/main/java/me/vekster/lightanticheat/input/model/LACPlayerSession.java:6`).

No `PacketEvents` or NMS types leak past provider boundary. Check layer imports only `LAC*` models and `LACEventBus` events.

## Packet Callback Boundary

Both providers execute on Netty/packet thread. They do no Bukkit API access. They:
1. `LACPlayerManager.captureSession(uuid)` to resolve `LACPlayerSession` (fail-closed if absent).
2. `engine.nextSequence(session)` for monotonic per-player sequence.
3. For `FLYING`, `PacketEventsMovementTracker` produces `LACMovementFrame` (from/to, pos/rot deltas, claimedOnGround).
4. `engine.enqueue(frame, movementOpt)` — enqueues only; no dispatch here.

## Owner-Thread / Bukkit State Boundary

`LACInputEngine.enqueue` (`src/main/java/me/vekster/lightanticheat/input/LACInputEngine.java:186`) inserts into per-player `LACPlayerInputQueue`. Queue validates:
- `session.equals(frame.getSession())`
- `frame.getInputEpoch() == session.getPlayerEpoch()` (stale epoch drop)
- `movementFrame` session/epoch match

`LACPlayerInputQueue.scheduleDrain` (`src/main/java/me/vekster/lightanticheat/input/LACPlayerInputQueue.java:131`) does CAS `drainScheduled` and delegates to `LACPlayerManager.execute(session, true, this::drain)` (`src/main/java/me/vekster/lightanticheat/player/LACPlayerManager.java:124`). `execute` routes through `Scheduler.entityThread(player, force, ...)` (`src/main/java/me/vekster/lightanticheat/util/scheduler/Scheduler.java:60`), which on Folia uses `entity.getScheduler().execute(...)` (`src/main/java/me/vekster/lightanticheat/util/hook/server/folia/FoliaUtil.java:226`) and on Bukkit uses main-thread scheduler. Drain thus runs on owner thread (entity owner region on Folia, primary thread on Spigot/Paper).

`drain` validates `context.isCurrent()` and `owner.matchesSession(session)`; processes up to `MAX_FRAMES_PER_DRAIN = 128` per invocation; re-schedules if non-empty. Stale sessions are dropped.

`LACInputDispatcher.dispatch` (`src/main/java/me/vekster/lightanticheat/input/LACInputDispatcher.java:37`) runs already on owner thread (it is called from `drain` via dispatcher consumer). It executes a second `LACPlayerManager.execute(session)` to hydrate Bukkit state, then:
- Builds `Location` + `BlockCache` only if `worldId` matches `context.worldId()` and `FoliaUtil.isOwnedByCurrentRegion` holds.
- Fires `LACAsyncPacketReceiveEvent` (`ASYNC_PACKET_RECEIVE`) for every frame.
- For `USE_ENTITY` on `>1.8`, fires `LACAsyncPlayerAttackEvent` (`ASYNC_PLAYER_ATTACK`).
- For `FLYING` with `LACMovementFrame` in `PACKET` mode, builds `LACAsyncPlayerMoveEvent` (`ASYNC_PLAYER_MOVE`) with `fromCache`/`toCache`.

Owner-thread guarantee: no `Player.getWorld()`, `BlockCache.capture`, teleport, or check cache access occurs off owner thread.

## Separate Bukkit Confirmation Lane

`LACBukkitStateBridge` (`src/main/java/me/vekster/lightanticheat/input/LACBukkitStateBridge.java:26`) handles synchronous Bukkit events (already on owner thread):
- `onMovement(PlayerMoveEvent)` -> `LACPlayerMoveEvent` (`PLAYER_MOVE`); in `NMS` mode additionally emits `LACAsyncPlayerMoveEvent` from sync event. In `PACKET` mode async movement comes only from dispatcher, not bridge.
- `onEntityDamage(EntityDamageByEntityEvent)` -> `LACPlayerAttackEvent` (`PLAYER_ATTACK`) + `LACAsyncPlayerAttackEvent` (`ASYNC_PLAYER_ATTACK`).
- `onBlockPlace(BlockPlaceEvent)` -> `PLAYER_PLACE_BLOCK` + `ASYNC_PLAYER_PLACE_BLOCK`.
- `onBlockBreak(BlockBreakEvent)` -> `PLAYER_BREAK_BLOCK` + `ASYNC_PLAYER_BREAK_BLOCK`.

Bridge is the server-confirmed lane. Packet lane is speculative (pre-Bukkit). Checks subscribe to async (`ASYNC_*`) or sync (`PLAYER_*`) explicitly; they do not mix assumptions.

## Player / Session Epochs

- `LACPlayer.epoch` (`src/main/java/me/vekster/lightanticheat/player/LACPlayer.java:39`) — `AtomicLong`, bumped on `attach`, `beginTransition`, `completeTransition`, `detach`.
- `LACPlayerSession.playerEpoch` (`src/main/java/me/vekster/lightanticheat/input/model/LACPlayerSession.java:10`) — snapshot of epoch at capture.
- `LACInputEngine.inputEpoch` (`src/main/java/me/vekster/lightanticheat/input/LACInputEngine.java:40`) — bumped on mode switch (`reconfigure`) and `close`.
- `LACPlayer.Context.epoch` (`src/main/java/me/vekster/lightanticheat/player/LACPlayer.java:51`) — captured at `LACPlayer.capture`.

Invalidation:
- Queue drops frames where `inputEpoch != session.playerEpoch` or session mismatch.
- `LACPlayer.isCurrent(Context)` (`src/main/java/me/vekster/lightanticheat/player/LACPlayer.java:183`) checks `active`, `boundPlayer == context.player`, `cache ==`, `epoch ==`, `worldId ==`, online, world match, Folia region ownership.
- `LACPlayer.matchesSession(session)` checks `uuid`, `active`, `epoch == session.playerEpoch`, `worldId == session.worldId`.
- `LACPlayerManager.execute(Session,...)` re-checks `matchesSession` before and inside `entityThread`.

World change goes through `beginTransition`/`completeTransition`; stale queues drain is re-scheduled only if `isCurrent`.

## Mode Switching

`LACInputMode` (`src/main/java/me/vekster/lightanticheat/input/model/LACInputMode.java:6`): `PACKET` (`listener-mode: packet`) and `NMS` (`nms`).

Startup `Main.onEnable` (`src/main/java/me/vekster/lightanticheat/Main.java:96`): parses `Config.listenerMode`, creates `LACInputEngine(plugin, mode)`, which activates initial provider via `ensurePacketProvider` / `ensureNmsProvider`. Failure disables plugin.

Runtime `ConfigManager.reloadConfig` (`src/main/java/me/vekster/lightanticheat/util/config/ConfigManager.java:253`): parses new mode, calls `engine.reconfigure(target)` (`src/main/java/me/vekster/lightanticheat/input/LACInputEngine.java:113`). `reconfigure`:
- No-op if `target == current`.
- Ensures target provider started.
- Increments `inputEpoch`, sets `activeMode`.
- If leaving `NMS`, closes NMS provider.

Providers gate on `engine.getActiveMode()` per packet; stale packets after epoch bump are dropped at `enqueue` and queue level.

No world replication or tick compensation is performed. Input frames are forwarded as-is; lag/exploit checks use `TPSCalculator`, `BlockCache`, and config thresholds, not predicted world state.

## LACEventBus — Explicit Per-Listener Subscriptions

`LACEventBus` (`src/main/java/me/vekster/lightanticheat/event/bus/LACEventBus.java:16`) is static. `LACEventType` (`src/main/java/me/vekster/lightanticheat/event/bus/LACEventType.java:3`): `ASYNC_PACKET_RECEIVE`, `PLAYER_MOVE`, `ASYNC_PLAYER_MOVE`, `PLAYER_ATTACK`, `ASYNC_PLAYER_ATTACK`, `PLAYER_PLACE_BLOCK`, `ASYNC_PLAYER_PLACE_BLOCK`, `PLAYER_BREAK_BLOCK`, `ASYNC_PLAYER_BREAK_BLOCK`.

Subscriptions: `register(type, priority, owner, methodName, movementRequirement, consumer)` (`LACEventBus.java:34`). `LACEventSubscriber.registerLACEvents()` (`src/main/java/me/vekster/lightanticheat/event/bus/LACEventSubscriber.java:4`) is explicit per check. `Main.registerListener` (`Main.java:239`) calls both Bukkit `registerEvents` and `registerLACEvents`; `Main.registerCheckListener` (`Main.java:246`) calls `Check.registerListener`.

Dispatch `LACEventBus.call(type, event)` walks prebuilt snapshots (`SNAPSHOTS` / `MOVEMENT_SNAPSHOTS` by `LACMovementChange` mask). `canDispatch` gates on `LACPlayerContextEvent.canDispatch()` (epoch/world validity). No reflection scanning, no annotation magic — each listener subscribes explicitly.

## Checks

`Check` (`src/main/java/me/vekster/lightanticheat/check/Check.java:21`) extends `CheckUtil`, implements `LACEventSubscriber`. Constructor loads `CheckSetting` via `ConfigManager.loadCheck`. Static `registerListener(name, listener)` unregisters previous, checks `checkSetting.enabled && Config.enabled`, then registers Bukkit + LAC events. Checks fire violations via `callViolationEvent` -> `Bukkit.callEvent(new LACViolationEvent(...))`.

## Violation -> Punishment

`ViolationHandler` (`src/main/java/me/vekster/lightanticheat/util/violation/ViolationHandler.java:34`) listens `HIGHEST` to `LACViolationEvent` and `LACPunishmentEvent`:

- `onFlag` (`ViolationHandler.java:84`): honours `Config.Api.enabled && event.isCancelled()`; increments `PlayerViolations`; logs/alerts/discord with cooldown via `tryAcquire`; setback (`event.getCancellable().setCancelled(true)` or vertical setback teleport via `FoliaUtil.teleportPlayer`); if `violations == punishmentVio` fires `LACPunishmentEvent`.
- `onPunishment` (`ViolationHandler.java:186`): honours cancellation; logs/alerts/discord; executes `punishmentCommands` in two phases — **preparation** in the punishment event's player/server-owned execution context (live placeholders resolved via `PlaceholderConvertor.renderPunishmentCommand` (`PlaceholderConvertor.java:133` — `swapAll` + `colorize(true)` + `normalizeLegacyVanillaKickTarget` + `normalizeBukkitDispatchCommandLine` which accepts and strips a single optional leading `/` with surrounding whitespace trimmed; empty results skipped) before violation reset, and **dispatch** via `RuntimeCommandDispatcher.dispatchConsoleBatch` (`RuntimeCommandDispatcher.java:17`) scheduled with `Scheduler.globalThread` (`Scheduler.java:82`) carrying only prepared immutable strings in YAML list order — Bukkit/Spigot/classic Paper → primary server thread (`BukkitScheduler.java:12` → `Bukkit.getScheduler().runTask`), Folia → global region scheduler (`FoliaScheduler.java:11` → `FoliaUtil.runTask` / `FoliaUtil.java:87`); console sender; no live player reads in the dispatch task; `dispatchCommand=false` logs a controlled `Punishment command was not handled: '<rendered>' for <name> (<Title>).` and continues; `CommandException` is contained/logged as `Failed to execute punishment command '<rendered>' for <name> (<Title>): <msg>` and the next command continues; custom command semantics (`kick`/`ban`/etc. existence and arguments) remain server-defined; legacy `kick *%name%` compatibility remains via `normalizeLegacyVanillaKickTarget`; then violations reset (`ViolationHandler.java:240`).

## Folia Scheduler Ownership

`Scheduler` (`src/main/java/me/vekster/lightanticheat/util/scheduler/Scheduler.java:19`) delegates to `GameScheduler` impl chosen at class load: `FoliaScheduler` if `FoliaUtil.isFolia()` else `BukkitScheduler`. `FoliaUtil` (`src/main/java/me/vekster/lightanticheat/util/hook/server/folia/FoliaUtil.java:44`) detects `io.papermc.paper.threadedregions.RegionizedServer` via reflection and caches `RegionScheduler`/`EntityScheduler` handles. `entityThread(player, ...)` routes to entity scheduler; `isOwnedByCurrentRegion(entity/location/block)` guards `BlockCache` reads and movement emission in dispatcher. All `runTask*`/`teleportPlayer` calls go through `FoliaUtil` to use `teleportAsync` on Folia.

## Config / Check Registration Lifecycle

- `Main.onEnable` (`Main.java:94`): `ConfigManager.loadConfig()`, parse `listenerMode`, create `LACInputEngine`, `LACEventBus` implicitly empty, register `LACPlayerListener`, `ViolationHandler`, then `registerCheckListener` for each check (40 checks: FlightA/B/C, SpeedA-F, etc.).
- `Check` constructor loads `CheckSetting` once; `ConfigManager.loadCheck` maps `checks.<type>.<group>.<group>_<check>` to `CheckSetting` fields (`enabled`, `punishable`, `punishmentVio`, `minTps`, `maxPing`, etc.).
- `ConfigManager.reloadConfig` (`ConfigManager.java:253`): reloads Bukkit config, re-parses `listenerMode` and calls `engine.reconfigure`; reloads each `CheckSetting` via `loadCheck`; re-calls `Check.registerListener` which unregisters and re-registers based on new `enabled`.
- `Main.onDisable` (`Main.java:204`): `inputEngine.close()`, `LACEventBus.unregisterAll()`, `BlockMaterialCache.clear()`, `Scheduler.cancelTimer()`.

## What Is Not Done

Compensated world replication (tick-accurate block/entity replay per player as in Grim) is not implemented. LAC validates movement and interaction against live `BlockCache` snapshots and config thresholds on the owner thread, without per-player tick prediction or rollback world.
