# Packet / Inventory / Player Checks

> 12 checks — 8 packet + 2 inventory + 2 player.
> Sources: `CheckName.java:50-61`, shared bases `check/checks/packet/PacketCheck.java:13`, `check/checks/inventory/InventoryCheck.java:6`, `check/checks/player/PlayerCheck.java:6`, config `util/config/ConfigManager.java:297-319` + `src/main/resources/config.yml:922-1114`, detection gates `util/detection/CheckUtil.java:43-86`.

## Shared infrastructure

| Concern | Detail |
|---|---|
| Bases | `PacketCheck extends Check` — `limitPackets(char,int,long,int,int):13-39`, `flag(Player,LACPlayer):58-62` (`LACPlayerManager.execute` → `callViolationEvent`), `getLacPlayer(UUID):52-56`. `InventoryCheck extends Check` — no extra helpers. `PlayerCheck extends Check` — no extra helpers. All extend `CheckUtil` (`PassableUtil`). |
| Config provider | `ConfigManager.loadCheck(CheckSetting)` path `checks.<type>.<group>.<group>_<check>` lowercased (`ConfigManager.java:299-303`). Example: `checks.movement.flight.flight_a.enabled` (type `MOVEMENT` / group `Flight` / check `A`). Fields per check: `enabled`, `punishment.punishable`, `punishment.punishment-vio`, `punishment.commands`, `setback.setback`, `setback.setback-vio`, `detection.min-tps`, `detection.max-ping`, `detection.java`, `detection.bedrock`. File: `src/main/resources/config.yml:183-1114`. |
| Global exemptions (`CheckUtil.isCheckAllowed:76-86`) | `enabled`, `GeyserHook.bedrockOnly` (skips non-bedrock when true), `detectJava`/`detectBedrock`, `lightanticheat.bypass.*` + `lightanticheat.bypass.<check>` (`Permission` section), `DetectionStatus != ENABLED` when `Api.enabled`, `TikThreshold`/`TPS < min-tps`, `FloodgateHook.isCancelledCombat`, `ExecutableItemsHook` (sync lane), `ping > max-ping`, `ignoreTimeOnJoin/ignoreTimeOnTeleport` (`LagProtection`). |
| Event bus | Packet/timer/skin: `LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE \| ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL)` (`LACEventType`, `LACEventPriority`, `LACMovementRequirement`). Inventory: Bukkit `InventoryClickEvent` / `InventoryOpenEvent` via `Bukkit.getPluginManager().registerEvents` (`Check.java:63`). |
| Violation callsite | Packet: `callViolationEvent` (direct or via `Scheduler.runTask(true)`) or `flag()` (BadPacketsA/B) or `callViolationEventIfRepeat(buffer, Main.getBufferDurationMils()-1000)` (TimerB E/F, ItemSwapA). Inventory: `callViolationEvent(event)` or `callViolationEventIfRepeat`. Player: `callViolationEvent` / `callViolationEvent` after `Scheduler.runTaskLater(1)`. Setback/punishment dispatch via `LACViolationEvent` listeners, not inside the check itself. |
| Hooks referenced | `FloodgateHook.isBedrockPlayer / isProbablyPocketEditionPlayer / isCancelledCombat`, `EnchantsSquaredHook.hasEnchantment("Telekinesis")`, `ExecutableItemsHook`, `CooldownUtil`, `TPSCalculator`, `VerIdentifier`/`LACVersion`. |

---

## Packet — 8 checks (`CheckType.PACKET`)

### 1. MorePacketsA — `PacketRate`

