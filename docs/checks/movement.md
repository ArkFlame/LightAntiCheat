# Movement Checks

> Source of truth is implementation under `src/main/java/me/vekster/lightanticheat/check/checks/movement/**` and `CheckName.java:3-27`. This document lists exactly 24 movement checks. Do not add or remove entries.

Base class: `src/main/java/me/vekster/lightanticheat/check/checks/movement/MovementCheck.java:26` — `abstract class MovementCheck extends Check`.

- Gating: every check implements `isConditionAllowed(Player, LACPlayer, PlayerCache, isClimbing, isInWater, isFlying, isInsideVehicle, isGliding, isRiptiding)` and delegates to an `LACAsyncPlayerMoveEvent` / `LACPlayerMoveEvent` / `LACAsyncPacketReceiveEvent` overload. Most checks reset their `Buffer` counters when `isConditionAllowed == false`.
- Shared exemptions pattern (see per-check): `cache.flyingTicks / climbingTicks / glidingTicks / riptidingTicks` windows; `lastInsideVehicle`, `lastInWater`, `lastKnockback` / `lastKnockbackNotVanilla`, `lastKbVelocity`, `lastAirKbVelocity`, `lastStrongKbVelocity`, `lastFlight`, `lastGliding`/`lastRiptiding`, `lastWindCharge`/`lastWindBurst`, `lastEntityNearby`/`lastEntityVeryNearby`, `lastTeleport`, `lastRespawn`, slime/honey timestamps, `lastWasHit`/`lastWasDamaged`, `lastBlockExplosion`/`lastEntityExplosion`.
- Cross-cutting hooks: `FloodgateHook` (Bedrock / Pocket-Edition score adjustments, `isCancelledMovement`), `EnchantsSquaredHook` (`Burden`, `Rope Dart`, `Shockwave` exemptions via `getPlayersForEnchantsSquared`), `ValhallaMMOHook` (disables or relaxes `SpeedC`/`FlightA`), `AccuracyUtil.isViolationCancel`, `isLagGlidingPossible` / `isPingGlidingPossible` in `MovementCheck.java:49-85`, attribute overrides (`GENERIC_MOVEMENT_SPEED`, `GENERIC_JUMP_STRENGTH`, `GENERIC_FLYING_SPEED`, etc.).
- Setback: config exposes `checks.movement.*.<check>.setback.setback` + `setback-vio` (see paths below); only `SpeedE` (`SpeedE.java:101-167`) performs immediate movement cancellation + `FoliaUtil.teleportPlayer(player, from)` on limit breach before the violation pipeline. All other movement checks call `callViolationEvent` / `callViolationEventIfRepeat` and optionally `updateDownBlocks` (ghost-block resync on violation).

## Summary table

| Check | Title | CheckName desc | Config path | Lane |
|---|---|---|---|---|
| FlightA | `FlightA` | `Acceleration` | `checks.movement.flight.flight_a` | `LACAsyncPlayerMoveEvent` (`ASYNC_PLAYER_MOVE:POSITION`) + `LACAsyncPlayerPlaceBlockEvent` + `beforeMovement(LOW)` |
| FlightB | `FlightB` | `Height` | `checks.movement.flight.flight_b` | `LACAsyncPlayerMoveEvent` + `PlaceBlock` + `PlayerJoin`/`PlayerVelocity` |
| FlightC | `FlightC` | `Vector` | `checks.movement.flight.flight_c` | `LACAsyncPlayerMoveEvent` + `PlaceBlock` |
| SpeedA | `SpeedA` | `Horizontal` | `checks.movement.speed.speed_a` | `LACAsyncPlayerMoveEvent` (dual handler: `totalHorizontal` + `airHorizontal`) |
| SpeedB | `SpeedB` | `Ground` | `checks.movement.speed.speed_b` | `LACAsyncPlayerMoveEvent` |
| SpeedC | `SpeedC` | `Prediction` | `checks.movement.speed.speed_c` | `LACAsyncPlayerMoveEvent` (+ `PlayerTeleport`/`WorldChange`/`Respawn` reset) |
| SpeedD | `SpeedD` | `Liquid` | `checks.movement.speed.speed_d` | `LACAsyncPlayerMoveEvent` |
| SpeedE | `SpeedE` | `Limiter` | `checks.movement.speed.speed_e` | `LACAsyncPlayerMoveEvent` (`afterMovement:HIGH` + `onTeleportHorizontal/Vertical` + `onHorizontal/onVertical`) + Bukkit `PlayerTeleport/WorldChange/Respawn` |
| SpeedF | `SpeedF` | `Legal` | `checks.movement.speed.speed_f` | `LACAsyncPlayerMoveEvent` |
| NoFallA | `NoFallA` | `FallDistance` | `checks.movement.nofall.nofall_a` | `LACAsyncPlayerMoveEvent` + `LACAsyncPlayerBreakBlockEvent` |
| NoFallB | `NoFallB` | `GroundSpoof` | `checks.movement.nofall.nofall_b` | `LACAsyncPlayerMoveEvent` |
| JumpA | `JumpA` | `Speed` | `checks.movement.jump.jump_a` | `LACAsyncPlayerMoveEvent` + `PlayerVelocityEvent` |
| JumpB | `JumpB` | `Height` | `checks.movement.jump.jump_b` | `LACAsyncPlayerMoveEvent` + `PlayerVelocityEvent` |
| LiquidWalkA | `LiquidWalkA` | `Jesus` | `checks.movement.liquidwalk.liquidwalk_a` | `LACAsyncPlayerMoveEvent` |
| LiquidWalkB | `LiquidWalkB` | `Jesus` | `checks.movement.liquidwalk.liquidwalk_b` | `LACAsyncPlayerMoveEvent` |
| FastClimbA | `FastClimbA` | `ClimbingSpeed` | `checks.movement.fastclimb.fastclimb_a` | `LACAsyncPlayerMoveEvent` |
| NoSlowA | `NoSlowA` | `Cobweb` | `checks.movement.noslow.noslow_a` | `LACAsyncPlayerMoveEvent` + `beforeMovement` |
| StepA | `StepA` | `Step` | `checks.movement.step.step_a` | `LACAsyncPlayerMoveEvent` |
| BoatA | `BoatA` | `Boat` | `checks.movement.boat.boat_a` | `LACPlayerMoveEvent` (`PLAYER_MOVE:POSITION`, sync) dual: `boatFlight` + `boatSpeed` |
| VehicleA | `VehicleA` | `Vehicle` | `checks.movement.vehicle.vehicle_a` | `LACAsyncPacketReceiveEvent` `STEER_VEHICLE` |
| ElytraA | `ElytraA` | `Speed` | `checks.movement.elytra.elytra_a` | `LACAsyncPlayerMoveEvent` dual: `theSameSpeed` + `tooLowSpeed` + `beforeMovement` |
| ElytraB | `ElytraB` | `Acceleration` | `checks.movement.elytra.elytra_b` | `LACAsyncPlayerMoveEvent` + `beforeMovement` |
| ElytraC | `ElytraC` | `Takeoff` | `checks.movement.elytra.elytra_c` | `LACAsyncPlayerMoveEvent` + `beforeMovement` |
| TridentA | `TridentA` | `TridentBoost` | `checks.movement.trident.trident_a` | `LACAsyncPlayerMoveEvent` + `beforeMovement` |

