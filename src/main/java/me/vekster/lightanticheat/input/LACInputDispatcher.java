package me.vekster.lightanticheat.input;

import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.packetreceive.LACAsyncPacketReceiveEvent;
import me.vekster.lightanticheat.event.playerattack.LACAsyncPlayerAttackEvent;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.event.playermove.LACMovementChange;
import me.vekster.lightanticheat.event.playermove.blockcache.BlockCache;
import me.vekster.lightanticheat.input.model.LACInputMode;
import me.vekster.lightanticheat.input.model.LACLocation;
import me.vekster.lightanticheat.input.model.LACMovementFrame;
import me.vekster.lightanticheat.input.model.LACPacketFrame;
import me.vekster.lightanticheat.input.model.LACPacketType;
import me.vekster.lightanticheat.input.model.LACPlayerSession;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.LACPlayerManager;
import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import me.vekster.lightanticheat.version.identifier.LACVersion;
import me.vekster.lightanticheat.version.identifier.VerIdentifier;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

public final class LACInputDispatcher {

    private final LACInputEngine engine;

    public LACInputDispatcher(LACInputEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("engine must not be null");
        }
        this.engine = engine;
    }

    public void dispatch(LACPlayerInputQueue.QueuedItem item) {
        if (item == null) {
            return;
        }
        LACPacketFrame frame = item.getPacketFrame();
        Optional<LACMovementFrame> movementOpt = item.getMovementFrame();
        if (frame == null) {
            return;
        }
        LACPlayerSession session = frame.getSession();
        if (session == null) {
            return;
        }
        LACPlayerManager.execute(session, true, context -> {
            if (context == null || !context.isCurrent()) {
                return;
            }
            if (!context.owner().matchesSession(session)) {
                return;
            }
            if (context.worldId() == null || !context.worldId().equals(session.getWorldId())) {
                return;
            }

            LACPacketType packetType = frame.getPacketType();
            int entityId = frame.getEntityId();

            Optional<Location> locationOpt = Optional.empty();
            BlockCache blockCache = BlockCache.empty();
            if (packetType == LACPacketType.FLYING) {
                Optional<LACLocation> lacLocOpt = frame.getMovementLocation();
                if (lacLocOpt.isPresent()) {
                    LACLocation lacLoc = lacLocOpt.get();
                    if (lacLoc != null && lacLoc.getWorldId() != null && lacLoc.getWorldId().equals(context.worldId())) {
                        World world = context.player().getWorld();
                        if (world != null && world.getUID().equals(lacLoc.getWorldId())) {
                            Location bukkitLoc = new Location(world, lacLoc.getX(), lacLoc.getY(), lacLoc.getZ(), lacLoc.getYaw(), lacLoc.getPitch());
                            locationOpt = Optional.of(bukkitLoc);
                            blockCache = BlockCache.capture(context, bukkitLoc);
                        }
                    }
                } else if (movementOpt.isPresent()) {
                    // fallback to movement frame to if available
                    LACMovementFrame mf = movementOpt.get();
                    LACLocation lacLoc = mf.getTo();
                    if (lacLoc != null && lacLoc.getWorldId() != null && lacLoc.getWorldId().equals(context.worldId())) {
                        World world = context.player().getWorld();
                        if (world != null && world.getUID().equals(lacLoc.getWorldId())) {
                            Location bukkitLoc = new Location(world, lacLoc.getX(), lacLoc.getY(), lacLoc.getZ(), lacLoc.getYaw(), lacLoc.getPitch());
                            locationOpt = Optional.of(bukkitLoc);
                            blockCache = BlockCache.capture(context, bukkitLoc);
                        }
                    }
                }
            }

            LACAsyncPacketReceiveEvent packetEvent = new LACAsyncPacketReceiveEvent(context, frame, locationOpt, blockCache);

            if (packetType == LACPacketType.USE_ENTITY
                    && VerIdentifier.getVersion().isNewerThan(LACVersion.V1_8)) {
                LACEventBus.call(LACEventType.ASYNC_PLAYER_ATTACK,
                        new LACAsyncPlayerAttackEvent(context, entityId));
            }
            LACEventBus.call(LACEventType.ASYNC_PACKET_RECEIVE, packetEvent);

            if (movementOpt.isPresent() && engine.getActiveMode() == LACInputMode.PACKET) {
                LACMovementFrame movement = movementOpt.get();
                if (movement == null || !movement.getSession().equals(session)) {
                    return;
                }
                LACLocation fromLac = movement.getFrom();
                LACLocation toLac = movement.getTo();
                if (fromLac == null || toLac == null) {
                    return;
                }
                if (fromLac.getWorldId() == null || toLac.getWorldId() == null) {
                    return;
                }
                if (!fromLac.getWorldId().equals(context.worldId()) || !toLac.getWorldId().equals(context.worldId())) {
                    return;
                }
                World world = context.player().getWorld();
                if (world == null || !world.getUID().equals(context.worldId())) {
                    return;
                }
                Location from = new Location(world, fromLac.getX(), fromLac.getY(), fromLac.getZ(), fromLac.getYaw(), fromLac.getPitch());
                Location to = new Location(world, toLac.getX(), toLac.getY(), toLac.getZ(), toLac.getYaw(), toLac.getPitch());

                LACMovementChange change = LACMovementChange.of(fromLac, toLac);

                boolean sameWorld = from.getWorld() != null && to.getWorld() != null
                        && from.getWorld().getUID().equals(to.getWorld().getUID());

                BlockCache fromCache = context.cache().fromBlockCache;
                if (fromCache == null || !fromCache.isReadable() || !fromCache.matches(from)) {
                    fromCache = BlockCache.empty();
                }

                BlockCache toCache;
                boolean canReadTo = sameWorld
                        && change.isPositionChanged()
                        && context.isCurrent()
                        && (!FoliaUtil.isFolia()
                            || (FoliaUtil.isOwnedByCurrentRegion(context.player())
                                && FoliaUtil.isOwnedByCurrentRegion(to, 1)));
                if (canReadTo) {
                    toCache = BlockCache.capture(context, to);
                } else if (!change.isPositionChanged()) {
                    toCache = fromCache;
                } else {
                    toCache = BlockCache.empty();
                }

                boolean isFlying = context.player().isFlying();
                boolean isInsideVehicle = context.player().isInsideVehicle();
                boolean isGliding = context.owner().isGliding();
                boolean isRiptiding = context.owner().isRiptiding();

                LACAsyncPlayerMoveEvent moveEvent = new LACAsyncPlayerMoveEvent(
                        context, from, to, change,
                        isFlying, isInsideVehicle, isGliding, isRiptiding,
                        fromCache, toCache);

                if (sameWorld && toCache.isReadable()) {
                    context.cache().fromBlockCache = toCache;
                } else if (!sameWorld) {
                    context.cache().fromBlockCache = BlockCache.empty();
                }

                LACEventBus.call(LACEventType.ASYNC_PLAYER_MOVE, moveEvent);
            }
        });
    }
}
