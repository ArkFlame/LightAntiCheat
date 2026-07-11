package me.vekster.lightanticheat.check.checks.packet;

import me.vekster.lightanticheat.check.Check;
import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.LACPlayerManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class PacketCheck extends Check {
    public PacketCheck(CheckName name) {
        super(name);
    }

    public boolean limitPackets(char prefix, Buffer buffer, long interval, int limit, int limitRepeatsNeeded) {
        buffer.put(prefix + "methodPackets", buffer.getInt(prefix + "methodPackets") + 1);

        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong(prefix + "methodStartTime") <= interval)
            return false;
        buffer.put(prefix + "methodStartTime", currentTime);

        int packets = buffer.getInt(prefix + "methodPackets");
        buffer.put(prefix + "methodPackets", 0);

        if (packets <= limit) {
            buffer.put(prefix + "methodFlags", 0);
            return false;
        }
        buffer.put(prefix + "methodFlags", buffer.getInt(prefix + "methodFlags") + 1);

        if (buffer.getInt(prefix + "methodFlags") < limitRepeatsNeeded)
            return false;
        buffer.put(prefix + "methodFlags", 0);
        return true;
    }

    @Nullable
    public Player getPlayer(Object object) {
        if (!(object instanceof Player))
            return null;
        Player player = (Player) object;
        if (!player.isOnline())
            return null;
        return player;
    }

    @Nullable
    public LACPlayer getLacPlayer(UUID uuid) {
        return LACPlayerManager.find(uuid)
                .filter(player -> player.leaveTime == 0L)
                .orElse(null);
    }

    public void flag(Player player, LACPlayer lacPlayer) {
        LACPlayerManager.execute(player, true, context -> {
            callViolationEvent(getCheckSetting(), context.player(), context.owner(), null);
        });
    }

}