---

## Flight

### FlightA — `Flight_A` / `Acceleration`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/flight/FlightA.java:33`
- **Enum:** `FLIGHT_A`
- **Signal (read, not inferred):** Compares observed vertical speed `verticalSpeed = min( from->to, FIRST->to/2/1.2, packet-FIRST->FROM/1.25, packet-SECOND->FROM/2/1.25/1.2 ) / 0.925 - 0.095` (with `-0.11` / `*0.85` for Floodgate Bedrock) against `VanillaVerticalPhysics.flightVerticalLimit(jumpAmp, fallingTicks, slowFalling)` (`FlightA.java:179`). `fallingTicks` increments per airborne tick; `JUMP` and `SLOW_FALLING` + `GENERIC_GRAVITY`/`GENERIC_JUMP_STRENGTH` attributes adjust threshold. Early airborne horizontal-dominant ticks subtract `0.15/0.10`. Recent `fallingTime` grace (`-0.4 *0.9`) applies.
- **Lane:** `LACEventBus LACEventType.ASYNC_PLAYER_MOVE NORMAL onAsyncMovement + LOW beforeMovement` (`FlightA.java:44-46`); `ASYNC_PLAYER_PLACE_BLOCK scaffoldAsyncBlockPlace` suppresses for 400 ms after within-block place (`FlightA.java:286`).
- **Important exemptions/hooks:** `isConditionAllowed` (`FlightA.java:50`) — flying/vehicle/climbing/gliding/riptiding/inWater false, ticks `flying>=-25/climbing>=-2/gliding>=-3/riptiding>=-5` block, 22 timestamp gates including `lastKbVelocity/AirKbVelocity/Strong*`, `lastFlight`, `lastPowderSnowWalk`, `lastWindCharge/Burst*`; runtime: `effectTime` (LEVITATION/SLOW_FALLING>1/JUMP>6 for 2s), entityNearby 1s, scaffold place 400ms, onGround history (3), down-block impassable checks, `FloodgateHook` Bedrock offset, `GENERIC_JUMP_STRENGTH` attribute grace 4s, `isPing/ LagGlidingPossible`, `EnchantsSquared Burden/Rope Dart/Shockwave`, `AccuracyUtil.isViolationCancel`, `ValhallaMMOHook` first-9-flags grace 8s.
- **Setback:** Config `checks.movement.flight.flight_a.setback.*`; no immediate cancel — deferred `callViolationEventIfRepeat(buffer, 500/900)` inside `Scheduler.runTask(true)` (`FlightA.java:245`).

