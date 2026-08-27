package me.vekster.lightanticheat.player;

import me.vekster.lightanticheat.input.model.LACLocation;
import me.vekster.lightanticheat.input.model.LACPlayerSession;
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
    volatile LACLocation lastKnownLocation;
    volatile LACLocation seedLocation;

    public static final class Context {
        private final LACPlayer owner;
        private final Player player;
        private final PlayerCache cache;
        private final UUID worldId;
        private final long epoch;

        public Context(LACPlayer owner, Player player, PlayerCache cache, UUID worldId, long epoch) {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(cache, "cache");
            Objects.requireNonNull(worldId, "worldId");
            if (epoch < 1L) {
                throw new IllegalArgumentException("epoch must be >= 1");
            }
            this.owner = owner;
            this.player = player;
            this.cache = cache;
            this.worldId = worldId;
            this.epoch = epoch;
        }

        public LACPlayer owner() {
            return owner;
        }

        public Player player() {
            return player;
        }

        public PlayerCache cache() {
            return cache;
        }

        public UUID worldId() {
            return worldId;
        }

        public long epoch() {
            return epoch;
        }

        public boolean isCurrent() {
            return owner.isCurrent(this);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof Context)) return false;
            Context other = (Context) object;
            if (epoch != other.epoch) return false;
            if (!Objects.equals(owner, other.owner)) return false;
            if (!Objects.equals(player, other.player)) return false;
            if (!Objects.equals(cache, other.cache)) return false;
            return Objects.equals(worldId, other.worldId);
        }

        @Override
        public int hashCode() {
            int result = owner != null ? owner.hashCode() : 0;
            result = 31 * result + (player != null ? player.hashCode() : 0);
            result = 31 * result + (cache != null ? cache.hashCode() : 0);
            result = 31 * result + (worldId != null ? worldId.hashCode() : 0);
            result = 31 * result + (int) (epoch ^ (epoch >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "Context{owner=" + owner + ", player=" + player + ", cache=" + cache + ", worldId=" + worldId + ", epoch=" + epoch + '}';
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
        LACLocation lacLocation = new LACLocation(worldId, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        seedLocation = lacLocation;
        lastKnownLocation = lacLocation;
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
        LACLocation lacLocation = new LACLocation(worldId, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        seedLocation = lacLocation;
        lastKnownLocation = lacLocation;
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

    public Optional<LACPlayerSession> captureSession() {
        if (!active) return Optional.empty();
        UUID wid = worldId;
        long e = epoch.get();
        if (wid == null || e < 1L) return Optional.empty();
        return Optional.of(new LACPlayerSession(uuid, wid, e));
    }

    public boolean matchesSession(LACPlayerSession session) {
        if (session == null) return false;
        if (!uuid.equals(session.getPlayerId())) return false;
        if (!active) return false;
        long e = epoch.get();
        UUID wid = worldId;
        return e == session.getPlayerEpoch() && wid != null && wid.equals(session.getWorldId());
    }

    public Optional<LACPlayer.Context> captureCurrent(LACPlayerSession session) {
        if (session == null) return Optional.empty();
        if (!matchesSession(session)) return Optional.empty();
        Player bound = boundPlayer();
        PlayerCache c = cache;
        UUID wid = worldId;
        long e = epoch.get();
        if (c == null || wid == null || e < 1L) return Optional.empty();
        LACPlayer.Context context = new LACPlayer.Context(this, bound, c, wid, e);
        if (!context.isCurrent()) return Optional.empty();
        return Optional.of(context);
    }

    public LACLocation getLastKnownLocation() {
        return lastKnownLocation;
    }

    public void setLastKnownLocation(LACLocation location) {
        lastKnownLocation = location;
    }

    public LACLocation getSeedLocation() {
        return seedLocation;
    }

    public Player peekPlayer() {
        return boundPlayer();
    }

}
