package me.vekster.lightanticheat.check.buffer;

import me.vekster.lightanticheat.check.Check;
import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Buffer {

    public Buffer(Check check, UUID uuid) {
        this(check, uuid, false);
    }

    public Buffer(Check check, UUID uuid, boolean async) {
        this(Check.getCheckSetting(check).name, uuid, async);
    }

    public Buffer(Check check, Player player) {
        this(check, player.getUniqueId(), false);
    }

    public Buffer(Check check, Player player, boolean async) {
        this(Check.getCheckSetting(check).name, player.getUniqueId(), async);
    }

    private Buffer(CheckName checkName, UUID uuid, boolean async) {
        this.async = async;
        this.checkName = checkName;
        this.uuid = uuid;
        this.playerBuffer = getPlayerBuffer();
    }

    private static final Map<CheckName, Map<UUID, PlayerBuffer>> BUFFERS;
    private static final Map<CheckName, Map<UUID, PlayerBuffer>> ASYNC_BUFFERS = new ConcurrentHashMap<>();

    private final CheckName checkName;
    private final UUID uuid;
    private final PlayerBuffer playerBuffer;
    private final boolean async;

    private static final CheckName[] CHECK_NAMES = CheckName.values();
    private static final AtomicInteger SYNC_CLEAN_INDEX = new AtomicInteger();
    private static final AtomicInteger ASYNC_CLEAN_INDEX = new AtomicInteger();

    static {
        BUFFERS = !FoliaUtil.isFolia() ? new HashMap<>() : new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, PlayerBuffer> getCheckBuffer() {
        if (async)
            return ASYNC_BUFFERS.computeIfAbsent(checkName, k -> new ConcurrentHashMap<>());
        Map<UUID, PlayerBuffer> checkBuffer = BUFFERS.get(checkName);
        if (checkBuffer == null) {
            checkBuffer = new HashMap<>();
            BUFFERS.put(checkName, checkBuffer);
        }
        return checkBuffer;
    }

    private PlayerBuffer getPlayerBuffer() {
        Map<UUID, PlayerBuffer> checkBuffer = getCheckBuffer();
        if (async)
            return ((ConcurrentHashMap<UUID, PlayerBuffer>) checkBuffer)
                    .computeIfAbsent(uuid, k -> new PlayerBuffer(async));
        PlayerBuffer existing = checkBuffer.get(uuid);
        if (existing != null)
            return existing;
        PlayerBuffer newBuffer = new PlayerBuffer(async);
        checkBuffer.put(uuid, newBuffer);
        return newBuffer;
    }

    public static void loadBufferCleaner(long cacheTimeMils) {
        Scheduler.runTaskTimer(() -> {
            CheckName checkNameToClean = CHECK_NAMES[SYNC_CLEAN_INDEX.getAndIncrement() % CHECK_NAMES.length];
            long time = System.currentTimeMillis();

            Map<UUID, PlayerBuffer> checkBuffer = BUFFERS.get(checkNameToClean);
            if (checkBuffer != null && !checkBuffer.isEmpty())
                checkBuffer.entrySet().removeIf(entry -> time - entry.getValue().updated > cacheTimeMils);
        }, 1, 1);

        Scheduler.runTaskTimerAsynchronously(() -> {
            CheckName checkNameToClean = CHECK_NAMES[ASYNC_CLEAN_INDEX.getAndIncrement() % CHECK_NAMES.length];
            long time = System.currentTimeMillis();

            Map<UUID, PlayerBuffer> asyncCheckBuffer = ASYNC_BUFFERS.get(checkNameToClean);
            if (asyncCheckBuffer != null && !asyncCheckBuffer.isEmpty())
                asyncCheckBuffer.entrySet().removeIf(entry -> time - entry.getValue().updated > cacheTimeMils);
        }, 1, 1);
    }

    public boolean isExists(String key) {
        return playerBuffer.containsKey(key);
    }

    public Integer getInt(String key) {
        PlayerBuffer.PlayerVariable v = playerBuffer.get(key);
        if (v == null || !(v.object instanceof Integer))
            return 0;
        return (Integer) v.object;
    }

    public Long getLong(String key) {
        PlayerBuffer.PlayerVariable v = playerBuffer.get(key);
        if (v == null || !(v.object instanceof Long))
            return 0L;
        return (Long) v.object;
    }

    public Float getFloat(String key) {
        PlayerBuffer.PlayerVariable v = playerBuffer.get(key);
        if (v == null || !(v.object instanceof Float))
            return 0.0F;
        return (Float) v.object;
    }

    public Double getDouble(String key) {
        PlayerBuffer.PlayerVariable v = playerBuffer.get(key);
        if (v == null || !(v.object instanceof Double))
            return 0.0;
        return (Double) v.object;
    }

    public Boolean getBoolean(String key) {
        PlayerBuffer.PlayerVariable v = playerBuffer.get(key);
        if (v == null || !(v.object instanceof Boolean))
            return false;
        return (Boolean) v.object;
    }

    public String getString(String key) {
        PlayerBuffer.PlayerVariable v = playerBuffer.get(key);
        if (v == null || !(v.object instanceof String))
            return null;
        return (String) v.object;
    }

    public Location getLocation(String key) {
        PlayerBuffer.PlayerVariable v = playerBuffer.get(key);
        if (v == null || !(v.object instanceof Location))
            return null;
        return (Location) v.object;
    }

    public Block getBlock(String key) {
        PlayerBuffer.PlayerVariable v = playerBuffer.get(key);
        if (v == null || !(v.object instanceof Block))
            return null;
        return (Block) v.object;
    }

    public Material getMaterial(String key) {
        PlayerBuffer.PlayerVariable v = playerBuffer.get(key);
        if (v == null || !(v.object instanceof Material))
            return null;
        return (Material) v.object;
    }

    public UUID getUUID(String key) {
        PlayerBuffer.PlayerVariable v = playerBuffer.get(key);
        if (v == null || !(v.object instanceof UUID))
            return null;
        return (UUID) v.object;
    }

    public Entity getEntity(String key) {
        PlayerBuffer.PlayerVariable v = playerBuffer.get(key);
        if (v == null || !(v.object instanceof Entity))
            return null;
        return (Entity) v.object;
    }

    public void put(String key, Object object) {
        playerBuffer.put(key, new PlayerBuffer.PlayerVariable(object));
    }

}
