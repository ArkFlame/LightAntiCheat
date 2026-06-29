package me.vekster.lightanticheat.event.bus;

import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.event.playermove.LACMovementChange;
import me.vekster.lightanticheat.event.playermove.LACPlayerMoveEvent;

public enum LACMovementRequirement {

    ANY,
    POSITION,
    ROTATION,
    POSITION_AND_ROTATION;

    public boolean accepts(final Object event) {
        if (this == ANY) {
            return true;
        }

        final LACMovementChange change = movementChange(event);
        if (change == null) {
            return true;
        }

        if (this == POSITION) {
            return change.isPositionChanged();
        }
        if (this == ROTATION) {
            return change.isRotationChanged();
        }
        if (this == POSITION_AND_ROTATION) {
            return change.isPositionChanged() && change.isRotationChanged();
        }
        return true;
    }

    private static LACMovementChange movementChange(final Object event) {
        if (event instanceof LACAsyncPlayerMoveEvent) {
            return ((LACAsyncPlayerMoveEvent) event).getMovementChange();
        }
        if (event instanceof LACPlayerMoveEvent) {
            return ((LACPlayerMoveEvent) event).getMovementChange();
        }
        return null;
    }
}
