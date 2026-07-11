package me.vekster.lightanticheat.player;

import me.vekster.lightanticheat.player.cache.PlayerCache;
import me.vekster.lightanticheat.player.cooldown.PlayerCooldown;
import me.vekster.lightanticheat.player.violation.PlayerViolations;
import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import me.vekster.lightanticheat.version.VerPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class LACPlayer extends VerPlayer {

    LACPlayer(Player player) {
        super(player);
        this.uuid = player.getUniqueId();
        this.joinTime = System.currentTimeMillis();
        this.leaveTime = 0;
        this.cooldown = new PlayerCooldown();
        this.violations = new PlayerViolations();
    }

    public UUID uuid;
    public long joinTime;
    public long leaveTime;

    public volatile PlayerCache cache;
    public PlayerCooldown cooldown;
    public PlayerViolations violations;
    public volatile boolean alerts = true;

    final AtomicLong epoch = new AtomicLong();
    final AtomicReference<LACPlayer.Context> queuedStateRefresh = new AtomicReference<>();
    volatile UUID worldId;
    volatile boolean active;

    public record Context(LACPlayer owner, Player player, PlayerCache cache, UUID worldId, long epoch) {
        public Context {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(cache, "cache");
            Objects.requireNonNull(worldId, "worldId");
            if (epoch < 1L) throw new IllegalArgumentException("epoch must be >= 1");
        }

        public boolean isCurrent() {
            return owner.isCurrent(this);
        }
    }

    public static LACPlayer getLacPlayer(UUID uuid) {
        return LACPlayerManager.find(uuid).orElse(null);
    }

    public static LACPlayer getLacPlayer(Player player) {
        return LACPlayerManager.find(player).orElse(null);
    }

    void attach(Player player) {
        active = false;
        epoch.incrementAndGet();
        bindPlayer(player);
        worldId = player.getWorld().getUID();
        Location location = player.getLocation().clone();
        this.cache = new PlayerCache(location, this.alerts);
        joinTime = System.currentTimeMillis();
        leaveTime = 0;
        queuedStateRefresh.set(null);
        active = true;
    }

    void beginTransition(Player player) {
        if (boundPlayer() != player) return;
        active = false;
        epoch.incrementAndGet();
        queuedStateRefresh.set(null);
    }

    void completeTransition(Player player) {
        if (boundPlayer() != player) return;
        if (active) return;
        bindPlayer(player);
        worldId = player.getWorld().getUID();
        Location location = player.getLocation().clone();
        this.cache = new PlayerCache(location, this.alerts);
        epoch.incrementAndGet();
        queuedStateRefresh.set(null);
        active = true;
    }

    void detach(Player player) {
        if (boundPlayer() != player) return;
        active = false;
        epoch.incrementAndGet();
        queuedStateRefresh.set(null);
        leaveTime = System.currentTimeMillis();
    }

    public Optional<LACPlayer.Context> capture(Player player) {
        Player bound = boundPlayer();
        if (bound != player) return Optional.empty();
        if (!active || cache == null || worldId == null) return Optional.empty();
        long e = epoch.get();
        if (e < 1L) return Optional.empty();
        return Optional.of(new Context(this, bound, cache, worldId, e));
    }

    boolean isCurrent(LACPlayer.Context context) {
        if (context == null) return false;
        if (this != context.owner()) return false;
        if (!active) return false;
        if (boundPlayer() != context.player()) return false;
        if (cache != context.cache()) return false;
        if (epoch.get() != context.epoch()) return false;
        if (worldId == null || !worldId.equals(context.worldId())) return false;
        Player p = context.player();
        if (p == null || !p.isOnline()) return false;
        if (p.getWorld() == null || !p.getWorld().getUID().equals(context.worldId())) return false;
        if (FoliaUtil.isFolia() && !FoliaUtil.isOwnedByCurrentRegion(p)) return false;
        return true;
    }

    boolean tryQueueStateRefresh(LACPlayer.Context context) {
        return context.isCurrent() && queuedStateRefresh.compareAndSet(null, context);
    }

    void finishStateRefresh(LACPlayer.Context context) {
        queuedStateRefresh.compareAndSet(context, null);
    }

}