### FlightB — `Flight_B` / `Height`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/flight/FlightB.java:38`
- **Enum:** `FLIGHT_B`
- **Signal:** Cumulative height `height = distanceVertical(startLocation, to)*0.9 -0.1 - interactiveOffset` (`FlightB.java:219`) vs `VanillaVerticalPhysics.maxJumpHeight(jumpAmp)` (`FlightB.java:244`). `startLocation` resets on ground (`beforeMovement`, `onVelocity`) and tracks interactiveBlock vertical stacking (+1 per Y step within 100° cone, `FlightB.java:90-118`). `jumpAmp>2` subtracts `0.2*amp`. Attribute `GENERIC_JUMP_STRENGTH` subtracts 10/20/40/80.
- **Lane:** Same as FlightA + `PlayerJoinEvent`/`PlayerVelocityEvent` (`FlightB.java:343-375`) to seed `startLocation` and `lastVelocityTime` (velocity Y != -0.0784).
- **Exemptions/hooks:** `isConditionAllowed` similar to FlightA with `flying>=-10` (`FlightB.java:53`); runtime: `effectTime` (LEVITATION / JUMP>32, 2s), `justEffectTime` (JUMP removal 100ms), `velocityBypass 750+1500*SLOW_FALLING`, scaffold 400ms, `EnchantsSquared` height-scaled exemptions (`*0.9-0.5` / `*0.7-1.8`), lag/ping gliding guards.
- **Config:** `checks.movement.flight.flight_b` | **Setback:** `setback-vio 21` deferred violation (`500/900`).

### FlightC — `Flight_C` / `Vector`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/flight/FlightC.java:35`
- **Enum:** `FLIGHT_C`
- **Signal:** Two-stage trajectory. (1) `airJump` state machine: `>0.05` up → `<-0.125` down → `>0.125` up (requires `VerIdentifier > V1_8`, `FlightC.java:150-176`) directly violates (bedrock-aware repeat 2000 vs `bufferDuration-1000`). (2) Sustained-flight path: after `flightTicks>2`, checks `isSpeedDecreasing` over 7 (or 10 on 1.8) history locations in both event/packet channels — if no decreasing vertical delta, flags via `callViolationEventIfRepeat(buffer, 750/400 or 500/250 for Bedrock)` (`FlightC.java:184-215`). Attribute gate: `GENERIC_JUMP_STRENGTH >0.15/0.43` aborts.
- **Lane:** `ASYNC_PLAYER_MOVE + beforeMovement + scaffoldAsyncBlockPlace`.
- **Exemptions/hooks:** `isConditionAllowed` `flying>=-15` (`FlightC.java:49`); runtime: `effectTime` (LEVITATION / SLOW_FALLING>1 / JUMP>6, 2s), scaffold 400ms, `isPing/LagGlidingPossible`, `EnchantsSquared`.
- **Config:** `checks.movement.flight.flight_c`.

---

## Speed

### SpeedA — `Speed_A` / `Horizontal`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/speed/SpeedA.java:40`
- **Enum:** `SPEED_A`
- **Signal:** Dual handler.
  - `totalHorizontal` (`SpeedA.java:78`): `hSpeed = min(FIRST->to/2 - vertical*2, from->to - vertical*2)/ (walkSpeed/0.2)` vs `max 0.8418 * (SPEED*0.35+1, *1.35 if >2) * (JUMP*0.25+1) * (LEVITATION*0.20+1) /1.15 (+1.15 if interactive)`, attributes scale `(1.05+0.11)*(1+attr)`. Requires `speedTicks>2`.
  - `airHorizontal` (`SpeedA.java:240`): same with `vertical/7.5` penalty, `max 1.15*(SPEED*0.35, *1.3)*(JUMP*0.35,*1.3)*(LEVITATION*0.5)`, requires 4-tick airborne, not on ICE/ SoulSpeed, near-zero accel guard `delta>0.000071`, horizontal floor `0.091`.
- **Lane:** `ASYNC_PLAYER_MOVE POSITION` (`totalHorizontal NORMAL`, `airHorizontal NORMAL`, two `beforeMovement LOW`). Resets `flags`/`airFlags` on teleport/world/respawn.
- **Exemptions/hooks:** `isConditionAllowed` (`SpeedA.java:46`) — not flying/vehicle/climbing/gliding/riptiding/inWater, `flying>=-6` etc., 20 timestamp gates; `effectTime 1250ms` (total) / `airEffectTime 2000ms` (SPEED>5 / LEVITATION/SLOW_FALLING/JUMP>5); passable slabs/ICE/SOUL_SAND+SoulSpeed; `isGliding/isRiptiding` skip; `EnchantsSquared /2.5`; `Floodgate`-not relevant here; attribute grace 3s.
- **Config:** `checks.movement.speed.speed_a` (setback-vio 16, punish 30) | repeat `5000`.

