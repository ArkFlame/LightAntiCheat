package me.vekster.lightanticheat.event.playermove;

import me.vekster.lightanticheat.player.LACPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;

public class LACPlayerMoveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final PlayerMoveEvent event;
    private final Player player;
    private final LACPlayer lacPlayer;
    private final Location to;
    private final Location from;
    private final LACMovementChange movementChange;
    private final boolean isPlayerFlying;
    private final boolean isPlayerInsideVehicle;
    private final boolean isPlayerGliding;
    private final boolean isPlayerRiptiding;

    public LACPlayerMoveEvent(PlayerMoveEvent event, Player player,
                              LACPlayer lacPlayer, Location from, Location to) {
        this.event = event;
        this.player = player;
        this.lacPlayer = lacPlayer;
        this.from = from.clone();
        this.to = to.clone();
        this.movementChange = LACMovementChange.of(from, to);
        this.isPlayerFlying = player.isFlying();
        this.isPlayerInsideVehicle = player.isInsideVehicle();
        this.isPlayerGliding = lacPlayer.isGliding();
        this.isPlayerRiptiding = lacPlayer.isRiptiding();
    }

    public PlayerMoveEvent getEvent() {
        return event;
    }

    public Player getPlayer() {
        return player;
    }

    public LACPlayer getLacPlayer() {
        return lacPlayer;
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