| Field | Value |
|---|---|
| Display name | `MorePacketsA` (`CheckName.MOREPACKETS_A:50` — title `MorePackets_A`, `title.replace("_","")` → `MorePacketsA`) |
| Enum description | `PacketRate` |
| High-level signal | Generic packet-rate flood. Dual window: A long `1600 ms` (`335 pkts ≈ ceil(ceil(20*1.6*5.5)*1.9)` ×3 repeats: `PacketCheck.limitPackets('A',1600,335,3)`) and B short `800 ms` (`137 pkts ≈ ceil(ceil(20*0.8*4.5)*1.9)` ×5 repeats: `limitPackets('B',800,137,5)`) — `morepackets/MorePacketsA.java:23-24,42,64`. Counts **all** `LACAsyncPacketReceiveEvent` packets. Violates via `Scheduler.runTask(true) → callViolationEvent`. |
| Event / input lane | `LACEventBus LACEventType.ASYNC_PACKET_RECEIVE` ×2 handlers `onAsyncPacketReceiveA/B:28-29` (no `LACPacketType` filter). Async lane. |
| Exemptions | Global `isCheckAllowed(.., true)` only + per-method cooldown: `lastFlagTime <1500 ms` (A) / `<700 ms` (B) (`MorePacketsA.java:45,67`). `PacketCheck.limitPackets` internal repeat counter must reach threshold before first flag. |
| Setback | `setback: false`, `setback-vio: 1` (`config.yml:932-934`) — punishment-only; `CheckSetting.setback/setbackVio` read at `ConfigManager.java:316-317`. |
| Config path | `checks.packet.morepackets.morepackets_a` |

### 2. MorePacketsB — `Nuker`

| Field | Value |
|---|---|
| Display name | `MorePacketsB` (`CheckName.MOREPACKETS_B:51` — title `MorePackets_B`) |
| Enum description | `Nuker` |
| High-level signal | Nuker / `BLOCK_DIG` spam. `limitPackets('A',667,400,3)` — 400 dig packets per `667 ms` sustained 3 windows (`morepackets/MorePacketsB.java:40`). |
| Event / input lane | `LACEventType.ASYNC_PACKET_RECEIVE` handler `onAsyncPacketReceive:26` filtered `event.getPacketType() != LACPacketType.BLOCK_DIG → return:30-31`. |
| Exemptions | `isCheckAllowed(.., true)` only. No extra bypass. |
| Setback | `setback: false`, `setback-vio: 1` (`config.yml:947-949`) |
| Config path | `checks.packet.morepackets.morepackets_b` |

### 3. TimerA — `MovementTimer`

| Field | Value |
|---|---|
| Display name | `TimerA` (`CheckName.TIMER_A:52`) |
| Enum description | `MovementTimer` |
| High-level signal | Timer / speedhack via `FLYING` packet frequency. Sliding 1000 ms window with balancer: `packets` vs `packetsBalancer/balancerTime` (`timer/TimerA.java:74-92`). Flag when `packets > threshold` (28 on >1.8, 35 on ≤1.8: `VerIdentifier.getVersion().isNewerThan(V1_8)`) after draining `balancerTime`; requires 3 local flags (`localFlags>2`) inside rolling 2 s windows before global `lastFlagTime >2000 ms` + `flag()`. |
| Event / input lane | `ASYNC_PACKET_RECEIVE` filtered `LACPacketType.FLYING:38` + `ASYNC_PLAYER_MOVE` (`LACMovementRequirement.POSITION`) `onAsyncMovement:31` to set `buffer.moved=true:99` (required before counting). |
| Exemptions | `isCheckAllowed(..,true)`, `FloodgateHook.isProbablyPocketEditionPlayer:44`, `joinTime<2000 ms` (or `<12000 ms` on ≤1.8: `53`), `!moved` skip, vehicle alternating skip (`skipVehiclePacket` toggle: `56-59`), wind-charge `lastWindCharge<3000`/`lastWindChargeReceive<1000` alternating skip: `62-67`, bootstrap guard `lastNonExistingFieldTime ≤2000 ms` prevents flag: `104`, `joinTime≤10s` suppress: `89`, `balancerTime>0` drains instead of flagging, `localFlagTime`/`lastFlagTime` windows. |
| Setback | `setback: false`, `setback-vio: 2` (`config.yml:963-965`) |
| Config path | `checks.packet.timer.timer_a` |

