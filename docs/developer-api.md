# Developer API

Source:
- `src/main/java/me/vekster/lightanticheat/api/LACApi.java:7`
- `src/main/java/me/vekster/lightanticheat/api/InstanceHolder.java:3`
- `src/main/java/me/vekster/lightanticheat/api/CheckType.java:3`
- `src/main/java/me/vekster/lightanticheat/api/DetectionStatus.java:3`
- `src/main/java/me/vekster/lightanticheat/util/api/ApiUtil.java:15`
- `src/main/java/me/vekster/lightanticheat/util/api/ApiInstance.java:10`
- `src/main/java/me/vekster/lightanticheat/api/event/LACViolationEvent.java:11`
- `src/main/java/me/vekster/lightanticheat/api/event/LACPunishmentEvent.java:11`

## Getting the API

```java
LACApi api = LACApi.getInstance(); // LACApi.java:8
// delegates to InstanceHolder.getApi() // InstanceHolder.java:11
```

`ApiUtil.setApiInstance()` is called in `Main.onEnable` (`src/main/java/me/vekster/lightanticheat/Main.java:113`). Before that, `getInstance()` returns `null`. After `onDisable`, instance is not cleared automatically — treat as unavailable after disable.

## LACApi — Exact Signatures (`LACApi.java:7`)

```java
public interface LACApi {
    static LACApi getInstance() { return InstanceHolder.getApi(); }

    Set<String> getCheckNames(CheckType checkType);

    boolean disableDetection(Player player, String checkName);

    boolean disableDetection(Player player, String checkName, long durationMils);

    boolean enableDetection(Player player, String checkName);

    DetectionStatus getDetectionStatus(Player player, String checkName);
}
```

Impl `ApiInstance` (`ApiInstance.java:10`) delegates to `ApiUtil`.

## CheckType (`CheckType.java:3`)

```java
public enum CheckType { ALL, MOVEMENT, COMBAT, INTERACTION, PACKET, INVENTORY, PLAYER }
```

Distinct from `CheckName.CheckType` (`src/main/java/me/vekster/lightanticheat/check/CheckName.java:63`) which has no `ALL`.

## DetectionStatus (`DetectionStatus.java:3`)

```java
public enum DetectionStatus { ENABLED, DISABLED, TEMPORARILY_DISABLED }
```

Resolution in `ApiUtil.getCheckStatusLowercase` (`ApiUtil.java:141`):
- unknown `checkName` -> `DISABLED`
- not in `DISABLED_CHECKS` -> `ENABLED`
- `value == 0L` -> `DISABLED` (permanent)
- `value < now` -> `ENABLED` (expired)
- otherwise -> `TEMPORARILY_DISABLED`

Overload `getCheckStatus(Player, CheckSetting)` (`ApiUtil.java:129`) returns `DISABLED` if `player == null || checkSetting == null`.

## getCheckNames

```java
Set<String> getCheckNames(CheckType checkType)
```

- `checkType == ALL` -> unmodifiable view of all lowercased `CheckName.name()` (`ApiUtil.java:80`)
- otherwise lookup `CHECK_NAMES_BY_TYPE.get(checkType.name().toLowerCase())` or empty set (`ApiUtil.java:83`)
- Names are lowercased enum names, e.g. `flight_a`, `killaura_a`. Compare to `LACViolationEvent.getCheckName()` which is also lowercased (`LACViolationEvent.java:22`).

## disableDetection — permanent

```java
boolean disableDetection(Player player, String checkName)
```

- Normalizes `checkName.toLowerCase()`. Unknown name -> `false` (`ApiUtil.java:104`).
- Inserts `DISABLED_CHECKS.put(key, 0L)` if not already permanently disabled. If already `TEMPORARILY_DISABLED` (value `!= 0`), upgrades to permanent (`0L`) and returns `true` (`ApiUtil.java:112`). If already permanent, returns `false`.
- Permanent entry lives until `enableDetection` removes it or player entry is GC'd by periodic task.

## disableDetection — temporary

```java
boolean disableDetection(Player player, String checkName, long durationMils)
```

- Unknown name -> `false` (`ApiUtil.java:87`).
- If not present, inserts `now + duration` and returns `true` (`ApiUtil.java:92`).
- If present with `endTime != 0 && endTime < now + duration`, extends to `now + duration` and returns `true` (`ApiUtil.java:96`). Does not shorten a longer existing window. Permanent (`0L`) is never overwritten by this overload — returns `false`.
- Expiry is lazy: `getDetectionStatus` treats expired as `ENABLED`; periodic `Timer` task every 1000 ms (`ApiUtil.java:61`) removes entries where `player == null` (offline) or `value != 0 && value < now`.

## enableDetection

```java
boolean enableDetection(Player player, String checkName)
```

- Unknown name -> `false` (`ApiUtil.java:120`).
- `DISABLED_CHECKS.remove(key)` and returns `true` always for known names (`ApiUtil.java:125`), even if not previously disabled (still returns `true` per impl — note differs from disable which is conditional).

## getDetectionStatus

