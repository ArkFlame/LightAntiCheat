# Interaction Checks

Base class: `src/main/java/me/vekster/lightanticheat/check/checks/interaction/InteractionCheck.java:11` (`InteractionCheck extends Check`). Provides `isScaffoldPlacement(Player, Block, BlockAgainst)` (`InteractionCheck.java:16`) — true only when 2 blocks below placed block are air, and both block + blockAgainst are within 0.45-radius down-blocks of the player (used by `ScaffoldA`/`ScaffoldB`).

All 11 checks use `CheckName` enum `src/main/java/me/vekster/lightanticheat/check/CheckName.java:39-49` with `CheckType.INTERACTION`.

| # | Title (display name) | Enum `description` | Config path |
|---|---------------------|---------------------|-------------|
| 1 | AirPlaceA | `AirPlaceA` | `checks.interaction.airplace.airplace_a` |
| 2 | FastPlaceA | `FastPlaceA` | `checks.interaction.fastplace.fastplace_a` |
| 3 | BlockPlaceA | `Rotation` | `checks.interaction.blockplace.blockplace_a` |
| 4 | BlockPlaceB | `Reach` | `checks.interaction.blockplace.blockplace_b` |
| 5 | GhostBreakA | `ThroughBlock` | `checks.interaction.ghostbreak.ghostbreak_a` |
| 6 | FastBreakA | `MiningSpeed` | `checks.interaction.fastbreak.fastbreak_a` |
| 7 | AutoToolA | `ToolSwap` | `checks.interaction.autotool.autotool_a` |
| 8 | BlockBreakA | `Rotation` | `checks.interaction.blockbreak.blockbreak_a` |
| 9 | BlockBreakB | `Reach` | `checks.interaction.blockbreak.blockbreak_b` |
| 10 | ScaffoldA | `Rotation` | `checks.interaction.scaffold.scaffold_a` |
| 11 | ScaffoldB | `Sprint` | `checks.interaction.scaffold.scaffold_b` |

---

## AirPlaceA — `AirPlace_A` / `AirPlaceA`

- **Enum:** `AIRPLACE_A` — **Title:** `AirPlace_A`
- **Display name:** `AirPlaceA` — **Enum description:** `AirPlaceA` (`CheckName.java:39`)
- **High-level signal:** Places a block where all 6 faces (`UP/DOWN/NORTH/SOUTH/EAST/WEST`) around the placed block are `AIR`/`WATER`/`LAVA` and the replaced state is also air/liquid. Detects AirPlace/LiquidPlace (`AirPlaceA.java:22`).
- **Event lane:** `LACEventType.PLAYER_PLACE_BLOCK` sync — `LOW` `beforeBlockPlace` (desync mitigation) + `NORMAL` `onBlockPlace` (`AirPlaceA.java:43-44`). `beforeBlockPlace` throttled to 5s resends 3x3x3 block updates via `lacPlayer.sendBlockDate`; `onBlockPlace` enforces 200 ms post-update grace.
- **Exemptions / hooks:** `LILY_PAD` / `COPPER`-name blocks exempt; high ping (>400 ms) requires 2 flags; `EnchantsSquaredHook.hasEnchantment(player, "Illuminated", "Harvesting")` (`AirPlaceA.java:105`) exempt.
- **Setback:** `setback: false`, `setback-vio: 2` (`config.yml:757-759`).
- **Config path:** `checks.interaction.airplace.airplace_a` (`config.yml:750`).

## FastPlaceA — `FastPlace_A` / `FastPlaceA`