### SpeedB — `Speed_B` / `Ground`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/speed/SpeedB.java:33`
- **Enum:** `SPEED_B`
- **Signal:** Horizontal speed while continuously grounded: requires 6-tick streak of `onGround towardsTrue` in both event and packet history (`SpeedB.java:121-148`). `hSpeed = min(from->to - vert/1.8, FIRST->to/2 - vert/2/1.8) / (walkSpeed/0.2)` vs `max 0.2806 * (SPEED*0.4, *1.35 if >2) *1.3`, attribute scaled. Non-block-height airborne adds `- vert*2.5` penalty but levitation/jump abort.
- **Lane:** `ASYNC_PLAYER_MOVE`.
- **Exemptions/hooks:** `isConditionAllowed` (`SpeedB.java:39`) similar plus `lastWindChargeReceive 875`; `effectTime 1250ms` SPEED>5; `FloodgateHook.isCancelledMovement` check; slabs/ICE/SoulSpeed; `isPingGlidingPossible` / `isLagGlidingPossible(r=15)` with 7s grace, `EnchantsSquared/2.5`, `AccuracyUtil`.
- **Config:** `checks.movement.speed.speed_b`.

### SpeedC — `Speed_C` / `Prediction`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/speed/SpeedC.java:42`
- **Enum:** `SPEED_C`
- **Signal:** Simulated friction prediction: `speed = ScalaredMath.scaleVal(dist(from,to),2)`, `finalSpeedLimit = scaleVal(MoveEngine.getSpeedByTick(localAirTicker)* maxSpeed +0.01,2)` where `localAirTicker` counts consecutive non-ground ticks (`SpeedC.java:91`), `maxSpeed =1.0*(SPEED*0.35,*1.35)`. Pocket-Edition `*0.85`. Attribute non-zero aborts. Accumulates `localPlayerRaport +3/-1`, flags when `>30` then `flags>3` (`>2` if entityNearby>1s) (`SpeedC.java:162-169`).
- **Lane:** `ASYNC_PLAYER_MOVE`.
- **Exemptions/hooks:** `isConditionAllowed` (`SpeedC.java:48`) — `flying>=-8/climbing>=-3/gliding>=-7/riptiding>=-8`, 22 timestamps; `ValhallaMMOHook` disables entirely (`SpeedC.java:79`); `effectTime 2500ms`, impassable/ice/soulSpeed grace; `EnchantsSquared`, `AccuracyUtil`; disabled on attribute `MOVEMENT_SPEED>0.14`.
- **Config:** `checks.movement.speed.speed_c`.

### SpeedD — `Speed_D` / `Liquid`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/speed/SpeedD.java:33`
- **Enum:** `SPEED_D`
- **Signal:** Horizontal speed while fully submerged: both `getWithinBlocks(from)` and `to` must contain WATER/LAVA and no solid non-liquid within (`SpeedD.java:89-115`), requires 4-tick airborne (no ground last 4). `hSpeed = min(from->to - vert/4, FIRST->to/2 - vert/2/4)/(walkSpeed/0.2)` vs `max 0.2 * (Dolphins1 *2.5 / Dolphins2 *5.0) * (DepthStrider 1+level/2 or skip if DolphinsGrace) *1.3`, attribute scaled `*13` via `GENERIC_WATER_MOVEMENT_EFFICIENCY` etc. Bedrock `*1.1+0.05`. Needs `liquidTicks>4`.
- **Lane:** `ASYNC_PLAYER_MOVE` + `beforeMovement` DolphinGrace effect window.
- **Exemptions/hooks:** `isConditionAllowed` (`SpeedD.java:39`) — allows `isInWater` (unlike A/B/C), `flying>=-6`, `lastFlight 3000`; `effectTime 4000ms` (LEVITATION>2 / SPEED>5), `dolphin1/2EffectTime`, attribute grace 3s, `isPingGlidingPossible`.
- **Config:** `checks.movement.speed.speed_d`.

### SpeedE — `Speed_E` / `Limiter`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/speed/SpeedE.java:36`
- **Enum:** `SPEED_E`
- **Signal:** Absolute blatant limiter, four independent guards:
  - `onTeleportHorizontal`: `distanceHorizontal(from,to) >6` → `event.setCancelled(true)` + `FoliaUtil.teleportPlayer(player, from)` + deferred violation (`SpeedE.java:101-133`).
  - `onTeleportVertical`: `distanceVertical >12` same teleport (`SpeedE.java:135-167`).
  - `onHorizontal`: `min(SECOND->to/3, FIRST->to/2, SECOND->from/2)/(walkSpeed/0.2)` vs `max 3.0*(SPEED>3*2.5 / >2*2)*(Dolphins>1*2.5)` attributescaled `*13`; needs `flags>3` (`SpeedE.java:169-247`).
  - `onVertical`: `min(from->to, FIRST->to/2)` vertical vs `max 0.72*2=1.44` attributescaled; needs 10-tick non-ground, passable, `lastJoin>2s`, effect LEVITATION≤1/SLOW_FALLING≤1/JUMP≤2 (`SpeedE.java:249-361`).
- **Lane:** `ASYNC_PLAYER_MOVE: afterMovement HIGH (stores lastMovement), onTeleportHorizontal/Vertical NORMAL, onHorizontal/Vertical NORMAL` plus Bukkit `PlayerTeleport/WorldChange/Respawn` flag reset.
- **Exemptions/hooks:** `isConditionAllowed` (`SpeedE.java:42` + inline vertical variant `SpeedE.java:263`) — 20 timestamps, `FloodgateHook.isBedrockPlayer` skips all four (Bedrock exempt), `joinTime 7500ms` for horizontal / `2000ms` vertical, `lastMovement 1000ms` freshness, `lastTeleport 1000ms/2500ms` guards, SPEED/DOLPHINS caps, `CooldownUtil skip 150` for vertical.
- **Config:** `checks.movement.speed.speed_e` | **Special setback:** teleport setback on the two teleport guards; standard `setback-vio 16`.

