# Development

## Build

Canonical build:

```
mvn clean package
```

`pom.xml:113` sets `<finalName>LightAntiCheat-Plus-${project.version}</finalName>` (`2.0.2` at `pom.xml:5`). Output is `target/LightAntiCheat-Plus-2.0.2.jar` (shaded). Do not use `mvn package` without `clean` for proof builds; `clean` is required to verify artifact freshness.

## Java Target

`pom.xml:18`:

```xml
<maven.compiler.release>8</maven.compiler.release>
```

`maven-compiler-plugin:3.13.0` (`pom.xml:132`) compiles with `<release>8</release>` and `-parameters`. Source and target are Java 8; bytecode is Java 8 compatible. Do not introduce `var`, records, or `Stream.toList()`.

## Spigot API

`pom.xml:19`:

```xml
<spigot.api.version>1.20.2-R0.1-SNAPSHOT</spigot.api.version>
```

Dependency (`pom.xml:52`):

```xml
<groupId>org.spigotmc</groupId>
<artifactId>spigot-api</artifactId>
<version>${spigot.api.version}</version>
<scope>provided</scope>
```

Repository `spigot-repo` (`pom.xml:35`): `https://hub.spigotmc.org/nexus/content/repositories/snapshots/`. `api-version: 1.13` in `src/main/resources/plugin.yml`. `folia-supported: true`.

## Dependencies

### Provided (not shaded)

- `org.spigotmc:spigot-api:${spigot.api.version}` — server provides.
- `org.jetbrains:annotations:24.1.0`, `com.google.code.findbugs:jsr305:3.0.2` — compile-only.
- `io.netty:netty-all:4.1.79.Final` — server provides.
- `org.projectlombok:lombok:1.18.42` — annotation processor only (`pom.xml:142`).
- `com.github.retrooper:packetevents-spigot:2.13.0` — PacketEvents owns transport (`pom.xml:93`). Must be installed as a separate plugin when `listener-mode: packet`; plugin is `softdepend` in `plugin.yml`. See PacketEvents ownership below.

### Compiled + shaded (relocated)

- `com.arkflame.vendor:multiversion:1.0.0-arkflame` (`pom.xml:84`)
- `com.tchristofferson:ConfigUpdater:2.2` (`pom.xml:89`)
- `com.fren_gor:lightInjector` via `src/main/java/com/fren_gor/lightInjector/LightInjector.java` (in-tree source, relocated)

`maven-shade-plugin:3.6.1` (`pom.xml:152`) relocations (`pom.xml:163`):

| pattern | shadedPattern |
|---|---|
| `com.fren_gor` | `me.vekster.lightanticheat.libs.fren_gor` |
| `me.vekster.multiversion` | `me.vekster.lightanticheat.libs.multiversion` |
| `com.tchristofferson` | `me.vekster.lightanticheat.libs.tchristofferson` |

`createDependencyReducedPom: false` (`pom.xml:162`). No other artifacts are relocated.

## PacketEvents Provided Ownership

`packetevents-spigot:2.13.0` is `scope: provided` (`pom.xml:97`). It is **not shaded**. The plugin does not embed PacketEvents; the server must have the `packetevents` plugin installed when `listener-mode: packet` is used. `PacketEventsInputProvider` (`src/main/java/me/vekster/lightanticheat/input/provider/packetevents/PacketEventsInputProvider.java:42`) resolves `Bukkit.getPluginManager().getPlugin("packetevents")` and `PacketEvents.getAPI()` at `start()` and fails fast if missing/not initialized. `NMS` mode has no PacketEvents requirement.

## Local Vendor Repository

`pom.xml:30`:

```xml
<repository>
  <id>arkflame-local-vendor</id>
  <url>file://${project.basedir}/vendor/maven-repository</url>
</repository>
```

Artifacts vendored under `vendor/maven-repository/`:

- `com/arkflame/vendor/multiversion/1.0.0-arkflame/`
- `com/arkflame/vendor/lightinjector/1.0.2-arkflame/`

Rationale: `multiversion` (`me.vekster.multiversion`) and `lightInjector` (`com.fren_gor.lightInjector`) are not published to Maven Central / Spigot repos at the required ArkFlame-patched coordinates. They provide Bukkit 1.8–1.20 compatibility adapters (`VerUtil`, `VerPlayer`, etc. import `me.vekster.multiversion.*` at `src/main/java/me/vekster/lightanticheat/version/VerUtil.java:7`) and the NMS Netty `LightInjector` (`src/main/java/com/fren_gor/lightInjector/LightInjector.java:23`) used by `NmsInputProvider`. The `vendor/` directory is project-local and must not be deleted; without it `mvn clean package` fails to resolve `com.arkflame.vendor:multiversion`. Do not replace `${project.basedir}` with an absolute filesystem path in docs or config — the `file://` URL is intentionally relative.

