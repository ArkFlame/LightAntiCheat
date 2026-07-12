package me.vekster.lightanticheat.check.checks.movement.elytra;

import me.vekster.lightanticheat.Main;
import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.movement.MovementCheck;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.cache.PlayerCache;
import me.vekster.lightanticheat.player.cache.history.HistoryElement;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import me.vekster.lightanticheat.version.VerUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Fast takeoff without a firework
 */
public class ElytraC extends MovementCheck implements Listener {
    public ElytraC() {
        super(CheckName.ELYTRA_C);
    }

    private static final double[] TICK_SPEEDS = new double[41];
    private static final double[] EVENT_SPEEDS = new double[39];

    private static double tickSpeedLimit(int tick) {
        if (tick < 0 || tick >= TICK_SPEEDS.length) return Double.MAX_VALUE;
        double v = TICK_SPEEDS[tick];
        return Double.isNaN(v) ? Double.MAX_VALUE : v;
    }

    private static double eventSpeedLimit(int event) {
        if (event < 0 || event >= EVENT_SPEEDS.length) return Double.MAX_VALUE;
        double v = EVENT_SPEEDS[event];
        return Double.isNaN(v) ? Double.MAX_VALUE : v;
    }

    @Override
    public boolean isConditionAllowed(Player player, LACPlayer lacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing || !isGliding || isRiptiding || isInWater)
            return false;
        if (cache.flyingTicks >= -5 || cache.climbingTicks >= -2 || cache.glidingTicks <= 3)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastInsideVehicle > 150 && time - cache.lastInWater > 150 &&
                time - cache.lastKnockback > 750 && time - cache.lastKnockbackNotVanilla > 3000 &&
                time - cache.lastWasFished > 4000 && time - cache.lastTeleport > 500 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 700 &&
                time - cache.lastBlockExplosion > 8000 && time - cache.lastEntityExplosion > 3000 &&
                time - cache.lastSlimeBlockVertical > 6000 && time - cache.lastSlimeBlockHorizontal > 6000 &&
                time - cache.lastHoneyBlockVertical > 2500 && time - cache.lastHoneyBlockHorizontal > 2500 &&
                time - cache.lastFireworkBoost > 6000 && time - cache.lastFireworkBoostNotVanilla > 8000 &&
                time - cache.lastRiptiding > 15 * 1000 &&
                time - cache.lastWasHit > 350 && time - cache.lastWasDamaged > 150 &&
                time - cache.lastKbVelocity > 500 && time - cache.lastAirKbVelocity > 1000 &&
                time - cache.lastStrongKbVelocity > 2500 && time - cache.lastStrongAirKbVelocity > 5000 &&
                time - cache.lastFlight > 750;
    }

    @EventHandler
    public void onAsyncMovement(LACAsyncPlayerMoveEvent event) {
        LACPlayer lacPlayer = event.getLacPlayer();
        PlayerCache cache = lacPlayer.cache;
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, lacPlayer, true)) {
            buffer.put("glidingEvents", 0);
            return;
        }

        if (!isConditionAllowed(player, lacPlayer, event)) {
            buffer.put("glidingEvents", 0);
            return;
        }

        if (!event.isToWithinBlocksPassable() || !event.isFromWithinBlocksPassable()) {
            buffer.put("glidingEvents", 0);
            return;
        }
        if (!event.isToDownBlocksPassable() || !event.isFromDownBlocksPassable()) {
            buffer.put("glidingEvents", 0);
            return;
        }

        if (cache.history.onEvent.onGround.get(HistoryElement.FROM).towardsTrue ||
                cache.history.onPacket.onGround.get(HistoryElement.FROM).towardsTrue)
            return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("effectTime") < 1000) {
            buffer.put("glidingEvents", 0);
            return;
        }

        if (event.getFrom().getBlockY() > event.getTo().getBlockY() ||
                event.getFrom().getY() > event.getTo().getY() && event.getTo().getY() % 1.0 == 0) {
            for (Block block : event.getToDownBlocks()) {
                Block downBlock = block.getRelative(BlockFace.DOWN);
                if (!isActuallyPassable(downBlock)) {
                    buffer.put("glidingEvents", 0);
                    return;
                }
            }
        }

        Set<Block> interactiveBlocks = new HashSet<>();
        event.getToInteractiveBlocks().forEach(block -> {
            interactiveBlocks.add(block);
            interactiveBlocks.add(block.getRelative(BlockFace.UP));
        });
        for (Block block : interactiveBlocks)
            if (!isActuallyPassable(block)) {
                buffer.put("glidingEvents", 0);
                return;
            }

        buffer.put("glidingEvents", buffer.getInt("glidingEvents") + 1);
        if (buffer.getInt("glidingEvents") <= 1)
            return;

        double maxTickSpeed = tickSpeedLimit(cache.glidingTicks);
        if (maxTickSpeed == Double.MAX_VALUE) return;
        double maxEventSpeed = eventSpeedLimit(buffer.getInt("glidingEvents"));
        if (maxEventSpeed == Double.MAX_VALUE) return;

        double horizontalSpeed = distanceHorizontal(event.getFrom(), event.getTo());
        double averageHorizontalSpeed = distanceHorizontal(cache.history.onEvent.location.get(HistoryElement.FIRST), event.getTo()) / 2.0;

        if (Math.min(horizontalSpeed, averageHorizontalSpeed) < Math.max(maxTickSpeed, maxEventSpeed) * 1.6 + 0.35)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEventIfRepeat(player, lacPlayer, event, buffer, Main.getBufferDurationMils() - 1000L);
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void beforeMovement(LACAsyncPlayerMoveEvent event) {
        LACPlayer lacPlayer = event.getLacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        if (getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("LEVITATION")) > 0 ||
                getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("SLOW_FALLING")) > 0) {
            Buffer buffer = getBuffer(player, true);
            long currentTime = System.currentTimeMillis();
            buffer.put("effectTime", currentTime);
        }
    }

    static {
        java.util.Arrays.fill(TICK_SPEEDS, Double.NaN);
        java.util.Arrays.fill(EVENT_SPEEDS, Double.NaN);
        TICK_SPEEDS[4] = 0.44734879260319627D;
        TICK_SPEEDS[5] = 0.44734879260319627D;
        TICK_SPEEDS[6] = 0.44734879260319627D;
        TICK_SPEEDS[7] = 0.44734879260319627D;
        TICK_SPEEDS[8] = 0.44734879260319627D;
        TICK_SPEEDS[9] = 0.44734879260319627D;
        TICK_SPEEDS[10] = 0.44734879260319627D;
        TICK_SPEEDS[11] = 0.44734879260319627D;
        TICK_SPEEDS[12] = 0.44734879260319627D;
        TICK_SPEEDS[13] = 0.4678633848470381D;
        TICK_SPEEDS[14] = 0.4678633848470381D;
        TICK_SPEEDS[15] = 0.5096458131240956D;
        TICK_SPEEDS[16] = 0.5096458131240956D;
        TICK_SPEEDS[17] = 0.5096458131240956D;
        TICK_SPEEDS[18] = 0.5096458131240956D;
        TICK_SPEEDS[19] = 0.5096458131240956D;
        TICK_SPEEDS[20] = 0.5096458131240956D;
        TICK_SPEEDS[21] = 0.6363293271295004D;
        TICK_SPEEDS[22] = 0.6363293271295004D;
        TICK_SPEEDS[23] = 0.6363293271295004D;
        TICK_SPEEDS[24] = 0.6363293271295004D;
        TICK_SPEEDS[25] = 0.6363293271295004D;
        TICK_SPEEDS[26] = 0.6363293271295004D;
        TICK_SPEEDS[27] = 0.6363293271295004D;
        TICK_SPEEDS[28] = 0.7761990432739139D;
        TICK_SPEEDS[29] = 0.7761990432739139D;
        TICK_SPEEDS[30] = 0.7761990432739139D;
        TICK_SPEEDS[31] = 0.7761990432739139D;
        TICK_SPEEDS[32] = 0.7812136208841997D;
        TICK_SPEEDS[33] = 0.8276965891878098D;
        TICK_SPEEDS[34] = 0.8736567587535112D;
        TICK_SPEEDS[35] = 0.9183020882937782D;
        TICK_SPEEDS[36] = 0.9999682347314106D;
        TICK_SPEEDS[37] = 1.0135098587188913D;
        TICK_SPEEDS[38] = 1.0364071500995329D;
        TICK_SPEEDS[39] = 1.0696900826352207D;
        TICK_SPEEDS[40] = 1.100002594538592D;
        EVENT_SPEEDS[2] = 0.27182645707616016D;
        EVENT_SPEEDS[3] = 0.27182645707616016D;
        EVENT_SPEEDS[4] = 0.27182645707616016D;
        EVENT_SPEEDS[5] = 0.27182645707616016D;
        EVENT_SPEEDS[6] = 0.27182645707616016D;
        EVENT_SPEEDS[7] = 0.27182645707616016D;
        EVENT_SPEEDS[8] = 0.27182645707616016D;
        EVENT_SPEEDS[9] = 0.27182645707616016D;
        EVENT_SPEEDS[10] = 0.27182645707616016D;
        EVENT_SPEEDS[11] = 0.27182645707616016D;
        EVENT_SPEEDS[12] = 0.28429297909853596D;
        EVENT_SPEEDS[13] = 0.29731067574460923D;
        EVENT_SPEEDS[14] = 0.3110668199157708D;
        EVENT_SPEEDS[15] = 0.3262354959638223D;
        EVENT_SPEEDS[16] = 0.3449602243391874D;
        EVENT_SPEEDS[17] = 0.3662675896438863D;
        EVENT_SPEEDS[18] = 0.3904261112530776D;
        EVENT_SPEEDS[19] = 0.416232566957176D;
        EVENT_SPEEDS[20] = 0.4431727632559902D;
        EVENT_SPEEDS[21] = 0.4716248088280485D;
        EVENT_SPEEDS[22] = 0.5016334162704484D;
        EVENT_SPEEDS[23] = 0.5329268013209907D;
        EVENT_SPEEDS[24] = 0.565231662539986D;
        EVENT_SPEEDS[25] = 0.600932554447271D;
        EVENT_SPEEDS[26] = 0.6414542902207322D;
        EVENT_SPEEDS[27] = 0.6868105839578575D;
        EVENT_SPEEDS[28] = 0.734397683958967D;
        EVENT_SPEEDS[29] = 0.7812136208841997D;
        EVENT_SPEEDS[30] = 0.8276965891878098D;
        EVENT_SPEEDS[31] = 0.8736567587535112D;
        EVENT_SPEEDS[32] = 0.9183020882937782D;
        EVENT_SPEEDS[33] = 0.9604544698562417D;
        EVENT_SPEEDS[34] = 0.9999682347314106D;
        EVENT_SPEEDS[35] = 1.0364071500995329D;
        EVENT_SPEEDS[36] = 1.0696900826352207D;
        EVENT_SPEEDS[37] = 1.100002594538592D;
        EVENT_SPEEDS[38] = 1.1278065520749856D;
    }

}