### SpeedF — `Speed_F` / `Legal`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/speed/SpeedF.java:22`
- **Enum:** `SPEED_F`
- **Signal:** Speed while legitimately flying (`cache.flyingTicks >5` required — `SpeedF.java:32` `flyingTicks <=5 → not allowed`). `speed = min(dist(from,to), dist(FIRST,to)/2)/(flySpeed/0.1)` vs `max 1.17*1.4=1.638` (`SpeedF.java:79`). Accumulates `speedTicks 0..16`, violates at `>15`. Attribute `GENERIC_FLYING_SPEED>0.1` aborts 1s.
- **Lane:** `ASYNC_PLAYER_MOVE` only, with `CooldownUtil skip 190` when clean.
- **Exemptions/hooks:** `isConditionAllowed` (`SpeedF.java:28`) — only when `isFlying` absent from outer gate but `flyingTicks>5` inside (so must have been flying), no vehicle/climbing/gliding/riptiding, `lastFlight 1500`, no water/slime/honey gates special; allows flight.
- **Config:** `checks.movement.speed.speed_f` (punish 50, setback 26) | repeat `3000`.

---

## NoFall

### NoFallA — `NoFall_A` / `FallDistance`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/nofall/NoFallA.java:31`
- **Enum:** `NOFALL_A`
- **Signal:** Spoofed `player.getFallDistance()` vs prediction: `fallEvents++` per falling tick (vertical ≤0 for 3 history, `NoFallA.java:148`), `fallDistance = distVertical(fallStartLocation, from)`, `calculated = min(NoFallPredictionProfile.byEvents(jumpAmp, fallEvents), byDistance(jumpAmp, fallDistance))`. Adjusted `playerFallDistance += 0.3/0.7 + scaffoldBreaks*1.1 + sneaking>25?0:0.5 + interactive250ms?0.75` then `*1.2+0.35`; flag if `< calculated` (`NoFallA.java:171-198`). Horizontal-dominant case adds 0.3 bonus and `isHorizontalFallDistanceExemption` check.
- **Lane:** `ASYNC_PLAYER_MOVE + beforeMovement + ASYNC_PLAYER_BREAK_BLOCK scaffoldBlockBreak` (breaks within bump `scaffoldBreaks`).
- **Exemptions/hooks:** `isConditionAllowed` (`NoFallA.java:38`) 16 timestamps, Survival/Adventure only, `lastEntityNearby 3000ms`, `joinTime 5000ms`, `effectTime` LEVITATION/SLOW_FALLING/JUMP>5 (1s), sneak/interactive bonuses, `EnchantsSquared Burden*1.2+1.0 / Shockwave*1.5+1.2`.
- **Config:** `checks.movement.nofall.nofall_a`.

### NoFallB — `NoFall_B` / `GroundSpoof`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/nofall/NoFallB.java:27`
- **Enum:** `NOFALL_B`
- **Signal:** Spoofed `Entity.isOnGround()` (ground packet spoof): requires 3 ticks vertical `<=-0.00001` falling and `Y%0.5 !=0` (`NoFallB.java:126-128`), then `fallEvents>3` and `((LivingEntity)player).isOnGround()==true` flags (`NoFallB.java:131-137`). Repeat `3000` (or `600` if JUMP>2).
- **Lane:** `ASYNC_PLAYER_MOVE`.
- **Exemptions/hooks:** `isConditionAllowed` (`NoFallB.java:34`) 16 timestamps; `effectTime` LEVITATION/SLOW_FALLING (1s); `lastEntityNearby 3000ms`; downBlock passable; `jumpAmp` for repeat window.
- **Config:** `checks.movement.nofall.nofall_b`.

---

## Jump

### JumpA — `Jump_A` / `Speed`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/jump/JumpA.java:34`
- **Enum:** `JUMP_A`
- **Signal:** HighJump via `secondFlag` (`JumpA.java:173`): requires streak `maxEventGround>=2 && maxPacketGround>=2` (consecutive `onGround towardsFalse` over 6 history). Then `velocity.getY()` and `vSpeed=distVertical(from,to)` must satisfy `vSpeed >0.42*1.9` (0-2 jump amp scaled 0.42/0.52/0.621) with `velocity < base*1.05`. Needs `lastFlag` repeated within `100ms` (`JumpA.java:120`) and not `isBlockHeight(fromY)`, no slime/honey within/down. `lastVelocity 1750ms` gate via `PlayerVelocityEvent`.
- **Lane:** `ASYNC_PLAYER_MOVE` + `PlayerVelocityEvent` (`JumpA.java:210`).
- **Exemptions/hooks:** `isConditionAllowed` (`JumpA.java:45`) 22 timestamps including windCharge/Burst, `lastPowderSnowWalk`, `lastStrongKb`; `JUMP>2 || LEVITATION` abort, `GENERIC_JUMP_STRENGTH>0.43` grace 2s.
- **Config:** `checks.movement.jump.jump_a`.

