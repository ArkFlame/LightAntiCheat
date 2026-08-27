package me.vekster.lightanticheat.input.provider;

import me.vekster.lightanticheat.input.model.LACInputMode;

public interface LACInputProvider extends AutoCloseable {

    /**
     * Returns the mode this provider handles.
     */
    LACInputMode getMode();

    /**
     * Starts the provider. Idempotent: repeated calls after started have no effect.
     */
    void start();

    /**
     * Returns true if start() has been called and the provider has not been closed.
     */
    boolean isStarted();

    /**
     * Closes the provider. Idempotent: repeated calls have no effect.
     */
    @Override
    void close();
}
