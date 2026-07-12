package me.vekster.lightanticheat.player;

import me.vekster.lightanticheat.util.scheduler.Scheduler;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public final class LACPlayerManager {

    private static final ConcurrentMap<UUID, LACPlayer> PLAYERS = new ConcurrentHashMap<>();

    private LACPlayerManager() {
    }

    public static LACPlayer attach(Player player) {
        Objects.requireNonNull(player, "player");
        UUID uuid = player.getUniqueId();
        LACPlayer candidate = new LACPlayer(player);
        LACPlayer existing = PLAYERS.putIfAbsent(uuid, candidate);
        LACPlayer wrapper = existing != null ? existing : candidate;
        wrapper.attach(player);
        return wrapper;
    }

    public static Optional<LACPlayer> find(UUID uuid) {
        return Optional.ofNullable(PLAYERS.get(uuid));
    }

    public static Optional<LACPlayer> find(Player player) {
        return find(player.getUniqueId());
    }

    public static Collection<LACPlayer> values() {
        return Collections.unmodifiableList(new ArrayList<LACPlayer>(PLAYERS.values()));
    }

    public static Optional<LACPlayer.Context> capture(Player player) {
        return find(player).flatMap(lp -> lp.capture(player));
    }

    public static Optional<LACPlayer.Context> current(Player player) {
        return capture(player).filter(LACPlayer.Context::isCurrent);
    }

    public static void beginTransition(Player player) {
        find(player).ifPresent(lp -> lp.beginTransition(player));
    }

    public static void completeTransition(Player player) {
        find(player).ifPresent(lp -> lp.completeTransition(player));
    }

    public static void detach(Player player) {
        find(player).ifPresent(lp -> lp.detach(player));
    }

    public static void remove(UUID uuid) {
        PLAYERS.remove(uuid);
    }

    public static void execute(Player player, boolean force, Consumer<LACPlayer.Context> action) {
        Optional<LACPlayer.Context> ctx = capture(player);
        if (!ctx.isPresent()) return;
        LACPlayer.Context context = ctx.get();
        Scheduler.entityThread(player, force, () -> {
            if (!context.isCurrent()) return;
            action.accept(context);
        });
    }

    public static void execute(LACPlayer.Context context, boolean force, Consumer<LACPlayer.Context> action) {
        Scheduler.entityThread(context.player(), force, () -> {
            if (!context.isCurrent()) return;
            action.accept(context);
        });
    }

    public static void executeLater(Player player, long delayTicks, Consumer<LACPlayer.Context> action) {
        capture(player).ifPresent(context -> executeLater(context, delayTicks, action));
    }

    public static void executeLater(LACPlayer.Context context, long delayTicks, Consumer<LACPlayer.Context> action) {
        Scheduler.runTaskLater(context.player(), () -> {
            if (!context.isCurrent()) return;
            action.accept(context);
        }, delayTicks);
    }

    public static void queueStateRefresh(Player player, Consumer<LACPlayer.Context> action) {
        Optional<LACPlayer.Context> ctxOpt = capture(player);
        if (!ctxOpt.isPresent()) return;
        LACPlayer.Context context = ctxOpt.get();
        if (!context.owner().tryQueueStateRefresh(context)) return;
        try {
            execute(context, true, ctx -> {
                try {
                    action.accept(ctx);
                } finally {
                    ctx.owner().finishStateRefresh(ctx);
                }
            });
        } catch (RuntimeException exception) {
            context.owner().finishStateRefresh(context);
            throw exception;
        }
    }

}