### JumpB — `Jump_B` / `Height`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/jump/JumpB.java:37`
- **Enum:** `JUMP_B`
- **Signal:** Cumulative jump height while decelerating upward: requires `previousEventSpeed>eventSpeed>0` and `previousPacketSpeed>packetSpeed>0` (`JumpB.java:155`), accumulates `jumpHeight += eventSpeed` (`JumpB.java:161`) vs `maxJumpHeight` table `0:0.499,1:0.885,2:1.368,3:1.944,4:2.609,5:3.360 *1.2+0.25`, attribute adds 20/40/80 (`JumpB.java:163-187`). Flag if `>max` with repeat `bufferDuration-1000`. Disabled on Folia (`JumpB.java:72`).
- **Lane:** `ASYNC_PLAYER_MOVE` + `PlayerVelocityEvent` (Folia-aware).
- **Exemptions/hooks:** `isConditionAllowed` same as JumpA; `LEVITATION>0 || JUMP>5` abort; `lastJumpEffect` change resets; slime/honey, `GENERIC_JUMP_STRENGTH` grace 4s, `EnchantsSquared` scaled grace.
- **Config:** `checks.movement.jump.jump_b`.

---

## LiquidWalk (Jesus)

### LiquidWalkA — `LiquidWalk_A` / `Jesus`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/liquidwalk/LiquidWalkA.java:28`
- **Enum:** `LIQUIDWALK_A`
- **Signal:** Walking on liquid surface without falling: requires `onGround false` for unbounded history (`LiquidWalkA.java:75`), vertical `< LOWEST_BLOCK_HEIGHT` for 3 frames (`LiquidWalkA.java:83`), `horizontal>0.05`, downBlocks liquid either directly within 0.15 or at `Y%` drop within 0.18 (`LiquidWalkA.java:91-106`), no slime/honey via `getToInteractiveBlocks`.
- **Lane:** `ASYNC_PLAYER_MOVE` (async).
- **Exemptions/hooks:** `isConditionAllowed` (`LiquidWalkA.java:34`) 16 timestamps; `LEVITATION !=0` abort; calls `updateDownBlocks`.
- **Config:** `checks.movement.liquidwalk.liquidwalk_a`.

### LiquidWalkB — `LiquidWalk_B` / `Jesus`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/liquidwalk/LiquidWalkB.java:22`
- **Enum:** `LIQUIDWALK_B`
- **Signal:** Jesus with block-height oscillation: requires `onGround false` all history and at least one historical `isBlockHeight` (`LiquidWalkB.java:90`), downBlocks have liquid in 3-deep column (`LiquidWalkB.java:96`), interactive passable, collects `flags>1` + `lastUpdate 3s` window, then requires sequence `up>0.07` then `down<-0.07` (`LiquidWalkB.java:123-129`) before horizontal>0.08 with liquid Y% check (`LiquidWalkB.java:132-142`).
- **Lane:** `ASYNC_PLAYER_MOVE`.
- **Exemptions/hooks:** Same `isConditionAllowed` as A (no LEVITATION), interactive passable guard, `updateDownBlocks`.
- **Config:** `checks.movement.liquidwalk.liquidwalk_b`.

---

## Other

### FastClimbA — `FastClimb_A` / `ClimbingSpeed`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/fastclimb/FastClimbA.java:32`
- **Enum:** `FASTCLIMB_A`
- **Signal:** Vertical speed while `isClimbing==true` (inverted condition: only when climbing, requires `climbingTicks>2`, `FastClimbA.java:45`). Takes `min` over 4 history deltas: `from->to`, `FIRST->to/2`, `SECOND->to/3`, `packet FIRST->to` (`FastClimbA.java:103-112`). Flags if `>0.11*1.5` up or `<-0.15*1.65` down (2× on `<V1_13`, 1.2× for Bedrock). Skips scaffolding within, requires `sneakingTicks<=-15`, same material at feet and -1, ignores the `0.5 ±0.1176/0.15001` block-edge bug window (`FastClimbA.java:115-118`). Requires 5-tick monotonic sign consistency (`FastClimbA.java:143-159`).
- **Lane:** `ASYNC_PLAYER_MOVE` only.
- **Exemptions/hooks:** `isConditionAllowed` requires climbing; Bedrock early return (`FastClimbA.java:78`); `FloodgateHook` scaling.
- **Config:** `checks.movement.fastclimb.fastclimb_a`.

