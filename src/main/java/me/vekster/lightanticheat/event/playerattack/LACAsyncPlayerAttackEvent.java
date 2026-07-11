package me.vekster.lightanticheat.event.playerattack;

import me.vekster.lightanticheat.event.context.LACPlayerContextEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LACAsyncPlayerAttackEvent extends Event implements LACPlayerContextEvent {
    private static final HandlerList handlers = new HandlerList();
    private final LACPlayer.Context context;
    private final int entityId;

    public LACAsyncPlayerAttackEvent(LACPlayer.Context context, int entityId) {
        super(!FoliaUtil.isFolia());

        this.context = context;
        this.entityId = entityId;
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

    public int getEntityId() {
        return entityId;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
