package me.vekster.lightanticheat.event.playermove;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class LACMovementChange {

    private final boolean positionChanged;
    private final boolean horizontalChanged;
    private final boolean verticalChanged;
    private final boolean rotationChanged;
    private final boolean yawChanged;
    private final boolean pitchChanged;

    public static LACMovementChange of(final Location from, final Location to) {
        if (from == null || to == null) {
            return new LACMovementChange(false, false, false, false, false, false);
        }

        final boolean xChanged = Double.compare(from.getX(), to.getX()) != 0;
        final boolean yChanged = Double.compare(from.getY(), to.getY()) != 0;
        final boolean zChanged = Double.compare(from.getZ(), to.getZ()) != 0;
        final boolean yawChanged = Float.compare(from.getYaw(), to.getYaw()) != 0;
        final boolean pitchChanged = Float.compare(from.getPitch(), to.getPitch()) != 0;

        final boolean horizontalChanged = xChanged || zChanged;
        final boolean verticalChanged = yChanged;
        final boolean positionChanged = horizontalChanged || verticalChanged;
        final boolean rotationChanged = yawChanged || pitchChanged;

        return new LACMovementChange(
                positionChanged,
                horizontalChanged,
                verticalChanged,
                rotationChanged,
                yawChanged,
                pitchChanged
        );
    }
}