- **Enum:** `FASTPLACE_A` — **Title:** `FastPlace_A`
- **Display name:** `FastPlaceA` — **Enum description:** `FastPlaceA` (`CheckName.java:40`)
- **High-level signal:** Placement interval too low — async interval <=4 ms then sync interval <=3 ms with dual flag windows (8s / 6s) (`FastPlaceA.java:15-17`). Sustained rapid placement.
- **Event lane:** `LACEventType.ASYNC_PLAYER_PLACE_BLOCK` (`NORMAL` `onAsyncBlockPlace`) + `LACEventType.PLAYER_PLACE_BLOCK` (`NORMAL` `onBlockPlace`) (`FastPlaceA.java:25-26`). Async sets `asyncFlag` (buffer `true`), sync validates and applies 2-stage cooldown before `callViolationEvent`.
- **Exemptions / hooks:** None. No compatibility hooks checked. Early returns on `!isCheckAllowed` and `asyncFlag == false`.
- **Setback:** `setback: false`, `setback-vio: 2` (`config.yml:774-775`).
- **Config path:** `checks.interaction.fastplace.fastplace_a` (`config.yml:766`).

## BlockPlaceA — `BlockPlace_A` / `Rotation`

- **Enum:** `BLOCKPLACE_A` — **Title:** `BlockPlace_A`
- **Display name:** `BlockPlaceA` — **Enum description:** `Rotation` (`CheckName.java:41`)
- **High-level signal:** Head rotation — placed block not in look direction / line-of-sight. Fails `targetBlockExact(10)` (<=3.0 horiz) and `getLineOfSight` (<=2.5 horiz) then forced flag when yaw angle >110 deg with pitch in (-40, 60) (`BlockPlaceA.java:91-124`).
- **Event lane:** `LACEventType.ASYNC_PLAYER_PLACE_BLOCK` (`onAsyncBlockBreak` -> `flag(..., async=true)`) + `LACEventType.PLAYER_PLACE_BLOCK` (`onBlockBreak` -> `flag(..., async=false)`) (`BlockPlaceA.java:40-41`); async result cached as `lastAsyncResult`. Final violation deferred 1 tick via `LACPlayerManager.executeLater` with yaw-change check.
- **Exemptions / hooks:** `getYawChange(...) > 35.0` exempt (compares `FROM`/`FIRST` in both event and packet histories) (`BlockPlaceA.java:76`); `AureliumSkillsHook.isPrevented(player)`, `VeinMinerHook.isPrevented(player)`, `McMMOHook.isPrevented(block.getType())` (`BlockPlaceA.java:79-81`); `EnchantsSquaredHook.hasEnchantment(player, "Illuminated", "Harvesting")` (`BlockPlaceA.java:84`). Also 550 ms `lastFlag` throttle and 2-flag buffer.
- **Setback:** `setback: false`, `setback-vio: 6` (`config.yml:790-791`).
- **Config path:** `checks.interaction.blockplace.blockplace_a` (`config.yml:783`).

## BlockPlaceB — `BlockPlace_B` / `Reach`

- **Enum:** `BLOCKPLACE_B` — **Title:** `BlockPlace_B`
- **Display name:** `BlockPlaceB` — **Enum description:** `Reach` (`CheckName.java:42`)
- **High-level signal:** Horizontal block-place reach — `distanceHorizontal(eye, block) - 0.707107` exceeds `maxDistance` (base 6.0 + backwards distance + ping compensation, capped 8.5, +1.5 if not survival/adventure) (`BlockPlaceB.java:42-63`).
- **Event lane:** `LACEventType.ASYNC_PLAYER_PLACE_BLOCK` only (`NORMAL` `onAsyncBlockPlace`) (`BlockPlaceB.java:31`). Flags async, calls violation on main via `Scheduler.runTask`.
- **Exemptions / hooks:** Backwards-distance compensation from `onEvent`/`onPacket` history `FROM` (`BlockPlaceB.java:48-59`); `PLAYER_BLOCK_INTERACTION_RANGE` attribute (item + player) within 2500 ms window suppresses (`BlockPlaceB.java:73-77`); `EnchantsSquaredHook.hasEnchantment(player, "Illuminated", "Harvesting")` (`BlockPlaceB.java:80`). Requires 3 flags (`flags > 2`).
- **Setback:** `setback: false`, `setback-vio: 3` (`config.yml:805-806`).
- **Config path:** `checks.interaction.blockplace.blockplace_b` (`config.yml:797`).

