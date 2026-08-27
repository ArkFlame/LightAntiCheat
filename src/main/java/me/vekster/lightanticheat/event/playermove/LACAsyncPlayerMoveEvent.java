package me.vekster.lightanticheat.event.playermove;

import me.vekster.lightanticheat.Main;
import me.vekster.lightanticheat.event.context.LACPlayerContextEvent;
import me.vekster.lightanticheat.event.playermove.blockcache.BlockCache;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.LACPlayerManager;
import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class LACAsyncPlayerMoveEvent extends Event implements Cancellable, LACPlayerContextEvent {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    private final AtomicBoolean setbackScheduled = new AtomicBoolean();
    private final LACPlayer.Context context;
    private final Location to;
    private final Location from;
    private final LACMovementChange movementChange;
    private final boolean isPlayerClimbing;
    private final boolean isPlayerInWater;
    private final boolean isPlayerFlying;
    private final boolean isPlayerInsideVehicle;
    private final boolean isPlayerGliding;
    private final boolean isPlayerRiptiding;
    private final BlockCache fromBlockCache;
    private final BlockCache toBlockCache;

    public LACAsyncPlayerMoveEvent(LACPlayer.Context context,
                                     Location from,
                                     Location to,
                                     LACMovementChange movementChange,
                                     boolean isPlayerFlying,
                                     boolean isPlayerInsideVehicle,
                                     boolean isPlayerGliding,
                                     boolean isPlayerRiptiding,
                                     BlockCache fromBlockCache,
                                     BlockCache toBlockCache) {
        super(!FoliaUtil.isFolia());
        this.context = context;
        this.from = from != null ? from.clone() : null;
        this.to = to != null ? to.clone() : null;
        this.movementChange = movementChange != null ? movementChange : LACMovementChange.of(from, to);
        this.fromBlockCache = fromBlockCache != null ? fromBlockCache : BlockCache.empty();
        this.toBlockCache = toBlockCache != null ? toBlockCache : BlockCache.empty();
        this.isPlayerFlying = isPlayerFlying;
        this.isPlayerInsideVehicle = isPlayerInsideVehicle;
        this.isPlayerGliding = isPlayerGliding;
        this.isPlayerRiptiding = isPlayerRiptiding;
        if (toBlockCache != null && toBlockCache.isReadable()) {
            this.isPlayerClimbing = toBlockCache.playerClimbing;
            this.isPlayerInWater = toBlockCache.playerInWater;
        } else {
            this.isPlayerClimbing = false;
            this.isPlayerInWater = false;
        }
    }

    public static Optional<LACAsyncPlayerMoveEvent> createPacketMode(LACPlayer.Context context,
                                                                     Location from,
                                                                     Location to) {
        if (context == null || !context.isCurrent()) return Optional.empty();
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null) return Optional.empty();
        if (!from.getWorld().getUID().equals(context.worldId()) || !to.getWorld().getUID().equals(context.worldId())) return Optional.empty();
        if (!from.getWorld().getUID().equals(to.getWorld().getUID())) return Optional.empty();
        LACMovementChange change = LACMovementChange.of(from, to);
        boolean sameWorld = sameWorld(from, to);
        BlockCache existingFromCache = context.cache().fromBlockCache;
        BlockCache fromCache = (sameWorld && existingFromCache != null && existingFromCache.matches(from))
                ? existingFromCache : BlockCache.empty();
        boolean canReadTo = sameWorld
                && change.isPositionChanged()
                && context.isCurrent()
                && (!FoliaUtil.isFolia()
                    || (FoliaUtil.isOwnedByCurrentRegion(context.player())
                        && FoliaUtil.isOwnedByCurrentRegion(to, 1)));
        BlockCache toCache;
        boolean climbing;
        boolean inWater;
        if (canReadTo) {
            toCache = BlockCache.capture(context, to);
            climbing = toCache.playerClimbing;
            inWater = toCache.playerInWater;
        } else {
            toCache = fromCache;
            climbing = false;
            inWater = false;
        }
        if (sameWorld && toCache.isReadable()) {
            context.cache().fromBlockCache = toCache;
        } else if (!sameWorld) {
            context.cache().fromBlockCache = BlockCache.empty();
        }
        boolean isFlying = context.player().isFlying();
        boolean isInsideVehicle = context.player().isInsideVehicle();
        boolean isGliding = context.owner().isGliding();
        boolean isRiptiding = context.owner().isRiptiding();
        LACAsyncPlayerMoveEvent evt = new LACAsyncPlayerMoveEvent(context, from, to, change,
                isFlying, isInsideVehicle, isGliding, isRiptiding, fromCache, toCache);
        return Optional.of(evt);
    }

    public LACAsyncPlayerMoveEvent(LACPlayerMoveEvent event) {
        super(!FoliaUtil.isFolia());

        final LACPlayer lacPlayer = event.getLacPlayer();
        final org.bukkit.entity.Player player = event.getPlayer();
        final Optional<LACPlayer.Context> maybeContext = lacPlayer.capture(player);
        this.context = maybeContext.orElse(null);

        final Location eventFrom = event.getFrom();
        final Location eventTo = event.getTo();
        this.from = eventFrom.clone();
        this.to = eventTo.clone();
        this.movementChange = event.getMovementChange();
        this.isPlayerFlying = event.isPlayerFlying();
        this.isPlayerInsideVehicle = event.isPlayerInsideVehicle();
        this.isPlayerGliding = event.isPlayerGliding();
        this.isPlayerRiptiding = event.isPlayerRiptiding();

        boolean sameWorld = sameWorld(eventFrom, eventTo);
        final BlockCache existingFromCache = this.context != null ? this.context.cache().fromBlockCache : null;
        this.fromBlockCache = (sameWorld && existingFromCache != null && existingFromCache.matches(eventFrom))
                ? existingFromCache
                : BlockCache.empty();

        boolean canReadToBlocks = sameWorld
                && movementChange.isPositionChanged()
                && this.context != null
                && this.context.isCurrent()
                && (!FoliaUtil.isFolia()
                    || (FoliaUtil.isOwnedByCurrentRegion(this.context.player())
                        && FoliaUtil.isOwnedByCurrentRegion(eventTo, 1)));
        if (canReadToBlocks) {
            this.toBlockCache = BlockCache.capture(this.context, to);
            isPlayerClimbing = toBlockCache.playerClimbing;
            isPlayerInWater = toBlockCache.playerInWater;
        } else {
            this.toBlockCache = this.fromBlockCache;
            isPlayerClimbing = false;
            isPlayerInWater = false;
        }

        if (sameWorld && toBlockCache.isReadable() && this.context != null) {
            this.context.cache().fromBlockCache = this.toBlockCache;
        } else if (!sameWorld && this.context != null) {
            this.context.cache().fromBlockCache = BlockCache.empty();
        }
    }

    private static boolean sameWorld(final Location first, final Location second) {
        return first != null
                && second != null
                && first.getWorld() != null
                && second.getWorld() != null
                && first.getWorld().getUID().equals(second.getWorld().getUID());
    }

    @Override
    public LACPlayer.Context getContext() {
        return context;
    }

    public org.bukkit.entity.Player getPlayer() {
        return context != null ? context.player() : null;
    }

    public LACPlayer getLacPlayer() {
        return context != null ? context.owner() : null;
    }

    @Override
    public boolean canDispatch() {
        if (context == null || !context.isCurrent()) {
            return false;
        }
        if (!movementChange.isPositionChanged()) {
            return true;
        }
        return toBlockCache.isReadable();
    }

    public Location getFrom() {
        return from.clone();
    }

    public Location getTo() {
        return to.clone();
    }

    public LACMovementChange getMovementChange() {
        return movementChange;
    }

    public boolean hasPositionChanged() {
        return movementChange.isPositionChanged();
    }

    public boolean hasHorizontalChanged() {
        return movementChange.isHorizontalChanged();
    }

    public boolean hasVerticalChanged() {
        return movementChange.isVerticalChanged();
    }

    public boolean hasRotationChanged() {
        return movementChange.isRotationChanged();
    }

    public boolean isPlayerClimbing() {
        return isPlayerClimbing;
    }

    public boolean isPlayerInWater() {
        return isPlayerInWater;
    }

    public boolean isPlayerFlying() {
        return isPlayerFlying;
    }

    public boolean isPlayerInsideVehicle() {
        return isPlayerInsideVehicle;
    }

    public boolean isPlayerGliding() {
        return isPlayerGliding;
    }

    public boolean isPlayerRiptiding() {
        return isPlayerRiptiding;
    }

    public Set<Block> getFromWithinBlocks() {
        return fromBlockCache.withinBlocks;
    }

    public Set<Material> getFromWithinMaterials() {
        return fromBlockCache.withinMaterials;
    }

    public boolean isFromWithinBlocksPassable() {
        return fromBlockCache.withinBlocksPassable;
    }

    public Set<Block> getFromDownBlocks() {
        return fromBlockCache.downBlocks;
    }

    public Set<Material> getFromDownMaterials() {
        return fromBlockCache.downMaterials;
    }

    public boolean isFromDownBlocksPassable() {
        return fromBlockCache.downBlocksPassable;
    }

    public Set<Block> getToWithinBlocks() {
        return toBlockCache.withinBlocks;
    }

    public Set<Material> getToWithinMaterials() {
        return toBlockCache.withinMaterials;
    }

    public boolean isToWithinBlocksPassable() {
        return toBlockCache.withinBlocksPassable;
    }

    public Set<Block> getToDownBlocks() {
        return toBlockCache.downBlocks;
    }

    public Set<Material> getToDownMaterials() {
        return toBlockCache.downMaterials;
    }

    public boolean isToDownBlocksPassable() {
        return toBlockCache.downBlocksPassable;
    }

    public Set<Block> getFromInteractiveBlocks() {
        return fromBlockCache.interactiveBlocks;
    }

    public Set<Material> getFromInteractiveMaterials() {
        return fromBlockCache.interactiveMaterials;
    }

    public Set<Block> getToInteractiveBlocks() {
        return toBlockCache.interactiveBlocks;
    }

    public Set<Material> getToInteractiveMaterials() {
        return toBlockCache.interactiveMaterials;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
        if (cancel && setbackScheduled.compareAndSet(false, true)) {
            LACPlayerManager.execute(context, true, ctx -> {
                if (!cancelled || !ctx.isCurrent()) return;
                FoliaUtil.teleportPlayerAsync(ctx.player(), getFrom()).whenComplete((success, throwable) -> {
                    if (throwable != null) {
                        Main.getInstance().getLogger().warning(
                                "LAC setback teleport failed for " + ctx.player().getName() + ": " + throwable.getMessage());
                    }
                });
            });
        }
    }
}