### 4. TimerB — `Timer`

| Field | Value |
|---|---|
| Display name | `TimerB` (`CheckName.TIMER_B:53`) |
| Enum description | `Timer` |
| High-level signal | Timer via **movement-event** rate (not raw packets). Six parallel limiters `timer/TimerB.java:44-147`: A `1600 ms /176(20*1.6*5.5) ×3`, B `1200/120(20*1.2*5.0)×4`, C `800/72(20*0.8*4.5)×5`, D `571/46(20*0.571*4.1)×7` all `callViolationEvent`; E `960/176×3`, F `480/72×5` use `callViolationEventIfRepeat(Main.getBufferDurationMils()-1000)`. All share `PacketCheck.limitPackets` per prefix `A-F`. |
| Event / input lane | `LACEventType.ASYNC_PLAYER_MOVE` ×6 (`onAsyncMovementA-F:26-31`) — input lane is movement ticks, not packets. |
| Exemptions | `isCheckAllowed(..,true)` per method + `limitPackets` repeat gate + `lastFlagTime` `1500 ms` (A-D) / `900 ms` (E/F) cooldown (`TimerB.java:47,67,87,107,129,149`). E/F additionally gated by repeat buffer `MissedMethodFlag` pattern (`Check.java:88-99`). No Floodgate bypass here. |
| Setback | `setback: false`, `setback-vio: 2` (`config.yml:978-980`) |
| Config path | `checks.packet.timer.timer_b` |

### 5. BadPacketsA — `Protocol`

| Field | Value |
|---|---|
| Display name | `BadPacketsA` (`CheckName.BADPACKETS_A:54`) |
| Enum description | `Protocol` |
| High-level signal | Self-damage / self-interaction: `USE_ENTITY` targeting own `entityId` (`badpackets/BadPacketsA.java:36` — `event.getEntityId()==player.getEntityId()` → `flag()`). |
| Event / input lane | `ASYNC_PACKET_RECEIVE` filtered `LACPacketType.USE_ENTITY:28`. Calls `PacketCheck.flag()` → `LACPlayerManager.execute → callViolationEvent`. |
| Exemptions | `isCheckAllowed(..,true)` only. |
| Setback | `setback: false`, `setback-vio: 4` (`config.yml:994-996`) |
| Config path | `checks.packet.badpackets.badpackets_a` |

### 6. BadPacketsB — `Impassible`

| Field | Value |
|---|---|
| Display name | `BadPacketsB` (`CheckName.BADPACKETS_B:55`) |
| Enum description | `Impassible` |
| High-level signal | Impossible entity ID: `USE_ENTITY` with `entityId <0` (`BadPacketsB.java:36` → `flag()`). |
| Event / input lane | `ASYNC_PACKET_RECEIVE` filtered `USE_ENTITY:28`. |
| Exemptions | `isCheckAllowed(..,true)` only. |
| Setback | `setback: false`, `setback-vio: 19` (`config.yml:1009-1011`) |
| Config path | `checks.packet.badpackets.badpackets_b` |

### 7. BadPacketsC — `Impassible` (SteerVehicle)

| Field | Value |
|---|---|
| Display name | `BadPacketsC` (`CheckName.BADPACKETS_C:56`) |
| Enum description | `Impassible` (javadoc: *Impassible SteerVehicle packet*) |
| High-level signal | `STEER_VEHICLE` outside any vehicle and without nearby entities / movement, re-validated over 2 ticks (async 1 + async 1) before `flag()` (`BadPacketsC.java:49-72`). Intended to catch vehicle-packet spoof. |
| Event / input lane | `ASYNC_PACKET_RECEIVE` filtered `STEER_VEHICLE:32` — deferred `Scheduler.runTaskLaterAsynchronously(1)` → `runTaskLaterAsynchronously(1)` → `Scheduler.runTask(false) → flag()`. |
| Exemptions | `isCheckAllowed(..,true)`, `joinTime<2000 ms:40`, `isInsideVehicle \| lastInsideVehicle<500 ms:43,52,61`, `getNearbyEntitiesAsync(NEARBY).nonEmpty` skip: `45,54,63`, `distance(FIRST, FROM)==0` (no movement) skip: `46,55,64` — all re-checked at each delayed stage. |
| Setback | `setback: false`, `setback-vio: 14` (`config.yml:1024-1026`) |
| Config path | `checks.packet.badpackets.badpackets_c` |

