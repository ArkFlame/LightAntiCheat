package me.vekster.lightanticheat.check.checks.movement.nofall;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.movement.MovementCheck;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.bus.LACMovementRequirement;
import me.vekster.lightanticheat.event.playerbreakblock.LACAsyncPlayerBreakBlockEvent;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.cache.PlayerCache;
import me.vekster.lightanticheat.player.cache.history.HistoryElement;
import me.vekster.lightanticheat.util.hook.plugin.simplehook.EnchantsSquaredHook;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import me.vekster.lightanticheat.version.VerUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

/**
 * Spoof of the fall distance
 */
public class NoFallA extends MovementCheck implements Listener {

    public NoFallA() {
        super(CheckName.NOFALL_A);
    }

    @Override
    public boolean isConditionAllowed(Player player, LACPlayer lacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing || isGliding || isRiptiding || isInWater)
            return false;
        if (cache.flyingTicks >= -5 || cache.climbingTicks >= -2 ||
                cache.glidingTicks >= -3 || cache.riptidingTicks >= -5)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastInsideVehicle > 150 && time - cache.lastInWater > 150 &&
                time - cache.lastKnockback > 750 && time - cache.lastKnockbackNotVanilla > 3000 &&
                time - cache.lastWasFished > 4000 && time - cache.lastTeleport > 900 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 700 &&
                time - cache.lastBlockExplosion > 4000 && time - cache.lastEntityExplosion > 2000 &&
                time - cache.lastSlimeBlockVertical > 2500 && time - cache.lastSlimeBlockHorizontal > 2500 &&
                time - cache.lastHoneyBlockVertical > 2500 && time - cache.lastHoneyBlockHorizontal > 2500 &&
                time - cache.lastWasHit > 350 && time - cache.lastWasDamaged > 150 &&
                time - cache.lastFlight > 750;
    }

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, this, "onAsyncMovement", LACMovementRequirement.POSITION, event -> onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, this, "beforeMovement", LACMovementRequirement.POSITION, event -> beforeMovement((LACAsyncPlayerMoveEvent) event));
        LACEventBus.register(LACEventType.ASYNC_PLAYER_BREAK_BLOCK, LACEventPriority.NORMAL, this, "scaffoldBlockBreak", event -> scaffoldBlockBreak((LACAsyncPlayerBreakBlockEvent) event));
    }

    public void onAsyncMovement(LACAsyncPlayerMoveEvent event) {
        LACPlayer lacPlayer = event.getLacPlayer();
        PlayerCache cache = lacPlayer.cache;
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (System.currentTimeMillis() - buffer.getLong("lastScaffoldBreak") > 5000)
            buffer.put("scaffoldBreaks", 0);

        if (!isCheckAllowed(player, lacPlayer, true)) {
            buffer.put("fallEvents", 0);
            buffer.put("fallStartLocation", null);
            return;
        }

        if (!isConditionAllowed(player, lacPlayer, event)) {
            buffer.put("fallEvents", 0);
            buffer.put("fallStartLocation", null);
            return;
        }

        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            buffer.put("fallEvents", 0);
            buffer.put("fallStartLocation", null);
            return;
        }

        if (!event.isToWithinBlocksPassable() || !event.isFromWithinBlocksPassable()) {
            buffer.put("fallEvents", 0);
            buffer.put("fallStartLocation", null);
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lacPlayer.joinTime < 5000) {
            buffer.put("fallEvents", 0);
            buffer.put("fallStartLocation", null);
            return;
        }

        if (currentTime - cache.lastEntityNearby <= 3000) {
            buffer.put("fallEvents", 0);
            buffer.put("fallStartLocation", null);
            return;
        }

        if (currentTime - buffer.getLong("effectTime") < 1000) {
            buffer.put("fallEvents", 0);
            buffer.put("fallStartLocation", null);
            return;
        }

        Set<Block> accurateDownBlocks = distanceAbsVertical(event.getFrom(), event.getTo()) > distanceHorizontal(event.getFrom(), event.getTo()) * 2.0 ?
                getDownBlocks(player, event.getTo(), Double.MIN_VALUE * 100) : event.getToDownBlocks();
        for (Block block : accurateDownBlocks) {
            if (!isActuallyPassable(block)) {
                buffer.put("fallEvents", 0);
                buffer.put("fallStartLocation", null);
                return;
            }
        }

        if (event.getFrom().getBlockY() > event.getTo().getBlockY() ||
                event.getFrom().getY() > event.getTo().getY() && getBlockY(event.getTo().getY()) == 0) {
            for (Block block : accurateDownBlocks) {
                if (!isActuallyPassable(block.getRelative(BlockFace.DOWN))) {
                    buffer.put("fallEvents", 0);
                    buffer.put("fallStartLocation", null);
                    return;
                }
            }
        }

        for (int i = 0; i < 3 && i < HistoryElement.count(); i++) {
            final HistoryElement element = HistoryElement.at(i);
            if (cache.history.onEvent.onGround.get(element).towardsTrue ||
                    cache.history.onPacket.onGround.get(element).towardsTrue) {
                buffer.put("fallEvents", 0);
                buffer.put("fallStartLocation", null);
                return;
            }
        }

        Location newerLocation = event.getTo();
        for (int i = 0; i < 3 && i < HistoryElement.count(); i++) {
            Location location = cache.history.onEvent.location.get(HistoryElement.at(i));
            double vSpeed = distanceVertical(location, newerLocation);
            newerLocation = location;
            if (vSpeed > 0) {
                buffer.put("fallEvents", 0);
                buffer.put("fallStartLocation", null);
                return;
            }
        }

        buffer.put("fallEvents", buffer.getInt("fallEvents") + 1);

        if (buffer.getLocation("fallStartLocation") == null) {
            buffer.put("fallStartLocation", event.getFrom());
            return;
        }

        if (buffer.getInt("fallEvents") <= 1)
            return;

        int fallEvents = buffer.getInt("fallEvents");
        double fallDistance = distanceVertical(buffer.getLocation("fallStartLocation"), event.getFrom());
        float playerFallDistance = player.getFallDistance();

        int jumpEffectAmplifier = getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("JUMP"));
        float calculatedFallDistanceByEvents = NoFallPredictionProfile.byEvents(jumpEffectAmplifier, fallEvents);
        if (calculatedFallDistanceByEvents == -1F)
            return;

        float calculatedFallDistanceByDistance = NoFallPredictionProfile.byDistance(jumpEffectAmplifier, fallDistance);
        if (calculatedFallDistanceByDistance == -1F)
            return;
        float calculatedFallDistance = Math.min(calculatedFallDistanceByEvents, calculatedFallDistanceByDistance);

        if (distanceHorizontal(event.getFrom(), event.getTo()) * 2.0 > distanceAbsVertical(event.getFrom(), event.getTo())) {
            playerFallDistance += 0.3;
            if (NoFallPredictionProfile.isHorizontalFallDistanceExemption(calculatedFallDistance - playerFallDistance * 1.1 + 0.15))
                return;
        }
        if (distanceHorizontal(event.getFrom(), event.getTo()) > distanceAbsVertical(event.getFrom(), event.getTo()))
            playerFallDistance += 0.7;
        if (buffer.getInt("scaffoldBreaks") != 0)
            playerFallDistance += buffer.getInt("scaffoldBreaks") * 1.1;
        if (cache.sneakingTicks <= 25)
            playerFallDistance += 0.5;
        if (System.currentTimeMillis() - buffer.getLong("interactiveBlockTime") < 250)
            playerFallDistance += 0.75;
        playerFallDistance = (float) (playerFallDistance * 1.2 + 0.35);
        if (playerFallDistance > calculatedFallDistance)
            return;

        Set<Player> players = getPlayersForEnchantsSquared(lacPlayer, player);
        updateDownBlocks(player, lacPlayer, event.getToDownBlocks());
        float finalPlayerFallDistance = playerFallDistance;
        Scheduler.runTask(true, () -> {
            if (EnchantsSquaredHook.hasEnchantment(player, "Burden") &&
                    finalPlayerFallDistance * 1.2 + 1.0 > calculatedFallDistance)
                return;

            if (isEnchantsSquaredImpact(players) && finalPlayerFallDistance * 1.5 + 1.2 > calculatedFallDistance)
                return;

            callViolationEventIfRepeat(player, lacPlayer, event, buffer, 1500);
        });
    }

    public void beforeMovement(LACAsyncPlayerMoveEvent event) {
        LACPlayer lacPlayer = event.getLacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        if (getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("LEVITATION")) > 0 ||
                getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("SLOW_FALLING")) > 0 ||
                getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("JUMP")) > 5) {
            Buffer buffer = getBuffer(player, true);
            buffer.put("effectTime", System.currentTimeMillis());
        }

        for (Block block : event.getToInteractiveBlocks()) {
            if (isActuallyPassable(block) && isActuallyPassable(block.getRelative(BlockFace.DOWN)))
                continue;
            Buffer buffer = getBuffer(player, true);
            buffer.put("interactiveBlockTime", System.currentTimeMillis());
        }
    }

    public void scaffoldBlockBreak(LACAsyncPlayerBreakBlockEvent event) {
        if (isActuallyPassable(event.getBlock()))
            return;
        Block placedBlock = event.getBlock();
        boolean within = false;
        for (Block block : getWithinBlocks(event.getPlayer())) {
            if (!equals(placedBlock, block) &&
                    !equals(placedBlock, block.getRelative(BlockFace.DOWN)) &&
                    !equals(placedBlock, block.getRelative(BlockFace.UP)))
                continue;
            within = true;
            break;
        }
        if (!within)
            return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("lastScaffoldBreak", System.currentTimeMillis());
        buffer.put("scaffoldBreaks", buffer.getInt("scaffoldBreaks") + 1);
    }

    private static boolean equals(Block block1, Block block2) {
        return block1.getX() == block2.getX() &&
                block1.getY() == block2.getY() &&
                block1.getZ() == block2.getZ();
    }

}