## GhostBreakA — `GhostBreak_A` / `ThroughBlock`

- **Enum:** `GHOSTBREAK_A` — **Title:** `GhostBreak_A`
- **Display name:** `GhostBreakA` — **Enum description:** `ThroughBlock` (`CheckName.java:43`)
- **High-level signal:** Breaking a block fully surrounded by occluding blocks (or paired same-type block) and no adjacent non-occluding face — interaction through blocks (`GhostBreakA.java:22-24`). Sends corrections for all 6 neighbors via `sendBlockDate` before flagging.
- **Event lane:** `LACEventType.PLAYER_BREAK_BLOCK` sync `NORMAL` `onBlockBreak` (`GhostBreakA.java:42`).
- **Exemptions / hooks:** `isOccluding` = `Material.isOccluding()` or `GLASS`/`*_GLASS` (`GhostBreakA.java:97-105`); high ping (>400 ms) requires 2 flags; `EnchantsSquaredHook.hasEnchantment(player, "Excavation", "Deforestation", "Harvesting")` (`GhostBreakA.java:91`).
- **Setback:** `setback: false`, `setback-vio: 2` (`config.yml:821-822`).
- **Config path:** `checks.interaction.ghostbreak.ghostbreak_a` (`config.yml:813`).

## FastBreakA — `FastBreak_A` / `MiningSpeed`

- **Enum:** `FASTBREAK_A` — **Title:** `FastBreak_A`
- **Display name:** `FastBreakA` — **Enum description:** `MiningSpeed` (`CheckName.java:44`)
- **High-level signal:** Mining speed for `STONE`/`DEEPSLATE` faster than expected for held pickaxe. Compares `left-click -> break` interval against `DURATIONS`/`ENCHANTED_DURATIONS` per tool (`FastBreakA.java:44-59`); flags when `interval < maxDuration / 1.45` with 6-flag accumulation (`FastBreakA.java:166-179`).
- **Event lane:** `LACEventType.PLAYER_BREAK_BLOCK` `NORMAL` `onBlockBreak` + `LOW` `beforeBlockBreak`, `LACEventType.ASYNC_PLAYER_MOVE` `LOW` `onMovement` (`FastBreakA.java:91-93`), plus Bukkit `PlayerInteractEvent` `LEFT_CLICK_BLOCK` to set `lastInteraction` (`FastBreakA.java:202`). `beforeBlockBreak`/`onMovement` track `FAST_DIGGING` (Haste) `effectTime` (10s window clears flags).
- **Exemptions / hooks (source-verified — only hooks actually checked in this file):** `AureliumSkillsHook.isPrevented(player)`, `VeinMinerHook.isPrevented(player)`, `McMMOHook.isPrevented(block.getType())` (`FastBreakA.java:105-107`); `EnchantsSquaredHook.hasEnchantment(player, "Excavation", "Deforestation", "Harvesting")` (`FastBreakA.java:185`); efficiency >5 exempt (`FastBreakA.java:114`); Haste potion (`FAST_DIGGING`) within 10s resets (`FastBreakA.java:157`); attributes `PLAYER_BLOCK_BREAK_SPEED` / `PLAYER_MINING_EFFICIENCY` / `PLAYER_SUBMERGED_MINING_SPEED` within 3500 ms exempt (`FastBreakA.java:189-195`); non-survival gamemode exempt; non-stone/deepslate or non-pickaxe clears flags. Does NOT check Floodgate, Jobs, mcMMO beyond `McMMOHook.isPrevented`.
- **Setback:** `setback: false`, `setback-vio: 8` (`config.yml:837-838`).
- **Config path:** `checks.interaction.fastbreak.fastbreak_a` (`config.yml:829`).

## AutoToolA — `AutoTool_A` / `ToolSwap`

