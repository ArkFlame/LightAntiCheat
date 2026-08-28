# Combat Checks

Base class: `src/main/java/me/vekster/lightanticheat/check/checks/combat/CombatCheck.java:17` — `CombatCheck extends Check`. Provides `distanceToHitbox(Player,Entity)` via `Ray.from(player)` / `AABB.from(entity).collidesD(Ray,0,16)` and inner types `CombatCheck.Ray` / `CombatCheck.AABB`. `flyingEntities` exempt set: `PARROT`, `BAT`, `BLAZE`, `GHAST`, `SQUID`, `SALMON`, `COD`, `TROPICAL_FISH`, `DOLPHIN` (`CombatCheck.java:22`).

All combat checks use `isCheckAllowed(...)` gating (enabled / `detection.min-tps` / `detection.max-ping` / `detection.java` / `detection.bedrock` / bypass) and `Buffer` for state/cooldowns.

---

## KillAuraA — AimBot

- **Display name:** `KillAuraA` (`CheckName.java:28`)
- **Enum:** `KILLAURA_A` — **Title:** `KillAura_A`
- **Enum description:** `AimBot`
- **High-level signal:** Head-rotation pattern: attack fires while yaw/pitch delta across `SECOND->FIRST` and `FIRST->FROM` history is `<0.01` (no look), arms `listen` for 5s; subsequent `ASYNC_PLAYER_MOVE` with `ROTATION` delta `>15` increments `flags`; flags `>3` violations (`killaura/KillAuraA.java:48`, `killaura/KillAuraA.java:64`).
- **Event / input lane:** `LACEventType.ASYNC_PLAYER_ATTACK` (`LACAsyncPlayerAttackEvent`) + `LACEventType.ASYNC_PLAYER_MOVE` with `LACMovementRequirement.ROTATION` (`killaura/KillAuraA.java:32`).
- **Exemptions / hooks:** `isCheckAllowed(player,lacPlayer,true)` only; no Floodgate/sweep/velocity guards in this check. History sourced from `cache.history.onEvent.location`.
- **Setback:** `setback: false`, `setback-vio: 4`.
- **Config path:** `checks.combat.killaura.killaura_a` (`config.yml:578`).

## KillAuraB — HitBox

- **Display name:** `KillAuraB` (`CheckName.java:29`)
- **Enum:** `KILLAURA_B` — **Title:** `KillAura_B`
- **Enum description:** `HitBox`
- **High-level signal:** Ray misses expanded hitbox (`distanceToHitbox == -1`) and horizontal angle to padded entity corners exceeds `atan(halfDiagonal/distance)` + distance-scaled `extraOffset` + motion/Bedrock slop (`killaura/KillAuraB.java:77`, `killaura/KillAuraB.java:121`); defers 1 tick re-check of `distanceToHitbox` before flagging.
- **Event / input lane:** `LACEventType.ASYNC_PLAYER_ATTACK` (`LACAsyncPlayerAttackEvent`), with `LACPlayerManager.executeLater(...,1L,...)` deferred confirmation (`killaura/KillAuraB.java:36`, `killaura/KillAuraB.java:136`).
- **Exemptions / hooks:** `isCheckAllowed`; `isGliding`/`isRiptiding` early return; entity width `>2.0` or `10x10` excluded; `FloodgateHook.isProbablyPocketEditionPlayer`/`isBedrockPlayer` (`+15` angle each); `AccuracyUtil.isViolationCancel`; `PLAYER_SWEEPING_DAMAGE_RATIO` attribute/item suppress 3500 ms; 200 ms `lastFlagTime` throttle; `CooldownUtil.getAllEntitiesAsync` lookup (`killaura/KillAuraB.java:48`, `killaura/KillAuraB.java:71`, `killaura/KillAuraB.java:126`, `killaura/KillAuraB.java:152`).
- **Setback:** `setback: false`, `setback-vio: 5`.
- **Config path:** `checks.combat.killaura.killaura_b` (`config.yml:593`).

## KillAuraC — ThroughBlock

