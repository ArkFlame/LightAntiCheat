package me.vekster.lightanticheat.util.command;

import me.vekster.lightanticheat.util.logger.LogType;
import me.vekster.lightanticheat.util.logger.Logger;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandException;

import java.util.ArrayList;
import java.util.List;

public final class RuntimeCommandDispatcher {

    private RuntimeCommandDispatcher() {
    }

    public static void dispatchConsoleBatch(final List<String> commands, final String playerName, final String checkTitle) {
        if (commands == null) {
            throw new IllegalArgumentException("commands must not be null");
        }
        final List<String> copy = new ArrayList<>(commands);
        if (copy.isEmpty()) {
            return;
        }
        final String diagnosticPlayer = playerName == null ? "unknown" : playerName;
        final String diagnosticCheck = checkTitle == null ? "unknown" : checkTitle;
        Scheduler.globalThread(() -> {
            for (String command : copy) {
                if (command == null || command.trim().isEmpty()) {
                    Logger.logConsole(LogType.ERROR, "(LightAntiCheat-Plus) Skipped empty punishment command for " + diagnosticPlayer + " (" + diagnosticCheck + ")");
                    continue;
                }
                try {
                    boolean handled = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    if (!handled) {
                        Logger.logConsole(LogType.ERROR, "(LightAntiCheat-Plus) Punishment command was not handled: '" + command + "' for " + diagnosticPlayer + " (" + diagnosticCheck + ").");
                        continue;
                    }
                } catch (CommandException e) {
                    Logger.logConsole(LogType.ERROR, "(LightAntiCheat-Plus) Failed to execute punishment command '" + command + "' for " + diagnosticPlayer + " (" + diagnosticCheck + "): " + e.getMessage());
                }
            }
        });
    }
}