## Source / Test Layout

```
src/main/java/me/vekster/lightanticheat/
  Main.java
  api/                      # LACApi, CheckType, DetectionStatus, InstanceHolder, event/LAC*Event
  check/                    # Check, CheckName, CheckSetting, buffer/, checks/*/*/
  command/                  # LACCommand
  event/
    bus/                    # LACEventBus, LACEventType, LACEventPriority, LACEventSubscriber
    context/                # LACPlayerContextEvent
    packetreceive/          # LACAsyncPacketReceiveEvent
    playerattack/           # LACPlayerAttackEvent, LACAsyncPlayerAttackEvent
    playerbreakblock/       # LACPlayerBreakBlockEvent, LACAsyncPlayerBreakBlockEvent
    playermove/             # LACPlayerMoveEvent, LACAsyncPlayerMoveEvent, LACMovementChange, blockcache/
    playerplaceblock/       # LACPlayerPlaceBlockEvent, LACAsyncPlayerPlaceBlockEvent
  input/
    LACInputEngine.java
    LACInputDispatcher.java
    LACBukkitStateBridge.java
    LACPlayerInputQueue.java
    model/                  # LACInputMode, LACLocation, LACPacketFrame, LACMovementFrame, LACPlayerSession, LACPacketType
    provider/               # LACInputProvider, packetevents/, nms/
  player/                   # LACPlayer, LACPlayerManager, LACPlayerListener, cache/, cooldown/, violation/
  util/
    api/                    # ApiUtil, ApiInstance
    config/                 # ConfigManager, placeholder/
    scheduler/              # Scheduler, gamescheduler/{GameScheduler,BukkitScheduler,FoliaScheduler}
    hook/server/folia/      # FoliaUtil
    logger/, tps/, violation/ ...
  version/                  # VerPlayer, VerUtil, identifier/
  com/fren_gor/lightInjector/ # LightInjector (in-tree, shaded)

src/main/resources/
  plugin.yml                # filtered (version substitution), others not filtered (pom.xml:115)
  config.yml

src/test/java/me/vekster/lightanticheat/
  check/buffer/             # BufferTest
  check/checks/movement/nofall/ # NoFallPredictionProfileTest
  event/playermove/blockcache/  # BlockCacheEnvironmentTest
  input/                    # LACInputEngineTest, LACPlayerInputQueueTest
  input/model/              # LACInputModeTest, LACLocationTest
  input/provider/packetevents/ # PacketEventsPacketMapperTest, PacketEventsMovementTrackerTest, PacketEventsInputProviderSourceGuardTest
  player/                   # LACPlayerManagerTest, cache/history/PlayerCacheHistoryTest, violation/PlayerViolationsTest
  util/config/placeholder/  # PlaceholderConvertorPunishmentCommandTest
  util/physics/             # VanillaVerticalPhysicsTest

src/test/resources/lac-quality-extract/
  flight-a.json, flight-b.json, boat-a.json, nofall-{events,distance}.json
```

## Testing

`pom.xml:100` / `pom.xml:182`:

```xml
org.junit.jupiter:junit-jupiter:5.10.3:test
org.junit.platform:junit-platform-launcher:1.10.3:test
maven-surefire-plugin:3.5.2
```

Run:

```
mvn test
mvn clean test
```

Tests are compile-time verification only. They do not start a Bukkit server and do not prove runtime behavior on Spigot/Paper/Folia.

## Clean-Build Proof Expectations

For any change that touches `pom.xml`, `src/main/java`, `src/main/resources`, or `vendor/`:

1. `mvn clean package` must succeed with no warnings treated as errors.
2. `target/LightAntiCheat-Plus-*.jar` mtime must be after the youngest source change.
3. `plugin.yml` inside the jar must contain the current `<version>` (resource filtering at `pom.xml:124`).
4. Shaded classes must be at `me/vekster/lightanticheat/libs/...` inside the jar (`jar tf target/... | grep libs`).
5. `mvn test` must stay green if tests exist for the affected area.

## Runtime Verification (Separate from Compile Tests)

Compile tests do not substitute for runtime proof. After `mvn clean package`, runtime verification is a separate step:

- Install the jar + `packetevents` (if `listener-mode: packet`) on a local Spigot/Paper/Folia server matching `1.20.2` API.
- Confirm `onEnable` logs, `/lac` command, and `listener-mode` switch via config reload.
- Folia path requires `FoliaUtil.isFolia() == true` and `entityThread` routing through `io.papermc.paper.threadedregions`.

Do not publish machine-specific paths (home directory, `/tmp` workdir, local server path) in commits or docs.