- **Display name:** `KillAuraC` (`CheckName.java:30`)
- **Enum:** `KILLAURA_C` — **Title:** `KillAura_C`
- **Enum description:** `ThroughBlock`
- **High-level signal:** Attacker fully enclosed in occluding blocks (`getWithinBlocks` all `isNotHittableThrough`) — or victim enclosed on sync hit — implies hit through wall (`killaura/KillAuraC.java:49`, `killaura/KillAuraC.java:86`).
- **Event / input lane:** Dual: `LACEventType.ASYNC_PLAYER_ATTACK` (`onAsyncHit`) + `LACEventType.PLAYER_ATTACK` (`onHit`, entity-attack cause only) (`killaura/KillAuraC.java:34`).
- **Exemptions / hooks:** `isGliding`/`isRiptiding`/`isInsideVehicle`; immured cleared if attacker/victim history `distance(FROM,FIRST) >= 0.175` on either `onEvent`/`onPacket`; `isNotHittableThrough` covers `isOccluding`, `GLASS`/`*_glass`, `TALL_GRASS`/`LARGE_FERN`/`SUNFLOWER`/`LILAC`/`ROSE_BUSH`/`PEONY`/`SUGAR_CANE`/`PITCHER_PLANT` (`killaura/KillAuraC.java:45`, `killaura/KillAuraC.java:58`, `killaura/KillAuraC.java:111`).
- **Setback:** `setback: false`, `setback-vio: 3`.
- **Config path:** `checks.combat.killaura.killaura_c` (`config.yml:608`).

## KillAuraD — Impossible

- **Display name:** `KillAuraD` (`CheckName.java:31`)
- **Enum:** `KILLAURA_D` — **Title:** `KillAura_D`
- **Enum description:** `Impossible`
- **High-level signal:** Two sub-checks: (1) **MultiAura** — >1 distinct attack per tick (`35 - min(ping/40,10)` ms window on both async+sync layers); (2) **Shield** — attack while `isBlocking`/`isSleeping`/`isDead` with `lastShieldAsyncFlag` within buffer window (`killaura/KillAuraD.java:44`, `killaura/KillAuraD.java:87`).
- **Event / input lane:** Four registrations: `ASYNC_PLAYER_ATTACK`→`multiAuraAsync`, `PLAYER_ATTACK`→`multiAura`, `ASYNC_PLAYER_ATTACK`→`shieldAsync`, `PLAYER_ATTACK`→`shield` (`killaura/KillAuraD.java:31`).
- **Exemptions / hooks:** MultiAura: `FloodgateHook.isProbablyPocketEditionPlayer`, `LivingEntity` only, `PLAYER_SWEEPING_DAMAGE_RATIO` suppress 2000 ms, 750 ms `lastFlag` throttle, ping-scaled interval. Shield: `blockingTicks < 2` guard when `isBlocking` (`killaura/KillAuraD.java:67`, `killaura/KillAuraD.java:76`, `killaura/KillAuraD.java:109`).
- **Setback:** `setback: false`, `setback-vio: 1`.
- **Config path:** `checks.combat.killaura.killaura_d` (`config.yml:623`).

## ReachA — Horizontal

- **Display name:** `ReachA` (`CheckName.java:32`)
- **Enum:** `REACH_A` — **Title:** `Reach_A`
- **Enum description:** `Horizontal`
- **High-level signal:** Horizontal `distanceHorizontal(eye,entity) - halfDiagonal` exceeds `3.0 + backwardsDistance*(1+ping/1000*20) + targetBackwardsDistance`, capped `7.5`, plus `+2.5` non-survival/adventure, `+0.5` projectile, `+0.675` lenience; `flags>1` flags (`reach/ReachA.java:51`, `reach/ReachA.java:90`, `reach/ReachA.java:104`).
- **Event / input lane:** `LACEventType.PLAYER_ATTACK` (`LACPlayerAttackEvent`, `isEntityAttackCause` only) (`reach/ReachA.java:33`).
- **Exemptions / hooks:** `10x10` entity excluded; `EliteMobsHook` `+0.35` vs `flyingEntities`/`isLiquid` `+0.25` airborne; `PLAYER_ENTITY_INTERACTION_RANGE` attribute suppress 2000 ms; backwards compensation from `onEvent`/`onPacket` histories for attacker and `Player` victim (`reach/ReachA.java:47`, `reach/ReachA.java:81`, `reach/ReachA.java:92`, `reach/ReachA.java:108`).
- **Setback:** `setback: false`, `setback-vio: 6`.
- **Config path:** `checks.combat.reach.reach_a` (`config.yml:639`).

## ReachB — Hitbox

