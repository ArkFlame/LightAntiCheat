package me.vekster.lightanticheat.check.checks.movement.flight;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.movement.MovementCheck;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.event.playerplaceblock.LACAsyncPlayerPlaceBlockEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.cache.PlayerCache;
import me.vekster.lightanticheat.player.cache.history.HistoryElement;
import me.vekster.lightanticheat.util.async.AsyncUtil;
import me.vekster.lightanticheat.util.detection.CheckUtil;
import me.vekster.lightanticheat.util.hook.plugin.simplehook.EnchantsSquaredHook;
import me.vekster.lightanticheat.util.physics.VanillaVerticalPhysics;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import me.vekster.lightanticheat.version.VerUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Set;

/**
 * Maximum possible height
 */
public class FlightB extends MovementCheck implements Listener {


    public FlightB() {
        super(CheckName.FLIGHT_B);
    }

    @Override
    public boolean isConditionAllowed(Player player, LACPlayer lacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing || isGliding || isRiptiding || isInWater)
            return false;
        if (cache.flyingTicks >= -10 || cache.climbingTicks >= -2 ||
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

    @EventHandler
    public void onAsyncMovement(LACAsyncPlayerMoveEvent event) {
        LACPlayer lacPlayer = event.getLacPlayer();
        PlayerCache cache = lacPlayer.cache;
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        boolean isInteractiveBlock = false;
        for (Block block : event.getToInteractiveBlocks()) {
            if (!isActuallyPassable(block) && getAngle(player, block) <= 100) {
                isInteractiveBlock = true;
                break;
            }
        }
        if (isInteractiveBlock) {
            if (buffer.getInt("interactiveOffset") == 0) {
                buffer.put("interactiveOffset", 1);
                Block block = AsyncUtil.getBlock(event.getTo());
                if (block != null) {
                    buffer.put("interactiveBlock", block);
                } else {
                    buffer.put("flightTicks", 0);
                    updateStartLocation(event.getTo(), false, 0.5, buffer);
                    buffer.put("interactiveOffset", 0);
                    return;
                }
            } else if (buffer.getBlock("interactiveBlock") != null) {
                Block block = AsyncUtil.getBlock(event.getTo());
                if (block == null) {
                    buffer.put("flightTicks", 0);
                    updateStartLocation(event.getTo(), false, 0.5, buffer);
                    buffer.put("interactiveOffset", 0);
                    return;
                }
                Block previousBlock = buffer.getBlock("interactiveBlock");
                if (previousBlock.getX() != block.getX() || previousBlock.getZ() != block.getZ()) {
                    if (previousBlock.getY() < block.getY()) {
                        buffer.put("interactiveOffset", buffer.getInt("interactiveOffset") + 1);
                        buffer.put("interactiveBlock", block);
                    }
                }
            }
        }

        if (!isCheckAllowed(player, lacPlayer, true)) {
            buffer.put("flightTicks", 0);
            updateStartLocation(event.getTo(), false, 0.5, buffer);
            buffer.put("interactiveOffset", 0);
            return;
        }

        if (!isConditionAllowed(player, lacPlayer, event)) {
            buffer.put("flightTicks", 0);
            updateStartLocation(event.getTo(), false, 0.5, buffer);
            buffer.put("interactiveOffset", 0);
            return;
        }

        if (!event.isToWithinBlocksPassable() || !event.isFromWithinBlocksPassable()) {
            buffer.put("flightTicks", 0);
            updateStartLocation(event.getTo(), false, 0.5, buffer);
            buffer.put("interactiveOffset", 0);
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (currentTime - cache.lastEntityNearby <= 1000) {
            buffer.put("flightTicks", 0);
            updateStartLocation(event.getTo(), false, 0.5, buffer);
            buffer.put("interactiveOffset", 0);
            return;
        }

        if (currentTime - buffer.getLong("effectTime") <= 2000) {
            buffer.put("flightTicks", 0);
            updateStartLocation(event.getTo(), false, 1.0, buffer);
            buffer.put("interactiveOffset", 0);
            return;
        }

        if (distanceAbsVertical(event.getFrom(), event.getTo()) < CheckUtil.LOWEST_BLOCK_HEIGHT &&
                cache.history.onEvent.onGround.get(HistoryElement.FROM).towardsFalse)
            buffer.put("lastVelocityTime", 0);
        long velocityBypass = 750L + 1500L * getEffectAmplifier(cache, VerUtil.potions.get("SLOW_FALLING"));
        if (currentTime - buffer.getLong("lastVelocityTime") < velocityBypass) {
            buffer.put("flightTicks", 0);
            updateStartLocation(event.getTo(), false, 1.0, buffer);
            buffer.put("interactiveOffset", 0);
            return;
        }

        if (cache.history.onEvent.onGround.get(HistoryElement.FROM).towardsTrue ||
                cache.history.onPacket.onGround.get(HistoryElement.FROM).towardsTrue) {
            updateStartLocation(event.getTo(), false, 0.0, buffer);
            buffer.put("flightTicks", 0);
            buffer.put("interactiveOffset", 0);
            return;
        }

        if (cache.history.onEvent.onGround.get(HistoryElement.FIRST).towardsTrue ||
                cache.history.onPacket.onGround.get(HistoryElement.FIRST).towardsTrue) {
            buffer.put("flightTicks", 0);
            buffer.put("interactiveOffset", 0);
            return;
        }

        if (event.getFrom().getBlockY() > event.getTo().getBlockY() ||
                event.getFrom().getY() > event.getTo().getY() && getBlockY(event.getTo().getY()) == 0) {
            if (!event.isToDownBlocksPassable()) {
                buffer.put("flightTicks", 0);
                updateStartLocation(event.getTo(), false, 0.0, buffer);
                buffer.put("interactiveOffset", 0);
                return;
            }
            for (Block block : event.getToDownBlocks()) {
                if (!isActuallyPassable(block.getRelative(BlockFace.DOWN))) {
                    buffer.put("flightTicks", 0);
                    updateStartLocation(event.getTo(), false, 0.0, buffer);
                    buffer.put("interactiveOffset", 0);
                    return;
                }
            }
        }

        if (getEffectAmplifier(cache, VerUtil.potions.get("JUMP")) == 0 &&
                currentTime - buffer.getLong("justEffectTime") < 100) {
            updateStartLocation(event.getTo(), false, 0.0, buffer);
            buffer.put("flightTicks", 0);
            buffer.put("interactiveOffset", 0);
        }

        buffer.put("flightTicks", buffer.getInt("flightTicks") + 1);
        if (buffer.getInt("flightTicks") <= 1 || buffer.getLocation("startLocation") == null)
            return;

        if (currentTime - buffer.getLong("lastScaffoldPlace") <= 400L) {
            buffer.put("flightTicks", 0);
            updateStartLocation(event.getTo(), false, 0.0, buffer);
            return;
        }

        double height = distanceVertical(buffer.getLocation("startLocation"), event.getTo());
        int jumpEffectAmplifier = getEffectAmplifier(cache, VerUtil.potions.get("JUMP"));
        if (jumpEffectAmplifier > 2)
            height -= (jumpEffectAmplifier - 2) * 0.2;
        height = height * 0.9 - 0.1 - buffer.getInt("interactiveOffset");

        double attributeAmount = Math.max(
                getItemStackAttributes(player, "GENERIC_JUMP_STRENGTH"),
                getPlayerAttributes(player).getOrDefault("GENERIC_JUMP_STRENGTH", 0.42) - 0.42
        );
        if (attributeAmount != 0)
            buffer.put("attribute", System.currentTimeMillis());
        else if (System.currentTimeMillis() - buffer.getLong("attribute") < 4000)
            return;
        if (attributeAmount != 0) {
            if (attributeAmount <= 0.25)
                height -= 10.0;
            else if (attributeAmount <= 0.5)
                height -= 20.0;
            else if (attributeAmount <= 1.0)
                height -= 40.0;
            else
                height -= 80.0;
        }

        double maxHeight = VanillaVerticalPhysics.maxJumpHeight(jumpEffectAmplifier);
        if (height <= maxHeight)
            return;

        Set<Player> players = getPlayersForEnchantsSquared(lacPlayer, player);
        updateDownBlocks(player, lacPlayer, event.getToDownBlocks());
        double finalHeight = height;
        Scheduler.runTask(true, () -> {
            if (currentTime - buffer.getLong("lastScaffoldPlace") <= 400L ||
                    lacPlayer.isGliding() || lacPlayer.isRiptiding()) {
                buffer.put("flightTicks", 0);
                updateStartLocation(event.getTo(), false, 0.0, buffer);
                return;
            }

            if (isLagGlidingPossible(player, buffer)) {
                buffer.put("lastGlidingLagPossibleTime", System.currentTimeMillis());
                return;
            }
            if (isPingGlidingPossible(player, cache))
                return;

            if (EnchantsSquaredHook.hasEnchantment(player, "Burden") &&
                    finalHeight * 0.9 - 0.5 <= maxHeight)
                return;
            if (isEnchantsSquaredImpact(players) && finalHeight * 0.7 - 1.8 <= maxHeight)
                return;

            if (System.currentTimeMillis() - buffer.getLong("lastGlidingLagPossibleTime") < 5 * 1000)
                callViolationEventIfRepeat(player, lacPlayer, event, buffer, 500);
            else
                callViolationEventIfRepeat(player, lacPlayer, event, buffer, 900);
        });
    }

    private void updateStartLocation(Location location, boolean force, double lift, Buffer buffer) {
        Location startCandidate = location.clone().add(0, lift, 0);
        Location startLocation = buffer.getLocation("startLocation");
        if (force || startLocation == null) {
            buffer.put("startLocation", startCandidate);
            return;
        }
        if (startCandidate.getY() > startLocation.getY())
            buffer.put("startLocation", startCandidate);
    }

    private float getAngle(Player player, Block block) {
        Location blockLocation = block.getLocation();
        Location eyeLocation = player.getEyeLocation();
        Vector vector = blockLocation.toVector().setY(0.0D).subtract(eyeLocation.toVector().setY(0.0D));
        return eyeLocation.getDirection().setY(0.0D).normalize().angle(vector.normalize()) * 57.2958F;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void beforeMovement(LACAsyncPlayerMoveEvent event) {
        LACPlayer lacPlayer = event.getLacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        Buffer buffer = getBuffer(player, true);

        if (lacPlayer.cache.history.onEvent.onGround.get(HistoryElement.FROM).towardsTrue ||
                lacPlayer.cache.history.onPacket.onGround.get(HistoryElement.FROM).towardsTrue)
            updateStartLocation(event.getTo(), true, 0, buffer);


        if (getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("LEVITATION")) > 0 ||
                getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("JUMP")) > 32)
            buffer.put("effectTime", System.currentTimeMillis());

        if (getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("JUMP")) != 0)
            buffer.put("justEffectTime", System.currentTimeMillis());
    }

    @EventHandler
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

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        LACPlayer lacPlayer = LACPlayer.getLacPlayer(player);
        if (lacPlayer != null) {
            if (!isCheckAllowed(player, lacPlayer, true))
                return;
            Buffer buffer = getBuffer(player, true);
            updateStartLocation(event.getPlayer().getLocation(), true, 0.5, buffer);
        } else {
            Scheduler.runTaskLater(() -> {
                if (!player.isOnline()) return;
                LACPlayer lacPlayer1 = LACPlayer.getLacPlayer(player);
                if (lacPlayer1 == null) return;
                if (!isCheckAllowed(player, lacPlayer1, true))
                    return;
                Buffer buffer = getBuffer(player, true);
                updateStartLocation(event.getPlayer().getLocation(), false, 0.5, buffer);
            }, 1);
        }
    }

    @EventHandler
    public void onVelocity(PlayerVelocityEvent event) {
        if (CheckUtil.isExternalNPC(event)) return;
        double yVelocity = event.getVelocity().getY();
        if (yVelocity < -0.0784000015258789 + 0.005 &&
                yVelocity > -0.0784000015258789 - 0.005)
            return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("lastVelocityTime", System.currentTimeMillis());
    }

}