### 8. BadPacketsD — `ArmAnimation`

| Field | Value |
|---|---|
| Display name | `BadPacketsD` (`CheckName.BADPACKETS_D:57`) |
| Enum description | `ArmAnimation` (javadoc: *Impassible SetCreativeSlot packet*) |
| High-level signal | `SET_CREATIVE_SLOT` while not in `CREATIVE` (`BadPacketsD.java:42-50`). Delayed 1 tick verification before `flag()`. |
| Event / input lane | `ASYNC_PACKET_RECEIVE` filtered `SET_CREATIVE_SLOT:30` → `Scheduler.runTaskLater(1) → flag()`. |
| Exemptions | `isCheckAllowed(..,true)`, `player.getGameMode()==CREATIVE` skip + `lastGamemodeChange<500 ms` skip (checked immediately `38-40` and again after delay `46-48`), `!isOnline \| leaveTime!=0` guard. |
| Setback | `setback: false`, `setback-vio: 9` (`config.yml:1039-1041`) |
| Config path | `checks.packet.badpackets.badpackets_d` |

---

## Inventory — 2 checks (`CheckType.INVENTORY`)

### 9. SortingA — `InstantSorting`

| Field | Value |
|---|---|
| Display name | `SortingA` (`CheckName.SORTING_A:58`) |
| Enum description | `InstantSorting` |
| High-level signal | Instant inventory sorting: 8 distinct-slot clicks within a 22 ms window while a chest/inventory is open (`sorting/SortingA.java:80-101` — `startTime` window 22 ms, `clicks` counts only when `lastSlot1 != slot` with `lastSlot2` dedup `85-97`). Flags via `callViolationEvent(player,lacPlayer,event)` with 500 ms `lastFlag` throttle. |
| Event / input lane | Bukkit `InventoryClickEvent` (`@EventHandler onInventoryClick:48`) + `InventoryOpenEvent` (`onInventoryOpen:121`) to capture chest `getLocation()` → `lastOpenChest` when block is `CHEST/TRAPPED_CHEST:142` (reflection `getLocation()` with `outdated` fallback `134`). |
| Exemptions | `isExternalNPC`, `GameMode.CREATIVE:54`, `isCheckAllowed(..,false)` sync, `FloodgateHook.isProbablyPocketEditionPlayer:62`, `IGNORED_ACTIONS {CLONE_STACK,MOVE_TO_OTHER_INVENTORY,NOTHING,UNKNOWN}:65`, `cursor==AIR` in non-survival/adventure: `69-71`, `inventoryType NOT IN {PLAYER,CRAFTING} && lastOpenChest>4000 ms` skip: `76`, same-slot debounce (lastSlot1/lastSlot2), `CRAFTING` type + target `CRAFTING_TABLE` within 10 blocks skip: `107-111`, `EnchantsSquaredHook Telemetry "Telekinesis":113`. |
| Setback | `setback: false`, `setback-vio: 2` (`config.yml:1057-1059`) |
| Config path | `checks.inventory.sorting.sorting_a` |

### 10. ItemSwapA — `WhileWalking`