### NoSlowA — `NoSlow_A` / `Cobweb`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/noslow/NoSlowA.java:24`
- **Enum:** `NOSLOW_A`
- **Signal:** Speed inside cobweb: both `from` and `to` within contain `COBWEB` (or `WEB` legacy) and every within block is passable or cobweb (`NoSlowA.java:77-99`). Needs `cobwebEvents>2`. Flags if `horizontal > (0.063701*2+0.28062)/3` (`≈0.136`) scaled by SPEED, or `vertical fall magnitude > (0.00392*2+0.30432)/3` (`≈0.104`) (`NoSlowA.java:105-118`). Attribute `GENERIC_MOVEMENT_EFFICIENCY` grace 4s.
- **Lane:** `ASYNC_PLAYER_MOVE + beforeMovement` (LEVITATION/JUMP>5 → effectTime 1s).
- **Exemptions/hooks:** `isConditionAllowed` (`NoSlowA.java:30`) 18 timestamps.
- **Config:** `checks.movement.noslow.noslow_a`.

### StepA — `Step_A` / `Step`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/step/StepA.java:23`
- **Enum:** `STEP_A`
- **Signal:** Single-tick step >1.5 blocks: `vSpeed=distVertical(from,to)` (`StepA.java:78`); requires `>0 && >=1.5`, `onGround FROM false` in both histories (`StepA.java:84`), and `isBlockHeight(fromY)` (`StepA.java:87`). Attribute `GENERIC_STEP_HEIGHT` grace 4s. `FloodgateHook.isCancelledMovement` exemption.
- **Lane:** `ASYNC_PLAYER_MOVE`.
- **Exemptions/hooks:** `isConditionAllowed` (`StepA.java:30`) 18 timestamps; `LEVITATION>0 || SLOW_FALLING>1 || JUMP>2` abort.
- **Config:** `checks.movement.step.step_a` (low thresholds: punish 3, setback 4).

### BoatA — `Boat_A` / `Boat`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/boat/BoatA.java:33`
- **Enum:** `BOAT_A`
- **Signal:** Dual, **sync** movement lane (unlike most movement checks):
  - `boatFlight` (`BoatA.java:65`): vertical `from->to` vs `VanillaVerticalPhysics.boatVerticalSpeed(boatFlightEvents)` with `*1.4` (positive) / `*0.7` (negative) +0.1, `flags 0..4` accumulator, violates at `>3` (`BoatA.java:169-182`).
  - `boatSpeed` (`BoatA.java:190`): horizontal `min(from->to, previous->to/2)` vs `max 3.65*1.35≈4.9275` divided by `3.0` (land) or `2.7` (over liquid) unless ICE (`BoatA.java:265-278`). Liquid via WATER within, ice via `ICE/PACKED_ICE/BLUE_ICE` down.
- **Lane:** `LACPlayerMoveEvent` (`PLAYER_MOVE:POSITION`, `BoatA.java:40-41`) — runs on main thread (needs `boat.getLocation()`, `isOnGround`). Requires `isInsideVehicle true` (`BoatA.java:45`), `entity BOAT`.
- **Exemptions/hooks:** `isConditionAllowed` requires insideVehicle; 16 timestamps (flight, slime/honey etc.); `entityCollision 3s`, `slimeHoneyTime 7.5s`, `boat.isOnGround+fallDistance==0` abort, `boat.isInsideVehicle` abort, `Floodgate` reduces flight events by 2.
- **Config:** `checks.movement.boat.boat_a`.

### VehicleA — `Vehicle_A` / `Vehicle`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/vehicle/VehicleA.java:25`
- **Enum:** `VEHICLE_A`
- **Signal:** **Packet lane** `LACAsyncPacketReceiveEvent STEER_VEHICLE` (`VehicleA.java:54`). Only `HORSE`/`MULE`/`PIG`. Dual threshold: horizontal `>3.65*1.35` (`VehicleA.java:132`) or vertical not decaying: `previous+0.002 >= current` for `verticalFlags>=4` while airborne (`isBlockHeight false` both locs and `!isOnGround(0.5)`) and solid down absent (`VehicleA.java:136-155`). Within-block impassable resets.
- **Lane:** `ASYNC_PACKET_RECEIVE`.
- **Exemptions/hooks:** `isConditionAllowed` requires insideVehicle (`VehicleA.java:32`); same 16-timestamp family; 3-event warmup.
- **Config:** `checks.movement.vehicle.vehicle_a`.

### ElytraA — `Elytra_A` / `Speed`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/elytra/ElytraA.java:27`
- **Enum:** `ELYTRA_A`
- **Signal:** Two mutually-triggered behaviours while `isGliding` (`ElytraA.java:41` requires gliding && `glidingTicks>3`):
  - `theSameSpeed` (`ElytraA.java:63`): `speed` (3D dist) and `hSpeed` stay constant within `0.00005` across all history with `speed>=0.25/hSpeed>=0.15` and `pitchDifference>=1.2` after `theSameSpeedEvents>5` (`ElytraA.java:119-138`).
  - `tooLowSpeed` (`ElytraA.java:143`): `dist<=0.025` across all history with `pitch/yaw change >=0.25` after `tooLowSpeedEvents>10` (`ElytraA.java:202-218`).