- **Display name:** `ReachB` (`CheckName.java:33`)
- **Enum:** `REACH_B` — **Title:** `Reach_B`
- **Enum description:** `Hitbox`
- **High-level signal:** Ray-to-AABB `distanceToHitbox` (`-1` = looking away, ignored) exceeds same reach model as ReachA but cap `6.5` and adds `+0.5` when vertical distance dominates (`1.5*h < v`) plus `+0.45` lenience (`reach/ReachB.java:47`, `reach/ReachB.java:88`, `reach/ReachB.java:98`).
- **Event / input lane:** `LACEventType.PLAYER_ATTACK` (`LACPlayerAttackEvent`) (`reach/ReachB.java:33`).
- **Exemptions / hooks:** Same as ReachA: `EliteMobsHook`/`flyingEntities`/`isLiquid` airborne bonus, gamemode/projectile/height bonuses, `PLAYER_ENTITY_INTERACTION_RANGE` suppress 2000 ms (`reach/ReachB.java:79`, `reach/ReachB.java:90`, `reach/ReachB.java:111`).
- **Setback:** `setback: false`, `setback-vio: 4`.
- **Config path:** `checks.combat.reach.reach_b` (`config.yml:654`).

## CriticalsA — Packet

- **Display name:** `CriticalsA` (`CheckName.java:34`)
- **Enum:** `CRITICALS_A` — **Title:** `Criticals_A`
- **Enum description:** `Packet`
- **High-level signal:** Packet/bypass critical: attacker off-ground with `fallDistance>0` and standing over passable void (`getWithinBlocks` passable + `getDownBlocks(0.1)` finds ground) yet `isBlockHeight(blockY)` critical height fires; async variant requires tight `packetHistory` Y range (`max-min <0.3`) with a bounce (`|y-previous|<LOWEST_BLOCK_HEIGHT`) (`criticals/CriticalsA.java:78`, `criticals/CriticalsA.java:88`, `criticals/CriticalsA.java:174`).
- **Event / input lane:** `LACEventType.PLAYER_ATTACK` (`onHit`) + `LACEventType.ASYNC_PLAYER_ATTACK` (`onAsyncHit`) (`criticals/CriticalsA.java:37`).
- **Exemptions / hooks:** `ValhallaMMOHook` skip; async: `FloodgateHook.isBedrockPlayer` skip; survival/adventure only; `BLINDNESS`/`LEVITATION` skip; `isFlying`/`isInsideVehicle`/`isGliding`/`isRiptiding`/`isClimbing`/`isInWater`/`flyingTicks`/`climbingTicks`/`glidingTicks`/`riptidingTicks`; cooldowns `lastInsideVehicle`/`lastInWater`/`lastWasFished`/`lastTeleport`/`lastRespawn`/`lastEntityVeryNearby`/`lastSlimeBlock`/`lastHoneyBlock`/`lastWasHit`/`lastWasDamaged`/`lastKbVelocity`; `PLAYER_SWEEPING_DAMAGE_RATIO` suppress 2500 ms (`criticals/CriticalsA.java:44`, `criticals/CriticalsA.java:70`, `criticals/CriticalsA.java:116`, `criticals/CriticalsA.java:178`).
- **Setback:** `setback: false`, `setback-vio: 1`.
- **Config path:** `checks.combat.criticals.criticals_a` (`config.yml:670`).

## CriticalsB — MiniJump

- **Display name:** `CriticalsB` (`CheckName.java:35`)
- **Enum:** `CRITICALS_B` — **Title:** `Criticals_B`
- **Enum description:** `MiniJump`
- **High-level signal:** Mini-jump critical: falling/tiny airborne attack just off ground with `verticalDistance(maxY-minY)<0.75` over last 10 ticks, validated by three deferred `getMinDownHeight` samples strictly below `max(HEIGHTS)` floor baseline (`criticals/CriticalsB.java:130`, `criticals/CriticalsB.java:151`).
- **Event / input lane:** `LACEventType.ASYNC_PLAYER_ATTACK` + `LACEventType.ASYNC_PLAYER_MOVE` (`LACMovementRequirement.POSITION`) for floor tracking; `PlayerJoinEvent`/`PlayerQuitEvent` maintain `HEIGHTS` ring buffer (40 entries) (`criticals/CriticalsB.java:44`, `criticals/CriticalsB.java:163`).
- **Exemptions / hooks:** Same state guards as CriticalsA plus `isOnGround(0.1,TRUE)` early return, `distanceVertical(FROM,current) >= -0.001` falling check, 5-tick onGround history must be airborne, within/down-layer passability including `+UP`/`-1..-3` layers, `BLINDNESS`/`LEVITATION` (`criticals/CriticalsB.java:57`, `criticals/CriticalsB.java:74`, `criticals/CriticalsB.java:83`, `criticals/CriticalsB.java:109`).
- **Setback:** `setback: false`, `setback-vio: 1`.
- **Config path:** `checks.combat.criticals.criticals_b` (`config.yml:685`).