| Field | Value |
|---|---|
| Display name | `ItemSwapA` (`CheckName.ITEMSWAP_A:59`) |
| Enum description | `WhileWalking` |
| High-level signal | Inventory click while moving (sprint/sneak/swim) and position actually changing (`swapping/ItemSwapA.java:57-65` — requires `isSprinting&&sprintingTicks≥2+lag` OR same for sneaking/swimming, with `lagCompensationTicks=ceil(ping/50)`, plus `distance(FROM,FIRST) ≥ Float.MIN_VALUE*5`). Burst gated: `lastFlag 450 ms`, `flags>1` (and `flags>2` when `ping>250`) then `callViolationEventIfRepeat(buffer, bufferDuration-1000)`. |
| Event / input lane | Bukkit `InventoryClickEvent` on `onInventoryClick:38` (sync, via `LACPlayerManager.execute(player,false)`). |
| Exemptions | `isExternalNPC`, `isCheckAllowed`, `FloodgateHook.isBedrockPlayer:50`, `IGNORED {NOTHING,UNKNOWN}:53`, not-moving-state skip `57-60`, stationary history skip `63-65`, `lastFlag<450 ms:69`, `flags` thresholds `74-77`, `EnchantsSquared Telekinesis:79`. |
| Setback | `setback: false`, `setback-vio: 3` (`config.yml:1073-1075`) |
| Config path | `checks.inventory.itemswap.itemswap_a` |

---

## Player — 2 checks (`CheckType.PLAYER`)

### 11. AutoBotA — `AutoBot`

| Field | Value |
|---|---|
| Display name | `AutoBotA` (`CheckName.AUTOBOT_A:60`) |
| Enum description | `AutoBot` |
| High-level signal | Two signals in one check (`autobot/AutoBotA.java:40-193`): **Head** — integer `yaw`/`pitch` rotation multiple of `90°` (`change % 90==0 && %360!=0`) across `from→to` horizontal and all `HistoryElement` entries (`integerHeadRotation:98-107`) — flags after 4th event inside 12/10/8 s cascading windows + 1000 ms throttle; **Pathing** — horizontal `x/z` distance ratio exactly `0.25 / 0.333333 / 0.5 / 0.666666 / 0.75 / 1.0` (ε 1e-6) on flat (`y` equal), same-world, `distance≥0.11` movement (`getRatio:195-206`) — flags via `runTaskLater(1)` re-checking ground. |
| Event / input lane | `LACEventType.ASYNC_PLAYER_MOVE` — `onHeadRotation` with `LACMovementRequirement.ROTATION:36` and `onMovement` with `POSITION_AND_ROTATION:37`. |
| Exemptions | Head: `isCheckAllowed(..,true)`, `Floodgate Bedrock`, `join/teleport/worldChange/respawn ≤1000 ms:52-56`, `lastRotationFlagTime<1000` + 3-window cascade. Pathing: `violations==0 && CooldownUtil.isSkip(215):114-116`, `isCheckAllowed`, Bedrock, `!withinBlocksPassable` skip, `isFlying/InsideVehicle/Gliding/Riptiding` + `flyingTicks≥-30/climbing≥-2/gliding≥-40/riptiding≥-50`, timers: `lastInsideVehicle/inWater≤150`, `lastKnockback≤300`, `lastKnockbackNotVanilla≤1000`, `lastWasFished≤400`, `lastTeleport/Respawn/EntityVeryNearby≤500`, `Block/EntityExplosion≤1000`, `SlimeBlock>3000`, `HoneyBlock>1500`, `lastWasHit≤150/Damaged≤50/Kb≤350/AirKb≤700/StrongKb≤1500/StrongAirKb≤15s:132-142`, 3-history `onGround.towardsFalse` required: `145-150`, `getCollisionBlockLayer` passability + UP face (`153-154`), `yaw%5==0` both skip: `156-158`; re-checks `onGround(FROM).towardsFalse` after 1 tick before flag. |
| Setback | `setback: false`, `setback-vio: 2` (`config.yml:1091-1093`) |
| Config path | `checks.player.autobot.autobot_a` |

