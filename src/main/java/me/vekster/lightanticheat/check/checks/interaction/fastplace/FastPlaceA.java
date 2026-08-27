package me.vekster.lightanticheat.check.checks.interaction.fastplace;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.interaction.InteractionCheck;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.playerplaceblock.LACAsyncPlayerPlaceBlockEvent;
import me.vekster.lightanticheat.event.playerplaceblock.LACPlayerPlaceBlockEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Placement interval
 */
public class FastPlaceA extends InteractionCheck implements Listener {
    public FastPlaceA() {
        super(CheckName.FASTPLACE_A);
    }

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, this, "onAsyncBlockPlace", event -> onAsyncBlockPlace((LACAsyncPlayerPlaceBlockEvent) event));
        LACEventBus.register(LACEventType.PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, this, "onBlockPlace", event -> onBlockPlace((LACPlayerPlaceBlockEvent) event));
    }

    public void onAsyncBlockPlace(LACAsyncPlayerPlaceBlockEvent event) {
        Player player = event.getPlayer();
        LACPlayer lacPlayer = event.getLacPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, lacPlayer, true)) {
            buffer.put("asyncFlag", false);
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (!buffer.isExists("lastAsyncPlace")) {
            buffer.put("lastAsyncPlace", currentTime);
            buffer.put("asyncFlag", false);
            return;
        }

        long interval = currentTime - buffer.getLong("lastAsyncPlace");
        if (interval > 4L) {
            buffer.put("lastAsyncPlace", currentTime);
            return;
        }

        buffer.put("lastAsyncPlace", currentTime);
        buffer.put("asyncFlag", true);
    }

    public void onBlockPlace(LACPlayerPlaceBlockEvent event) {
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);
        if (!buffer.getBoolean("asyncFlag"))
            return;

        LACPlayer lacPlayer = event.getLacPlayer();
        if (!isCheckAllowed(player, lacPlayer))
            return;

        long currentTime = System.currentTimeMillis();
        if (!buffer.isExists("lastPlace")) {
            buffer.put("lastPlace", currentTime);
            return;
        }

        long interval = currentTime - buffer.getLong("lastPlace");
        if (interval > 3L) {
            buffer.put("lastPlace", currentTime);
            return;
        }

        if (currentTime - buffer.getLong("lastFlag1") > 8 * 1000) {
            buffer.put("lastFlag1", System.currentTimeMillis());
            buffer.put("lastPlace1", currentTime);
            return;
        }

        if (currentTime - buffer.getLong("lastFlag2") > 6 * 1000) {
            buffer.put("lastFlag2", System.currentTimeMillis());
            buffer.put("lastPlace2", currentTime);
            return;
        }

        buffer.put("lastPlace", currentTime);

        callViolationEvent(player, lacPlayer, null);
    }

}
