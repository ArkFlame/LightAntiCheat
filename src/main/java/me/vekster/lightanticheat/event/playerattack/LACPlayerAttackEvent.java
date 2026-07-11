package me.vekster.lightanticheat.event.playerattack;

import me.vekster.lightanticheat.event.context.LACPlayerContextEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.detection.CheckUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class LACPlayerAttackEvent extends Event implements LACPlayerContextEvent {
    private static final HandlerList handlers = new HandlerList();
    private EntityDamageByEntityEvent event;
    private final LACPlayer.Context context;
    private Entity entity;
    private boolean isEntityAttackCause;

    public LACPlayerAttackEvent(EntityDamageByEntityEvent event, LACPlayer.Context context, Entity entity) {
        this.event = event;
        this.context = context;
        this.entity = entity;
        this.isEntityAttackCause = event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK;
    }

    public EntityDamageByEntityEvent getEvent() {
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

    public Entity getEntity() {
        return entity;
    }

    public boolean isEntityAttackCause() {
        return isEntityAttackCause;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