- **Enum:** `AUTOTOOL_A` — **Title:** `AutoTool_A`
- **Display name:** `AutoToolA` — **Enum description:** `ToolSwap` (`CheckName.java:45`)
- **High-level signal:** Automated tool swap — switches to correct tool within 150 ms of `LEFT_CLICK_BLOCK` and repeats, plus back-switch within 150 ms (`AutoToolA.java:20-28`). Requires 2 correct switches in 5s window (`SWITCH_THRESHOLD=2`, `STREAK_WINDOW=5000`).
- **Event lane:** Bukkit `PlayerInteractEvent` `LEFT_CLICK_BLOCK` (MONITOR, `onLeftClick`) to snapshot `lastClickTime`/`lastBlockType`/`lastSlot`/`lastHeldType`, and `PlayerItemHeldEvent` (NORMAL, `onItemHeld`) to evaluate swap (`AutoToolA.java:34-62`). Both routed via `LACPlayerManager.execute(player, false, ...)`.
- **Exemptions / hooks:** No plugin hooks. Exempt if not `SURVIVAL`/`ADVENTURE` (`isSurvivalLike` `AutoToolA.java:129`); previous tool already correct or new tool incorrect (`isCorrectTool` by name) cancels streak (`AutoToolA.java:97-99`); delay >150 ms decrements streak; slot mismatch ignored.
- **Setback:** `setback: false`, `setback-vio: 5` (`config.yml:853-854`).
- **Config path:** `checks.interaction.autotool.autotool_a` (`config.yml:845`).

## BlockBreakA — `BlockBreak_A` / `Rotation`

- **Enum:** `BLOCKBREAK_A` — **Title:** `BlockBreak_A`
- **Display name:** `BlockBreakA` — **Enum description:** `Rotation` (`CheckName.java:46`)
- **High-level signal:** Head rotation on break — same heuristic as BlockPlaceA but for breaking: target block (<=3.5 horiz) and line-of-sight (<=3.0 horiz), forced flag when angle >120 deg with pitch in (-40,60) (`BlockBreakA.java:91-124`).
- **Event lane:** `LACEventType.ASYNC_PLAYER_BREAK_BLOCK` (`onAsyncBlockBreak`) + `LACEventType.PLAYER_BREAK_BLOCK` (`onBlockBreak`) (`BlockBreakA.java:40-41`); async result cached, sync enforces 500 ms throttle and 2-flag buffer, final violation deferred 1 tick via `LACPlayerManager.executeLater`.
- **Exemptions / hooks:** `getYawChange(...) > 30.0` exempt (`BlockBreakA.java:76`); `AureliumSkillsHook.isPrevented(player)`, `VeinMinerHook.isPrevented(player)`, `McMMOHook.isPrevented(block.getType())` (`BlockBreakA.java:79-81`); `EnchantsSquaredHook.hasEnchantment(player, "Excavation", "Deforestation", "Harvesting")` (`BlockBreakA.java:84`).
- **Setback:** `setback: false`, `setback-vio: 6` (`config.yml:869-870`).
- **Config path:** `checks.interaction.blockbreak.blockbreak_a` (`config.yml:861`).

## BlockBreakB — `BlockBreak_B` / `Reach`

- **Enum:** `BLOCKBREAK_B` — **Title:** `BlockBreak_B`
- **Display name:** `BlockBreakB` — **Enum description:** `Reach` (`CheckName.java:47`)
- **High-level signal:** Horizontal block-break reach — same distance model as BlockPlaceB (`distanceHorizontal - 0.707107` vs 6.0 + backwards + ping compensation, cap 8.5, +1.5 if not survival/adventure) (`BlockBreakB.java:45-66`).
- **Event lane:** `LACEventType.ASYNC_PLAYER_BREAK_BLOCK` only (`NORMAL` `onAsyncBlockBreak`) (`BlockBreakB.java:34`). Async flags, violation via `Scheduler.runTask`.
- **Exemptions / hooks:** `AureliumSkillsHook.isPrevented(player)`, `VeinMinerHook.isPrevented(player)`, `McMMOHook.isPrevented(block.getType())` (`BlockBreakB.java:83-85`); `EnchantsSquaredHook.hasEnchantment(player, "Excavation", "Deforestation", "Harvesting")` (`BlockBreakB.java:88`); `PLAYER_BLOCK_INTERACTION_RANGE` attribute within 2500 ms (`BlockBreakB.java:76-79`); requires 3 flags.
- **Setback:** `setback: false`, `setback-vio: 3` (`config.yml:884-885`).
- **Config path:** `checks.interaction.blockbreak.blockbreak_b` (`config.yml:876`).

