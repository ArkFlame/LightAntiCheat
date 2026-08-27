package me.vekster.lightanticheat.check.checks.movement.flight;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.movement.MovementCheck;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.event.playerplaceblock.LACAsyncPlayerPlaceBlockEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.cache.PlayerCache;
import me.vekster.lightanticheat.player.cache.history.HistoryElement;
import me.vekster.lightanticheat.player.cache.history.PlayerCacheHistory;
import me.vekster.lightanticheat.util.hook.plugin.FloodgateHook;
import me.vekster.lightanticheat.util.hook.plugin.simplehook.EnchantsSquaredHook;
import me.vekster.lightanticheat.util.hook.plugin.simplehook.ValhallaMMOHook;
import me.vekster.lightanticheat.util.precise.AccuracyUtil;
import me.vekster.lightanticheat.util.physics.VanillaVerticalPhysics;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import me.vekster.lightanticheat.version.VerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.bus.LACMovementRequirement;

/**
 * Acceleration of free fall
 */
public class FlightA extends MovementCheck implements Listener {

    public FlightA() {
        super(CheckName.FLIGHT_A);
    }

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, this, "onAsyncMovement", LACMovementRequirement.POSITION, event -> onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, this, "beforeMovement", LACMovementRequirement.POSITION, event -> beforeMovement((LACAsyncPlayerMoveEvent) event));
        LACEventBus.register(LACEventType.ASYNC_PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, this, "scaffoldAsyncBlockPlace", event -> scaffoldAsyncBlockPlace((LACAsyncPlayerPlaceBlockEvent) event));
    }

    @Override
    public boolean isConditionAllowed(Player player, LACPlayer lacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing || isGliding || isRiptiding || isInWater)
            return false;
        if (cache.flyingTicks >= -25 || cache.climbingTicks >= -2 ||
                cache.glidingTicks >= -3 || cache.riptidingTicks >= -5)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastInsideVehicle > 150 && time - cache.lastInWater > 150 &&
                time - cache.lastKnockback > 750 && time - cache.lastKnockbackNotVanilla > 3000 &&
                time - cache.lastWasFished > 4000 && time - cache.lastTeleport > 700 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 700 &&
                time - cache.lastBlockExplosion > 5000 && time - cache.lastEntityExplosion > 3000 &&
                time - cache.lastSlimeBlockVertical > 4000 && time - cache.lastSlimeBlockHorizontal > 2500 &&
                time - cache.lastHoneyBlockVertical > 2500 && time - cache.lastHoneyBlockHorizontal > 1500 &&
                time - cache.lastPowderSnowWalk > 750 &&
                time - cache.lastWasHit > 350 && time - cache.lastWasDamaged > 150 &&
                time - cache.lastKbVelocity > 1000 && time - cache.lastAirKbVelocity > 2000 &&
                time - cache.lastStrongKbVelocity > 5000 && time - cache.lastStrongAirKbVelocity > 15 * 1000 &&
                time - cache.lastFlight > 750 &&
                time - cache.lastGliding > 2000 && time - cache.lastRiptiding > 3500 &&
                time - cache.lastWindCharge > 1000 && time - cache.lastWindChargeReceive > 500 &&
                time - cache.lastWindBurst > 1500 && time - cache.lastWindBurstNotVanilla > 4000;
    }

    public void onAsyncMovement(LACAsyncPlayerMoveEvent event) {
        LACPlayer lacPlayer = event.getLacPlayer();
        PlayerCache cache = lacPlayer.cache;
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, lacPlayer, true)) {
            buffer.put("flightTicks", 0);
            return;
        }

        if (!isConditionAllowed(player, lacPlayer, event)) {
            buffer.put("flightTicks", 0);
            return;
        }

        if (!event.isToWithinBlocksPassable() || !event.isFromWithinBlocksPassable()) {
            buffer.put("flightTicks", 0);
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (currentTime - cache.lastEntityNearby <= 1000) {
            buffer.put("flightTicks", 0);
            return;
        }

        if (currentTime - buffer.getLong("effectTime") <= 2000) {
            buffer.put("flightTicks", 0);
            return;
        }

        for (int i = 0; i < 3 && i < HistoryElement.count(); i++) {
            final HistoryElement element = HistoryElement.at(i);
            if (cache.history.onEvent.onGround.get(element).towardsTrue ||
                    cache.history.onPacket.onGround.get(element).towardsTrue) {
                buffer.put("flightTicks", 0);
                return;
            }
        }

        if (event.getFrom().getBlockY() > event.getTo().getBlockY() ||
                event.getFrom().getY() > event.getTo().getY() && getBlockY(event.getTo().getY()) == 0) {
            if (!event.isToDownBlocksPassable()) {
                buffer.put("flightTicks", 0);
                return;
            }
            for (Block block : event.getToDownBlocks()) {
                if (!isActuallyPassable(block.getRelative(BlockFace.DOWN))) {
                    buffer.put("flightTicks", 0);
                    return;
                }
            }
        }

        if (currentTime - buffer.getLong("lastScaffoldPlace") <= 400L) {
            buffer.put("flightTicks", 0);
            return;
        }

        buffer.put("flightTicks", buffer.getInt("flightTicks") + 1);
        int fallingTicks = buffer.getInt("flightTicks");

        int slowFallingEffectAmplifier = getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("JUMP"));
        if (getItemStackAttributes(player, "GENERIC_GRAVITY") != 0)
            slowFallingEffectAmplifier += 1;
        int jumpEffectAmplifier = getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("JUMP"));

        double attributeAmount = Math.max(
                getItemStackAttributes(player, "GENERIC_JUMP_STRENGTH"),
                getPlayerAttributes(player).getOrDefault("GENERIC_JUMP_STRENGTH", 0.42) - 0.42
        );
        if (attributeAmount != 0)
            buffer.put("attribute", System.currentTimeMillis());
        else if (System.currentTimeMillis() - buffer.getLong("attribute") < 4000)
            return;
        if (attributeAmount != 0) {
            if (attributeAmount <= 0.5) {
                if (attributeAmount <= 0.25 && jumpEffectAmplifier == 0)
                    jumpEffectAmplifier = 6;
                else
                    fallingTicks -= 90;
            } else if (attributeAmount <= 1.0) {
                fallingTicks -= 150;
            } else {
                fallingTicks -= 300;
            }
        }

        PlayerCacheHistory<Location> eventHistory = cache.history.onEvent.location;
        PlayerCacheHistory<Location> packetHistory = cache.history.onPacket.location;
        double verticalSpeed = Math.min(
                Math.min(
                        distanceVertical(event.getFrom(), event.getTo()),
                        decreaseVertical(distanceVertical(eventHistory.get(HistoryElement.FIRST), event.getTo()) / 2.0, 1.2)
                ),
                Math.min(
                        decreaseVertical(distanceVertical(packetHistory.get(HistoryElement.FIRST), packetHistory.get(HistoryElement.FROM)), 1.25),
                        decreaseVertical(distanceVertical(packetHistory.get(HistoryElement.SECOND), packetHistory.get(HistoryElement.FROM)) / 2.0, 1.25, 1.2)
                )
        );


        double calculatedVerticalSpeed = VanillaVerticalPhysics.flightVerticalLimit(
                jumpEffectAmplifier, fallingTicks, slowFallingEffectAmplifier != 0);

        if (calculatedVerticalSpeed > 0) verticalSpeed -= 0.05;
        verticalSpeed = decreaseVertical(verticalSpeed, 0.925);
        verticalSpeed -= 0.095;

        if (FloodgateHook.isBedrockPlayer(player, true)) {
            verticalSpeed -= 0.11;
        }
        if (FloodgateHook.isProbablyPocketEditionPlayer(player, true)) {
            verticalSpeed = decreaseVertical(verticalSpeed, 0.85);
            verticalSpeed -= 0.05;
        }

        if (fallingTicks <= 12) {
            if (distanceHorizontal(event.getFrom(), event.getTo()) > distanceAbsVertical(event.getFrom(), event.getTo()))
                verticalSpeed -= 0.15;
            else if (distanceHorizontal(event.getFrom(), event.getTo()) * 1.5 > distanceAbsVertical(event.getFrom(), event.getTo()))
                verticalSpeed -= 0.1;
        }

        if (System.currentTimeMillis() - buffer.getLong("fallingTime") < 4000) {
            verticalSpeed = decreaseVertical(verticalSpeed, 0.9);
            verticalSpeed -= 0.4;
        }

        if (verticalSpeed <= calculatedVerticalSpeed)
            return;

        Set<Player> players = getPlayersForEnchantsSquared(lacPlayer, player);
        updateDownBlocks(player, lacPlayer, event.getToDownBlocks());
        Scheduler.runTask(true, () -> {
            if (currentTime - buffer.getLong("lastScaffoldPlace") <= 400L ||
                    lacPlayer.isGliding() || lacPlayer.isRiptiding()) {
                buffer.put("flightTicks", 0);
                return;
            }

            if (isLagGlidingPossible(player, buffer)) {
                buffer.put("lastGlidingLagPossibleTime", System.currentTimeMillis());
                return;
            }
            if (isPingGlidingPossible(player, cache))
                return;

            if (EnchantsSquaredHook.hasEnchantment(player, "Burden"))
                return;
            if (isEnchantsSquaredImpact(players))
                return;

            if (AccuracyUtil.isViolationCancel(getCheckSetting(), buffer))
                return;

            if (ValhallaMMOHook.isPluginInstalled()) {
                if (System.currentTimeMillis() - buffer.getLong("firstLevelFlagTime") > 8000) {
                    buffer.put("firstLevelFlagTime", System.currentTimeMillis());
                    buffer.put("firstLevelFlags", 0);
                }

                buffer.put("firstLevelFlags", buffer.getInt("firstLevelFlags") + 1);
                if (buffer.getInt("firstLevelFlags") <= 9)
                    return;
            }

            if (System.currentTimeMillis() - buffer.getLong("lastGlidingLagPossibleTime") < 5 * 1000)
                callViolationEventIfRepeat(player, lacPlayer, event, buffer, 500);
            else
                callViolationEventIfRepeat(player, lacPlayer, event, buffer, 900);
        });
    }

    public void beforeMovement(LACAsyncPlayerMoveEvent event) {
        LACPlayer lacPlayer = event.getLacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        if (getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("LEVITATION")) > 0 ||
                getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("SLOW_FALLING")) > 1 ||
                getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("JUMP")) > 6) {
            Buffer buffer = getBuffer(player, true);
            buffer.put("effectTime", System.currentTimeMillis());
        }

        Location first = null;
        Location previous = null;
        for (int i = 0; i < 6 && i < HistoryElement.count(); i++) {
            Location location = lacPlayer.cache.history.onEvent.location.get(HistoryElement.at(i));
            if (previous == null) {
                first = location;
                previous = location;
                continue;
            }
            if (distanceVertical(location, previous) >= -0.05)
                break;
            if (i == 5) {
                if (distanceVertical(previous, first) >= -0.5)
                    break;
                Buffer buffer = getBuffer(player, true);
                buffer.put("fallingTime", System.currentTimeMillis());
            }
            previous = location;
        }
    }

    public void scaffoldAsyncBlockPlace(LACAsyncPlayerPlaceBlockEvent event) {
        if (isActuallyPassable(event.getBlock()))
            return;
        Block placedBlock = event.getBlock();
        boolean within = false;
        for (Block block : getWithinBlocks(event.getPlayer())) {
            if (!equals(placedBlock, block) &&
                    !equals(placedBlock, block.getRelative(BlockFace.DOWN)))
                continue;
            within = true;
            break;
        }
        if (!within)
            return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("lastScaffoldPlace", System.currentTimeMillis());
    }

    private static boolean equals(Block block1, Block block2) {
        return block1.getX() == block2.getX() &&
                block1.getY() == block2.getY() &&
                block1.getZ() == block2.getZ();
    }

    private static double decreaseVertical(double value, double multiplier) {
        return value >= 0 ? value * multiplier : value / multiplier;
    }

    private static double decreaseVertical(double value, double firstMultiplier, double secondMultiplier) {
        return decreaseVertical(decreaseVertical(value, firstMultiplier), secondMultiplier);
    }


}
