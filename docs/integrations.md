# Integrations — Descriptor vs Source Contract

Source of truth for every external integration: `plugin.yml` softdepend vs actual hook code in tree,
how detection really happens, what it touches, and what happens when the external is absent.

> References use `file:line` to jump to source. No invented behavior — if a hook does not exist, that is stated.

---

## 1. Descriptor (`src/main/resources/plugin.yml:6`) vs Source

`plugin.yml:6-17`:

```yaml
softdepend:
  - packetevents
  - Geyser-Spigot
  - floodgate
  - ViaVersion
  - GSit
  - mcMMO
  - ValhallaMMO
  - VeinMiner
  - AureliumSkills
  - ExecutableItems
  - EnchantsSquared
```

Note: `EnchantsPlus` alias does **not** appear in `plugin.yml` — it is handled in code only.
`EliteMobs` has a hook class but is **not** in `softdepend`. `Paper` and `Folia` are platform checks (no descriptor entry).

---

## 2. Contract Table (all requested integrations)

| Integration | Descriptor | Direct source hook | Detection | Affected checks / features | Missing-plugin behavior |
|---|---|---|---|---|---|
| **PacketEvents** (`packetevents`) | `softdepend: packetevents` `plugin.yml:7` | Yes — `input/provider/packetevents/PacketEventsInputProvider.java:33` loaded reflectively via `LACInputEngine.java:26` (`Class.forName` + ctor) | LAC looks up plugin by name `packetevents`/`PacketEvents` (`PacketEventsInputProvider.java:42`), then uses **external** `PacketEvents.getAPI()` lifecycle: `isLoaded/isInitialized/isTerminated` (`PacketEventsInputProvider.java:75-87`), registers `PacketListenerAbstract(LOWEST)` via `api.getEventManager().registerListener` (`PacketEventsInputProvider.java:102,121`). Handles `UserDisconnectEvent` cleanup. Filters `ConnectionState.PLAY` + `PacketType.Play.Client` (`PacketEventsInputProvider.java:166-176`). NMS mode (`NmsInputProvider.java:21`) is independent. | Input path only — `LACInputMode.PACKET` (`input/model/LACInputMode.java:7`). `LACInputEngine.activateInitialMode/reconfigure` throws `IllegalStateException` if PacketEvents absent (`LACInputEngine.java:73`). No per-check logic; feeds `LACPacketFrame`/`LACMovementFrame` to dispatcher. | `start()` throws `IllegalStateException("PacketEvents plugin not found/not enabled/API null/not loaded/not initialized/terminated")`. Engine does not silently fall back; caller must select `NMS` mode. `close()` unregisters listener + clears tracker (`PacketEventsInputProvider.java:140`). `NMS` mode works without PacketEvents. |
| **Geyser-Spigot** (`Geyser-Spigot`) | `softdepend: Geyser-Spigot` `plugin.yml:8` | No dedicated hook class. Covered indirectly by `FloodgateHook.java:1` via `ConfigManager.Config.GeyserHook` (`config/ConfigManager.java:158`) | No direct Geyser API call found (`rg ViaVersion\|GSit\|Geyser` only hits `ConfigManager`/`FloodgateHook`/`CheckUtil`/`CooldownUtil`). Detection for “Bedrock player” when floodgate absent still works via `GeyserHook.UUID.enabled` (`000000` prefix `FloodgateHook.java:43`) and `GeyserHook.Prefix` (`FloodgateHook.java:46`) heuristics, gated by `GeyserHook.enabled` (`FloodgateHook.java:41`) and `GeyserHook.bedrockOnly` (`detection/CheckUtil.java:41,64`). | Global gating: `CheckUtil.isCheckAllowed` `bedrockOnly` branch (`CheckUtil.java:41`), `shouldSkipJavaWhenBedrockOnly` (`CheckUtil.java:40`), per-check `detectJava/detectBedrock` + `isCancelledCombat/Movement` branches via `FloodgateHook`. All checks that call `isCheckAllowed` or Bedrock helpers are affected. | No class needed; feature is config-driven. If Geyser-Spigot not installed and floodgate also absent, Bedrock detection falls back to UUID/prefix heuristics only when `GeyserHook.enabled=true`. If `GeyserHook.enabled=false`, `isBedrockPlayerWithoutCache` returns `false` immediately (`FloodgateHook.java:41`). |
| **floodgate** (`floodgate`) | `softdepend: floodgate` `plugin.yml:9` | Yes — `util/hook/plugin/FloodgateHook.java:20` + `HookUtil.java:8` | `HookUtil.isPlugin` cached with 1111 ms TTL via `Bukkit.getPluginManager().getPlugin` (`HookUtil.java:22-30`). `FloodgateHook.loadFloodgateApi` reflectively loads `org.geysermc.floodgate.api.FloodgateApi` (`FloodgateHook.java:157`), calls `getInstance`, resolves `isFloodgatePlayer(UUID)` + `getPlayer(UUID)` (`FloodgateHook.java:158-161`). `available` flag cleared on `ReflectiveOperationException` (`FloodgateHook.java:58,105,163`). Heuristic layers before reflection: `GeyserHook.UUID` + `GeyserHook.Prefix` (`FloodgateHook.java:42-49`). `isProbablyPocketEditionPlayer` further checks `getDeviceOs` and treats console/unknown/mobile device OS as PE (`FloodgateHook.java:97-102`). Caching of Bedrock result via `CooldownUtil.isBedrockPlayer` (`FloodgateHook.java:67,71`, `cooldown/CooldownUtil.java:149,170`). Config entry `Config.GeyserHook.Floodgate.enabled` (`FloodgateHook.java:36`). | Bedrock-aware leniency/gating: `CheckUtil.isCheckAllowed` (`CheckUtil.java:63,88`), `placeholder/PlaceholderConvertor.java:59` (`%edition%`), plus explicit `FloodgateHook.isBedrockPlayer/isProbablyPocketEditionPlayer/isCancelled*` calls in: `ItemSwapA.java:50`, `SortingA.java:62`, `AutoBotA.java:48,121`, `SkinBlinkerA.java:45`, `TimerA.java:44`, `ScaffoldB.java:47`, `BoatA.java:170`, `FastClimbA.java:78,125`, `FlightC.java:148` and `FlightC/Speed` stairs helpers `FloodgateHook.java:129-143`. Floodgate PE kills `KillAuraB/ReachA/ReachB` via `isCancelledCombat` (`FloodgateHook.java:116`) and weakens `SpeedB/StepA` stairs checks via `isCancelledMovement` (`FloodgateHook.java:125`). | If `floodgate` not installed or `GeyserHook.Floodgate.enabled=false`, `isBedrockPlayerWithoutCache` returns `false` after prefix/UUID checks (`FloodgateHook.java:51`), then falls back to prefix only (`FloodgateHook.java:62`). `isProbablyPocketEditionPlayer` returns `true` when floodgate unavailable (`FloodgateHook.java:81-83`) — i.e., any Bedrock-marked player is assumed PE. Reflection failures log `warning "Floodgate reflection hook failed..."` / `"Floodgate device lookup failed..."` and disable `available` (`FloodgateHook.java:59,106`). No exceptions propagate. |
| **ViaVersion** | `softdepend: ViaVersion` `plugin.yml:10` | **No** — no file `ViaVersionHook.java` exists; `glob util/hook/**` lists only `FloodgateHook` + `simplehook/{Aurelium,EliteMobs,EnchantsSquared,ExecutableItems,McMMO,ValhallaMMO,VeinMiner}`; `rg HookUtil\|...` finds zero `ViaVersion` hits in `src/main/java` | Declared `softdepend` for load-order only. No API call, no `Bukkit.getPluginManager().getPlugin("ViaVersion")` check, no version-translation code in repo. Protocol-version differences are handled by shading `multiversion` (`pom.xml:84`) + `VerUtil`/`VerPlayer`, not ViaVersion. | None directly. Ordering ensures LAC loads after ViaVersion so packets seen by LAC have already been translated upstream — no check code branches on ViaVersion. | Absence is fully transparent. No fallback, no logging. Same behavior as unrelated plugin. |
| **GSit** | `softdepend: GSit` `plugin.yml:11` | **No** — same exhaustive search as ViaVersion yields zero hits for `GSit` in `src/main/java` | Load-order softdepend only. No `GSitHook`, no `isPlugin("GSit")` check, no pose/seat exemption. | None. | No-op when absent. |
| **mcMMO** (`mcMMO`) | `softdepend: mcMMO` `plugin.yml:12` | Yes — `util/hook/plugin/simplehook/McMMOHook.java:6` | `HookUtil.isPlugin("mcMMO")` (`McMMOHook.java:11`) cached 1111 ms. Pure Bukkit material-name heuristic, no mcMMO API. `isPrevented(Material)` returns `true` iff name ends with `_LOG` or `_LEAVES` (`McMMOHook.java:13-15`). | Interaction/break/place fastbreak trio: `FastBreakA.java:105-107`, `BlockPlaceA.java:79-81`, `BlockBreakA.java:79-81`, `BlockBreakB.java:83-85` — `AureliumSkillsHook.isPrevented \|\| VeinMinerHook.isPrevented \|\| McMMOHook.isPrevented(block.getType())` early-return prevents false flags when those plugins imply tool-aided break. | If `mcMMO` not installed, `isPrevented` returns `false` (`McMMOHook.java:11`). Checks run normally. |
| **ValhallaMMO** (`ValhallaMMO`) | `softdepend: ValhallaMMO` `plugin.yml:13` | Yes — `util/hook/plugin/simplehook/ValhallaMMOHook.java:5` (minimal `isPluginInstalled()` wrapper) | `HookUtil.isPlugin("ValhallaMMO")` (`ValhallaMMOHook.java:10`) | `CriticalsA.java:44,106` — extra crit handling when installed; `FlightA.java:233` — altitude leniency; `SpeedC.java:79,188,230,238,246` — multiple early `return` bypasses when installed (disables those SpeedC sub-checks entirely). | When absent, returns `false` → no bypass added, all those sub-checks run. |
| **VeinMiner** (`VeinMiner`) | `softdepend: VeinMiner` `plugin.yml:14` | Yes — `util/hook/plugin/simplehook/VeinMinerHook.java:8` | `HookUtil.isPlugin("VeinMiner")` + main-hand tool heuristic (`VeinMinerHook.java:13-23`): returns `true` iff main-hand type ends with `_AXE/_HOE/_PICKAXE/_SHOVEL` or is `SHEARS`. No VeinMiner API. | Same trio as mcMMO: `FastBreakA.java:106`, `BlockPlaceA.java:80`, `BlockBreakA.java:80`, `BlockBreakB.java:84` | Absent → `false`, no suppression. |
| **AureliumSkills** (`AureliumSkills`) | `softdepend: AureliumSkills` `plugin.yml:15` | Yes — `util/hook/plugin/simplehook/AureliumSkillsHook.java:8` | `HookUtil.isPlugin("AureliumSkills")` + shovel heuristic (`AureliumSkillsHook.java:13-20`): `true` iff `VerPlayer.getItemInMainHand` is non-empty and type ends with `_SHOVEL`. No AureliumSkills API. | Same quartet: `FastBreakA.java:105`, `BlockPlaceA.java:79`, `BlockBreakA.java:79`, `BlockBreakB.java:83` | Absent → `false`. |
| **ExecutableItems** (`ExecutableItems`) | `softdepend: ExecutableItems` `plugin.yml:16` | Yes — `util/hook/plugin/simplehook/ExecutableItemsHook.java:10` | `HookUtil.isPlugin("ExecutableItems")` (`ExecutableItemsHook.java:15`). `isPrevented(CheckName,Player)` reads main-hand `ItemStack`/`ItemMeta` (`ExecutableItemsHook.java:18-21`). `flag` true only if `checkName.type==COMBAT` + sword/axe **or** `INTERACTION` + pickaxe/shovel/axe (`ExecutableItemsHook.java:25-30`). Then returns `true` iff display name contains `§` (`ExecutableItemsHook.java:35`) **or** lore contains any non-empty line (`ExecutableItemsHook.java:37-41`). Heuristic for EI custom items. | Global gating in `CheckUtil.isCheckAllowed` for sync checks only (`CheckUtil.java:92-94`): if `ExecutableItemsHook.isPrevented(checkName, player)` → check skipped. Affects every combat/interaction check that routes through `isCheckAllowed`. | Absent → `false`; gating disabled. Async path intentionally skips this hook (`!async` guard `CheckUtil.java:92`). |
| **EnchantsSquared** (`EnchantsSquared`) | `softdepend: EnchantsSquared` `plugin.yml:17` | Yes — `util/hook/plugin/simplehook/EnchantsSquaredHook.java:10` | `isPluginInstalled()` true if `isPlugin("EnchantsSquared") \|\| isPlugin("EnchantsPlus")` (`EnchantsSquaredHook.java:12-14`). `hasEnchantment(Player, String...)` iterates full inventory (`EnchantsSquaredHook.java:19`), reads `ItemMeta.getLore()` and `String.contains(enchantment)` substring match (`EnchantsSquaredHook.java:26-28`). Aliases `EnchantsPlus` covered here — see next row. | Broad — lore-based enchants lift limits or suppress: <br>`ItemSwapA.java:79` (`Telekinesis`), `SortingA.java:113` (`Telekinesis`), `GhostBreakA.java:91`, `BlockBreakA/B.java:84,88`, `FastBreakA.java:185` (`Excavation/Deforestation/Harvesting`), `BlockPlaceA.java:84`, `BlockPlaceB.java:80`, `AirPlaceA.java:105` (`Illuminated/Harvesting`), `VelocityA.java:171` (`Steady/Burden`), plus movement via `MovementCheck.getPlayersForEnchantsSquared/isEnchantsSquaredImpact` (`MovementCheck.java:94,105`) checking `Rope Dart/Shockwave` radius and movement branches in `JumpB.java:193`, `FlightA.java:209`, `FlightB.java:248`, `FlightC.java:162,183`, `NoFallA.java:200`, `SpeedA/B/C.java:188,192,171,178`, `FlightB/C` `Burden` scaling, etc. | If neither `EnchantsSquared` nor `EnchantsPlus` installed, every helper returns `false`/empty set (`EnchantsSquaredHook.java:17,95-96,106-107`): checks run at base tuning with no enchantment compensation. |
| **EnchantsPlus** (alias, no descriptor) | **Not** in `plugin.yml` — code alias only | Same hook as above — `EnchantsSquaredHook.java:13,17` | Same dual-name detection: `isPlugin("EnchantsSquared") \|\| isPlugin("EnchantsPlus")`. No separate hook file. | Identical to EnchantsSquared row. | Presence of either satisfies hook; both absent → no effect. |
| **EliteMobs** | **Not** in `softdepend` (verified `plugin.yml:6-17` has no `EliteMobs` entry) | Yes — `util/hook/plugin/simplehook/EliteMobsHook.java:5` exists despite missing descriptor | `HookUtil.isPlugin("EliteMobs")` cached (`EliteMobsHook.java:10`). Minimal `isPluginInstalled()` only. | `ReachA.java:81` — when installed and target not on ground and not a flying entity type, `maxReach += 0.35` (vs `0.25` fallback with liquid-block check `ReachA.java:83-86`). Only horizontal reach. | Absent → `false`; tighter reach (`+0.25` or liquid check) applies. Missing descriptor means LAC does not enforce load-after EliteMobs, but runtime `isPlugin` poll covers late enable. |
| **Paper** | No descriptor — platform probe | Yes — `util/hook/server/paper/PaperUtil.java:3` | Static probe `Class.forName("com.destroystokyo.paper.ParticleBuilder")` in static initializer (`PaperUtil.java:9`), stored `boolean paper`. No plugin lookup. | `util/player/entities/NearbyEntitiesUtil.java:22` — branch changes entity-lookup path on non-Paper ≤1.8. `util/player/brand/ClientBrandRecognizer.java:12` gates brand logic on `PaperUtil.isPaper()`. | Non-Paper → `false`; code uses Spigot fallbacks. No warning. |
| **Folia** | No descriptor — platform probe (descriptor has `folia-supported: true` `plugin.yml:5`, not a dependency) | Yes — `util/hook/server/folia/FoliaUtil.java:18` (reflective scheduler bridge) | `loadFoliaUtil()` at startup `Main.java:93` probes `Class.forName("io.papermc.paper.threadedregions.RegionizedServer")` (`FoliaUtil.java:45`). If present, reflectively resolves `getGlobalRegionScheduler/getAsyncScheduler/getRegionScheduler`, `Entity.getScheduler`, `Entity.teleportAsync`, `isOwnedByCurrentRegion` overloads (`FoliaUtil.java:51-63`), caches method handles + scheduler instances (`FoliaUtil.java:64-71`). Any `ReflectiveOperationException` → `folia=false` + `clearMethods()` + `warning "Folia detected but scheduler API is unavailable..."` (`FoliaUtil.java:72-75`). `isFolia()` gates all threading. | Extensive — scheduler (`util/scheduler/gamescheduler/FoliaScheduler.java:11`, `util/scheduler/Scheduler.java:25` chooses `FoliaScheduler` vs `BukkitScheduler`), teleport (`FoliaUtil.teleportPlayer/teleportPlayerAsync` `FoliaUtil.java:135,150` used in `command/LACCommand.java:242`, `ViolationHandler.java:162`, `event/playermove/LACAsyncPlayerMoveEvent.java:326`, `movement/speed/SpeedE.java:126`), region ownership (`FoliaUtil.isOwnedByCurrentRegion*` `FoliaUtil.java:167` used in `player/LACPlayer.java:194`, `event/playermove/blockcache/*`, `event/playermove/LACAsyncPlayerMoveEvent.java:82`, `input/LACInputDispatcher.java:139`, `util/async/AsyncUtil.java:32`, `util/detection/specific/BlockUtil.java:57`, etc.), event async flag (`super(!FoliaUtil.isFolia())` in `LACAsync*Event` classes), `Buffer.BUFFERS` map type (`HashMap` vs `ConcurrentHashMap` `check/buffer/Buffer.java:55`), `CooldownUtil`, `ExternalNPCUtil`, `NearbyEntitiesUtil` branching. | Non-Folia → `false`; all code follows Bukkit scheduler / `Bukkit.isPrimaryThread()` fallbacks (`FoliaUtil.java:138,168,181`, etc.). Missing Folia is the normal path; no special behavior. On Folia, failing to own region causes blocks/entities/world lookups to return null/abort gracefully (e.g., `BlockMaterialCache.java:205`, `BlockCache.java:96`). |