## ScaffoldA — `Scaffold_A` / `Rotation`

- **Enum:** `SCAFFOLD_A` — **Title:** `Scaffold_A`
- **Display name:** `ScaffoldA` — **Enum description:** `Rotation` (`CheckName.java:48`)
- **High-level signal:** Impossible placement while scaffolding — scaffold placement (via `isScaffoldPlacement` `InteractionCheck.java:16`) with flat pitch (<=34 deg) or yaw+ pitch rotation delta between `FROM` and current vs `FROM`->`FIRST` (`ScaffoldA.java:57-64`).
- **Event lane:** `LACEventType.ASYNC_PLAYER_PLACE_BLOCK` only (`NORMAL` `onAsyncBlockPlace`) (`ScaffoldA.java:34`). Requires 3 flags; violation via `Scheduler.runTask` + `callViolationEventIfRepeat(..., 1500 ms)`.
- **Exemptions / hooks:** `!isScaffoldPlacement` exempt; colliding `withinBlocks` not `AIR` exempt (`ScaffoldA.java:48-51`); `LEVITATION` or `SPEED` amplifier >5 exempt (`ScaffoldA.java:53-55`). No EnchantsSquared/Aurelium/VeinMiner hooks in this check.
- **Setback:** `setback: false`, `setback-vio: 6` (`config.yml:900-901`).
- **Config path:** `checks.interaction.scaffold.scaffold_a` (`config.yml:892`).

## ScaffoldB — `Scaffold_B` / `Sprint`

- **Enum:** `SCAFFOLD_B` — **Title:** `Scaffold_B`
- **Display name:** `ScaffoldB` — **Enum description:** `Sprint` (`CheckName.java:49`)
- **High-level signal:** Sprinting while scaffold-placing — same scaffold placement predicate plus sprint state and airborne history (`ScaffoldB.java:23-25`).
- **Event lane:** `LACEventType.ASYNC_PLAYER_PLACE_BLOCK` only (`NORMAL` `onAsyncBlockPlace`) (`ScaffoldB.java:33`). Requires 3 flags; violation via `Scheduler.runTask`.
- **Exemptions / hooks:** `!isScaffoldPlacement` exempt; `FloodgateHook.isBedrockPlayer(player, true)` exempt (`ScaffoldB.java:47`); colliding `withinBlocks` not `AIR` exempt; `LEVITATION` or `SPEED` amplifier >5 exempt (`ScaffoldB.java:55-57`); last 3 `onGround` history entries not `towardsFalse` (event + packet) exempts (`ScaffoldB.java:59-64`); `!player.isSprinting()` exempt.
- **Setback:** `setback: false`, `setback-vio: 6` (`config.yml:915-916`).
- **Config path:** `checks.interaction.scaffold.scaffold_b` (`config.yml:907`).

---

## Common notes

- All checks gate on `isCheckAllowed(player, lacPlayer[, async])` which checks global `enabled`, `min-tps`, `max-ping`, `java`/`bedrock` toggles per `config.yml:760-764` style blocks.
- `CheckName` `title` stored without underscore (e.g. `AirPlaceA`) but `group`/`check` derived from underscore split (`CheckName.java:80-83`).
- Violation path is `Check.callViolationEvent(...)` -> `LACViolationEvent` -> punishment/setback handled by `CheckSetting`.