- **Lane:** `ASYNC_PLAYER_MOVE` dual + `beforeMovement` (LEVITATION/SLOW_FALLING → effectTime 1s); `CooldownUtil skip` for tooLow when clean.
- **Exemptions/hooks:** 20 timestamps including `lastFireworkBoost 4500/7000`, `lastRiptiding 15s`; passable+downPassable required; `onGround` any history aborts; `updateDownBlocks` for tooLow.
- **Config:** `checks.movement.elytra.elytra_a`.

### ElytraB — `Elytra_B` / `Acceleration`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/elytra/ElytraB.java:27`
- **Enum:** `ELYTRA_B`
- **Signal:** Pitch/acceleration based: while gliding, requires `glidingEvents>8`, `verticalSpeed>0.05 && previous>0.05` (`ElytraB.java:129`), and `speed` & `previous & prePrevious` strictly increasing beyond `+0.00005 - offset` where `offset {0,0.00003,0.00006,0.00012}` by speed tier (`ElytraB.java:133-136`). Violates with repeat `900`.
- **Lane:** `ASYNC_PLAYER_MOVE + beforeMovement`.
- **Exemptions/hooks:** Same `isConditionAllowed` as ElytraA (gliding), 20 timestamps, interactive passable, `onGround` abort, LEVITATION/SLOW_FALLING effect 1s.
- **Config:** `checks.movement.elytra.elytra_b`.

### ElytraC — `Elytra_C` / `Takeoff`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/elytra/ElytraC.java:30`
- **Enum:** `ELYTRA_C`
- **Signal:** Fast takeoff without firework: precomputed max horizontal takeoff curves `TICK_SPEEDS[4..40]` (by `glidingTicks`) and `EVENT_SPEEDS[2..38]` (by `glidingEvents`) (`ElytraC.java:171-248`). Flags when `min(horizontal, avgHorizontal/ `FIRST->to/2`) >= max(maxTick, maxEvent)*1.6 +0.35` after `glidingEvents>1` (`ElytraC.java:148`). Requires `isGliding`, no firework boost `6000/8000ms`.
- **Lane:** `ASYNC_PLAYER_MOVE + beforeMovement`.
- **Exemptions/hooks:** `isConditionAllowed` (`ElytraC.java:57`) firework/riptiding gates; passable, no `FROM onGround`, `effectTime` LEVITATION/SLOW_FALLING 1s, interactive grace.
- **Config:** `checks.movement.elytra.elytra_c`.

### TridentA — `Trident_A` / `TridentBoost`
- **File:** `src/main/java/me/vekster/lightanticheat/check/checks/movement/trident/TridentA.java:28`
- **Enum:** `TRIDENT_A`
- **Signal:** Horizontal riptide boost while `lacPlayer.isRiptiding()==true` (`TridentA.java:101`): `hSpeed=distanceHorizontal(from,to)` vs `max 3.5*1.25=4.375` (`TridentA.java:121`). Exempt if held trident has `RIPTIDE>3` (main or offhand) (`TridentA.java:104-112`). Requires `cancelTime 1s` since `isConditionAllowed==false` and `longGliding 3.75s / longRiptiding 7.5s` not active (`TridentA.java:115-118`), `isConditionAllowed` notably allows `isInWater` / `isGliding`/`isRiptiding` (only flying/vehicle/climbing blocked, `TridentA.java:35`). Within blocks must be passable or WATER.
- **Lane:** `ASYNC_PLAYER_MOVE + beforeMovement` (SPEED>2 / DOLPHINS>1 → effectTime 500ms).
- **Exemptions/hooks:** `isConditionAllowed` 15 timestamps (firework, slime/honey etc., no windCharge gates); long gliding/riptiding accumulators when `glidingTicks>5`/`riptidingTicks>5`.
- **Config:** `checks.movement.trident.trident_a` (punish 3, setback 7).

---

## Config reference (all 24)

Values shown are defaults from `src/main/resources/config.yml:185-574`; `recommended-config.yml:182-571` tightens `min-tps 18.0`, `max-ping 300`, `bedrock: false` for stricter prod.

```
checks.movement.flight.flight_a   checks.movement.flight.flight_b   checks.movement.flight.flight_c
checks.movement.speed.speed_a     checks.movement.speed.speed_b     checks.movement.speed.speed_c
checks.movement.speed.speed_d     checks.movement.speed.speed_e     checks.movement.speed.speed_f
checks.movement.nofall.nofall_a   checks.movement.nofall.nofall_b
checks.movement.jump.jump_a       checks.movement.jump.jump_b
checks.movement.liquidwalk.liquidwalk_a  checks.movement.liquidwalk.liquidwalk_b
checks.movement.fastclimb.fastclimb_a
checks.movement.noslow.noslow_a
checks.movement.step.step_a
checks.movement.boat.boat_a
checks.movement.vehicle.vehicle_a
checks.movement.elytra.elytra_a   checks.movement.elytra.elytra_b   checks.movement.elytra.elytra_c
checks.movement.trident.trident_a
```

Per-check subtree always contains `enabled`, `punishment{punishable, punishment-vio, commands:[kick %name% %check%]}`, `setback{setback, setback-vio}`, `detection{min-tps, max-ping, java, bedrock}` (see `config.yml` excerpt above for exact `punishment-vio`/`setback-vio` per check).
