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

    public boolean acceptsMask(final int movementMask) {
        if (this == ANY) {
            return true;
        }
        final boolean position = (movementMask & 1) != 0;
        final boolean rotation = (movementMask & 2) != 0;
        if (this == POSITION) {
            return position;
        }
        if (this == ROTATION) {
            return rotation;
        }
        if (this == POSITION_AND_ROTATION) {
            return position && rotation;
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