```java
DetectionStatus getDetectionStatus(Player player, String checkName)
```

- Lowercases `checkName`; delegates to `getCheckStatusLowercase(player, lower)` (`ApiUtil.java:136`).

## Events

### LACViolationEvent (`LACViolationEvent.java:11`)

```java
public class LACViolationEvent extends Event implements Cancellable {
    public LACViolationEvent(CheckSetting checkSetting, Player player, LACPlayer lacPlayer, @Nullable Cancellable cancellable);
    public String getCheckName();         // lowercased CheckName.name()
    public Player getPlayer();
    public CheckSetting getCheckSettings();
    public LACPlayer getAcPlayer();
    public @Nullable Cancellable getCancellable();
    public boolean isCancelled();
    public void setCancelled(boolean cancel);
    public HandlerList getHandlers();
    public static HandlerList getHandlerList();
}
```

Fired from `Check.callViolationEvent` (`src/main/java/me/vekster/lightanticheat/check/Check.java:80`): `Bukkit.getPluginManager().callEvent(new LACViolationEvent(...))`.

### LACPunishmentEvent (`LACPunishmentEvent.java:11`)

```java
public class LACPunishmentEvent extends Event implements Cancellable {
    public LACPunishmentEvent(CheckSetting checkSetting, Player player, LACPlayer lacPlayer, @Nullable Cancellable cancellable);
    public LACPunishmentEvent(LACViolationEvent event); // copies checkName/player/checkSetting/lacPlayer/cancellable
    public String getCheckName();
    public Player getPlayer();
    public CheckSetting getCheckSettings();
    public LACPlayer getAcPlayer();
    public @Nullable Cancellable getCancellable();
    public boolean isCancelled();
    public void setCancelled(boolean cancel);
    public HandlerList getHandlers();
    public static HandlerList getHandlerList();
}
```

Fired from `ViolationHandler.onFlag` when `violations == punishmentVio` (`src/main/java/me/vekster/lightanticheat/util/violation/ViolationHandler.java:93` and `181`): `Bukkit.callEvent(new LACPunishmentEvent(event))`.

## Cancellation Semantics

- Both events are `Cancellable` themselves (`event.isCancelled()` / `setCancelled`). Separately they carry an optional Bukkit `Cancellable getCancellable()` (the original movement/interaction cancellable, often `null` for packet-derived violations).
- `ViolationHandler` honors API cancellation only when `ConfigManager.Config.Api.enabled == true` (`ViolationHandler.java:87` and `189`):

  ```java
  if (ConfigManager.Config.Api.enabled && event.isCancelled()) return;
  ```

  If `api.enabled == false`, `setCancelled(true)` on the event is ignored — violation still increments, alerts fire, setback/punishment still run.

- When not cancelled, `ViolationHandler.onFlag` sets movement setback via `event.getCancellable().setCancelled(true)` if `checkSetting.setback && violations >= setbackVio` and not `isVerticalSetback`. Cancelling the event does **not** automatically cancel the Bukkit cancellable — they are distinct.

- `CheckUtil.isCheckAllowed` (`src/main/java/me/vekster/lightanticheat/util/detection/CheckUtil.java:80`) gates detection:

  ```java
  DetectionStatus s = ApiUtil.getCheckStatus(player, checkSetting);
  if (ConfigManager.Config.Api.enabled && s != DetectionStatus.ENABLED) return false;
  ```

  So `DISABLED`/`TEMPORARILY_DISABLED` suppress checks only when `api.enabled == true`. Temporarily-disabled entries that have expired are treated as `ENABLED`.

- `when api.enabled changes honoring` — `Config.Api.enabled` is read live from `ConfigManager.Config.Api.enabled` (static field reloaded via `ConfigManager.reloadConfig`). No restart needed; toggling `api.enabled` in `config.yml` + `/lac reload` immediately changes whether `isCheckAllowed` and `ViolationHandler` respect `DISABLED_CHECKS` and event cancellation.

## Threading

API methods touch `ConcurrentHashMap` and are safe off owner thread. Events fire on whichever thread the check fires them (typically owner thread via `LACEventBus` -> `LACInputDispatcher`, but also Bukkit thread via bridge). Listeners must not call Bukkit API off owner thread; use `Scheduler.entityThread`.

## Minimal Example

```java
LACApi api = LACApi.getInstance();
if (api == null) return;
api.disableDetection(player, "flight_a", 10_000L); // 10s
DetectionStatus s = api.getDetectionStatus(player, "flight_a"); // TEMPORARILY_DISABLED
api.enableDetection(player, "flight_a");

@EventHandler(priority = EventPriority.HIGH)
public void onViolation(LACViolationEvent e) {
    if (!ConfigManager.Config.Api.enabled) return;
    if (e.getCheckName().equals("speed_a")) e.setCancelled(true);
}
@EventHandler(priority = EventPriority.HIGH)
public void onPunishment(LACPunishmentEvent e) {
    if (!ConfigManager.Config.Api.enabled) return;
    e.setCancelled(true); // suppress default punish commands
}
```

Register events normally via `Bukkit.getPluginManager().registerEvents`.

