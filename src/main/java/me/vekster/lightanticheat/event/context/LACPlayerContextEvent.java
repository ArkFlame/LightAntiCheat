package me.vekster.lightanticheat.event.context;

import me.vekster.lightanticheat.player.LACPlayer;

public interface LACPlayerContextEvent {
    LACPlayer.Context getContext();

    default boolean canDispatch() {
        return getContext().isCurrent();
    }
}