---

## 3. Key Accuracies

### 3.1 ViaVersion / GSit have no hook class
Exhaustive `glob util/hook/**` + `rg ViaVersion|GSit` under `src/main/java` returns zero hits beyond `plugin.yml:10-11`. They are ordering-only `softdepend` entries to make LAC load after those plugins. Document above states this accurately.

### 3.2 EliteMobs is the opposite mismatch
Hook class `EliteMobsHook.java:5` exists and is called from `ReachA.java:81`, but `plugin.yml:6-17` contains no `EliteMobs` entry. Table above documents “descriptor: none / hook: yes”.

### 3.3 PacketEvents lifecycle ownership
External lifecycle belongs to **PacketEvents** itself (`PacketEvents.getAPI()` `isLoaded/isInitialized/isTerminated`). LAC owns only the listener/provider (`PacketEventsInputProvider.java:33`, `input/LACInputEngine.java:26`). See PacketEvents row for the sequence.

### 3.4 NMS does not require PacketEvents
`input/provider/nms/NmsInputProvider.java:21` extends `LightInjector` (Netty channel), selected via `LACInputMode.NMS` (`input/model/LACInputMode.java:7-8`). `LACInputEngine` instantiates it directly (`LACInputEngine.java:34`) vs PacketEvents reflectively (`LACInputEngine.java:26`). `PacketEvents` is `provided` scope in `pom.xml:93-98`, so both paths compile without requiring the PacketEvents plugin at runtime.