## AutoClickerA — Pattern

- **Display name:** `AutoClickerA` (`CheckName.java:36`)
- **Enum:** `AUTOCLICKER_A` — **Title:** `AutoClicker_A`
- **Enum description:** `Pattern`
- **High-level signal:** Consistent inter-click interval delta `abs(interval-lastInterval)` in `{49,50,51}` ms for >5 consecutive clicks (`autoclicker/AutoClickerA.java:52`).
- **Event / input lane:** `PlayerInteractEvent` (excluding `Action.PHYSICAL`) via `LACPlayerManager.execute(player,false,...)` (`autoclicker/AutoClickerA.java:22`).
- **Exemptions / hooks:** `isExternalNPC` (NPC) exempt; 3000 ms `lastFlag` throttle; no Geyser/attribute guards in this check.
- **Setback:** `setback: false`, `setback-vio: 2`.
- **Config path:** `checks.combat.autoclicker.autoclicker_a` (`config.yml:701`).

## AutoClickerB — Impossible

- **Display name:** `AutoClickerB` (`CheckName.java:37`)
- **Enum:** `AUTOCLICKER_B` — **Title:** `AutoClicker_B`
- **Enum description:** `Impossible`
- **High-level signal:** Peak CPS `>=47` from `CPSListener.getCurrentCps(player)` on interact (`autoclicker/AutoClickerB.java:35`).
- **Event / input lane:** `PlayerInteractEvent` (excluding `Action.PHYSICAL`) via `LACPlayerManager.execute` (`autoclicker/AutoClickerB.java:24`).
- **Exemptions / hooks:** `isExternalNPC` exempt; 2000 ms `lastFlag` throttle.
- **Setback:** `setback: false`, `setback-vio: 2`.
- **Config path:** `checks.combat.autoclicker.autoclicker_b` (`config.yml:716`).

## VelocityA — AntiKnockback

- **Display name:** `VelocityA` (`CheckName.java:38`)
- **Enum:** `VELOCITY_A` — **Title:** `Velocity_A`
- **Enum description:** `AntiKnockback`
- **High-level signal:** After `ENTITY_ATTACK` damage + `PlayerVelocityEvent`, player displacement over 1 tick (`lastLocation`→current) remains `<0.00045` 3D and `<0.000045` horizontal across two deferred `detect` calls; requires `flags>1` (`velocity/VelocityA.java:156`, `velocity/VelocityA.java:167`).
- **Event / input lane:** `EntityDamageByEntityEvent` (`ENTITY_ATTACK` on `Player` only) + `PlayerVelocityEvent` → `detect(player,false)→detect(player,true)` one tick apart (`velocity/VelocityA.java:54`, `velocity/VelocityA.java:93`, `velocity/VelocityA.java:134`).
- **Exemptions / hooks:** PandaSpigot 1.8 no-op (`VerIdentifier==V1_8 && serverName contains PandaSpigot`); `Vehicle` within `2,3,2`; initial velocity `<0.2` xz ignored; `NETHERITE_ARMOR` full-set check; `EnchantsSquaredHook` `Steady`/`Burden`; `GENERIC_KNOCKBACK_RESISTANCE` attribute `>0.41` suppress 3500 ms; survival/adventure only; `LEVITATION`; `isFlying`/`isInsideVehicle`/`isGliding`/`isRiptiding`/`isClimbing`/`isInWater` + `flyingTicks` etc + `lastInsideVehicle`/`lastInWater`/`lastWasFished`/`lastTeleport`/`lastRespawn`/`lastPowderSnowWalk`/`lastSlimeBlock`/`lastHoneyBlock`/`lastFlight`/`lastGliding`/`lastRiptiding` windows; ground history requires recent `towardsFalse` (`velocity/VelocityA.java:44`, `velocity/VelocityA.java:120`, `velocity/VelocityA.java:172`, `velocity/VelocityA.java:184`).
- **Setback:** `setback: false`, `setback-vio: 7`.
- **Config path:** `checks.combat.velocity.velocity_a` (`config.yml:732`).