### 12. SkinBlinkerA — `SkinBlinker`

| Field | Value |
|---|---|
| Display name | `SkinBlinkerA` (`CheckName.SKINBLINKER_A:61`) |
| Enum description | `SkinBlinker` |
| High-level signal | Skin-parts blinking: counts `CLIENT_INFORMATION` packets while actually rotating/moving (`skinblinker/SkinBlinkerA.java:34-74` — increments `buffer.packets` when `lastMovement<333 ms`, then per `2000 ms` window checks `packets≥12` twice (`flags≥2`) while `lastMovement<1800 ms` → `Scheduler.runTask(true)→callViolationEvent`). |
| Event / input lane | `ASYNC_PACKET_RECEIVE` filtered `LACPacketType.CLIENT_INFORMATION:35` + `ASYNC_PLAYER_MOVE POSITION_AND_ROTATION` `onMovement:31` to maintain `buffer.lastMovement` (only when yaw change >5° or pitch >0.5° and `distance!=0:81-89`). |
| Exemptions | `isCheckAllowed(..,true)`, `Floodgate Bedrock:45`, `startTime` window 2000 ms, `packets<12` resets `flags`, `flags<2` defers, `lastMovement≥1800 ms` suppresses flag; movement lane ignores micro-rotations and zero-distance events. |
| Setback | `setback: false`, `setback-vio: 1` (`config.yml:1107-1109`) |
| Config path | `checks.player.skinblinker.skinblinker_a` |

---

## Config defaults (all 12)

All packet/inventory/player checks ship `enabled: true`, `punishment.commands: ["kick %name% %check%"]`, `detection.min-tps: 5.0`, `max-ping: 10000`, `java: true`, `bedrock: true` in `src/main/resources/config.yml`. Punishment thresholds (`punishment-vio` / `setback-vio`) per check:

| Check | `punishment-vio` | `setback-vio` |
|---|---|---|
| `morepackets_a` | 2 | 1 |
| `morepackets_b` | 2 | 1 |
| `timer_a` | 3 | 2 |
| `timer_b` | 3 | 2 |
| `badpackets_a` | 5 | 4 |
| `badpackets_b` | 20 | 19 |
| `badpackets_c` | 15 | 14 |
| `badpackets_d` | 10 | 9 |
| `sorting_a` | 3 | 2 |
| `itemswap_a` | 4 | 3 |
| `autobot_a` | 3 | 2 |
| `skinblinker_a` | 2 | 1 |

Setback is `false` for all 12 (discouraged — see `config.yml:200-204` note — punishment commands preferred).

## Bypass & gating summary

Per-check bypass `lightanticheat.bypass.<apiName>` (e.g. `lightanticheat.bypass.morepackets_a`) requires `permission.per-check-bypass-permission: true` and `disable-all-bypass-permissions: false`. Global `lightanticheat.bypass` applies otherwise. `geyser-hook.bedrock-only: true` (default) short-circuits all checks for non-Bedrock before `detectJava/detectBedrock` is evaluated.

## References

- `src/main/java/me/vekster/lightanticheat/check/CheckName.java:50-61` — titles + descriptions
- `src/main/java/me/vekster/lightanticheat/check/Check.java:71-101` — `isCheckAllowed`, `callViolationEvent*`
- `src/main/java/me/vekster/lightanticheat/util/detection/CheckUtil.java:43-86` — detection gates
- `src/main/java/me/vekster/lightanticheat/util/config/ConfigManager.java:297-319` — path derivation
- `src/main/resources/config.yml:922-1114` — defaults for all 12
- Packet: `check/checks/packet/morepackets/*.java`, `timer/*.java`, `badpackets/*.java`
- Inventory: `check/checks/inventory/sorting/SortingA.java:33`, `swapping/ItemSwapA.java:25`
- Player: `check/checks/player/autobot/AutoBotA.java:29`, `skinblinker/SkinBlinkerA.java:23`