### 3.5 Detection helpers
- `HookUtil.isPlugin(String)` `HookUtil.java:22` — `HashMap<String,HPlugin>` with `lastCheck` and 1111 ms stale window; `Bukkit.getPluginManager().getPlugin(name)!=null`.
- `FloodgateHook` layers: `GeyserHook.UUID/Prefix` config → `PluginManager.getPlugin("floodgate")` → reflective `FloodgateApi` (`FloodgateHook.java:40-63,146-167`).
- `EnchantsSquaredHook` dual-name: `plugin.yml` only has `EnchantsSquared`; `EnchantsPlus` is an alias handled purely in code (`EnchantsSquaredHook.java:13,17`).
- `PaperUtil` / `FoliaUtil` are runtime class probes, not plugin lookups (`PaperUtil.java:9`, `FoliaUtil.java:45`).

### 3.6 Missing-plugin behavior is intentionally silent
No hook throws when the external is absent (except PacketEvents when PACKET mode is explicitly requested). All others return `false`/empty, causing checks to run at their vanilla-tuned thresholds.

---

## 4. File Map

| File | Role |
|---|---|
| `src/main/resources/plugin.yml:6` | Descriptor softdepends |
| `src/main/java/me/vekster/lightanticheat/util/hook/plugin/HookUtil.java:8` | Cache + `isPlugin` helper |
| `src/main/java/me/vekster/lightanticheat/util/hook/plugin/FloodgateHook.java:20` | Bedrock/PE detection (reflection + heuristics) |
| `src/main/java/me/vekster/lightanticheat/util/hook/plugin/simplehook/AureliumSkillsHook.java:8` | Shovel heuristic |
| `src/main/java/me/vekster/lightanticheat/util/hook/plugin/simplehook/VeinMinerHook.java:8` | Tool heuristic |
| `src/main/java/me/vekster/lightanticheat/util/hook/plugin/simplehook/McMMOHook.java:6` | `_LOG/_LEAVES` heuristic |
| `src/main/java/me/vekster/lightanticheat/util/hook/plugin/simplehook/ValhallaMMOHook.java:5` | Presence flag |
| `src/main/java/me/vekster/lightanticheat/util/hook/plugin/simplehook/EliteMobsHook.java:5` | Presence flag (no descriptor) |
| `src/main/java/me/vekster/lightanticheat/util/hook/plugin/simplehook/EnchantsSquaredHook.java:10` | Lore scan + dual-name |
| `src/main/java/me/vekster/lightanticheat/util/hook/plugin/simplehook/ExecutableItemsHook.java:10` | Combat/interaction + custom-item heuristic |
| `src/main/java/me/vekster/lightanticheat/util/hook/server/paper/PaperUtil.java:3` | Paper probe |
| `src/main/java/me/vekster/lightanticheat/util/hook/server/folia/FoliaUtil.java:18` | Folia scheduler bridge |
| `src/main/java/me/vekster/lightanticheat/input/provider/packetevents/PacketEventsInputProvider.java:33` | PACKET input provider |
| `src/main/java/me/vekster/lightanticheat/input/provider/nms/NmsInputProvider.java:21` | NMS fallback (no PacketEvents) |
| `src/main/java/me/vekster/lightanticheat/input/LACInputEngine.java:26` | Reflective PacketEvents creation |
| `src/main/java/me/vekster/lightanticheat/input/model/LACInputMode.java:6` | `PACKET` / `NMS` |
| `src/main/java/me/vekster/lightanticheat/util/detection/CheckUtil.java:51` | Bedrock/combat gating hub |
