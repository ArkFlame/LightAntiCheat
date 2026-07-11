package me.vekster.lightanticheat.event.playermove;

import me.vekster.lightanticheat.event.context.LACPlayerContextEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;

public class LACPlayerMoveEvent extends Event implements LACPlayerContextEvent {
    private static final HandlerList handlers = new HandlerList();
    private final PlayerMoveEvent event;
    private final LACPlayer.Context context;
    private final Location to;
    private final Location from;
    private final LACMovementChange movementChange;
    private final boolean isPlayerFlying;
    private final boolean isPlayerInsideVehicle;
    private final boolean isPlayerGliding;
    private final boolean isPlayerRiptiding;

    public LACPlayerMoveEvent(PlayerMoveEvent event, LACPlayer.Context context,
                              Location from, Location to) {
        this.event = event;
        this.context = context;
        this.from = from.clone();
        this.to = to.clone();
        this.movementChange = LACMovementChange.of(from, to);
        this.isPlayerFlying = context.player().isFlying();
        this.isPlayerInsideVehicle = context.player().isInsideVehicle();
        this.isPlayerGliding = context.owner().isGliding();
        this.isPlayerRiptiding = context.owner().isRiptiding();
    }

    public PlayerMoveEvent getEvent() {
        return event;
    }

    public Player getPlayer() {
        return context.player();
    }

    public LACPlayer getLacPlayer() {
        return context.owner();
    }

    public LACPlayer.Context getContext() {
        return context;
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

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
