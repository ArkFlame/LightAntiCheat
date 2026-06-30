package me.vekster.lightanticheat.util.api;

import me.vekster.lightanticheat.api.CheckType;
import me.vekster.lightanticheat.api.DetectionStatus;
import me.vekster.lightanticheat.api.InstanceHolder;
import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.CheckSetting;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ApiUtil {

    public static final Set<String> CHECK_NAMES = ConcurrentHashMap.newKeySet();
    public static final Map<String, Set<String>> CHECK_NAMES_BY_TYPE = new ConcurrentHashMap<>();
    public static final Map<DisabledCheckKey, Long> DISABLED_CHECKS = new ConcurrentHashMap<>();

    private static final class DisabledCheckKey {
        private final UUID uuid;
        private final String checkName;

        private DisabledCheckKey(final UUID uuid, final String checkName) {
            this.uuid = Objects.requireNonNull(uuid, "uuid");
            this.checkName = Objects.requireNonNull(checkName, "checkName");
        }

        @Override
        public boolean equals(final Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof DisabledCheckKey)) {
                return false;
            }
            final DisabledCheckKey other = (DisabledCheckKey) object;
            return uuid.equals(other.uuid) && checkName.equals(other.checkName);
        }

        @Override
        public int hashCode() {
            return 31 * uuid.hashCode() + checkName.hashCode();
        }
    }

    static {
        for (CheckName checkName : CheckName.values()) {
            CHECK_NAMES.add(checkName.name().toLowerCase());
        }
        for (CheckName.CheckType checkType : CheckName.CheckType.values()) {
            String checkTypeName = checkType.name().toLowerCase();
            Set<String> checkTypeChecks = ConcurrentHashMap.newKeySet();
            for (CheckName checkName : CheckName.values())
                if (checkTypeName.equals(checkName.type.name().toLowerCase()))
                    checkTypeChecks.add(checkName.name().toLowerCase());
            CHECK_NAMES_BY_TYPE.put(checkTypeName, checkTypeChecks);
        }

        Scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                DISABLED_CHECKS.entrySet().removeIf(entry -> {
                    Player player = Bukkit.getPlayer(entry.getKey().uuid);
                    if (player == null)
                        return true;
                    if (entry.getValue() != 0L && entry.getValue() < System.currentTimeMillis())
                        return true;
                    return false;
                });
            }
        }, 1000, 1000);
    }

    public static void setApiInstance() {
        InstanceHolder.setApi(new ApiInstance());
    }

    public static Set<String> getCheckNames(CheckType checkType) {
        if (checkType.name().equalsIgnoreCase("all"))
            return CHECK_NAMES;
        return CHECK_NAMES_BY_TYPE.getOrDefault(checkType.name().toLowerCase(), ConcurrentHashMap.newKeySet());
    }

    public static boolean disableCheck(Player player, String checkName, long duration) {
        checkName = checkName.toLowerCase();
        if (!CHECK_NAMES.contains(checkName))
            return false;
        DisabledCheckKey disabledCheck = new DisabledCheckKey(player.getUniqueId(), checkName);
        if (!DISABLED_CHECKS.containsKey(disabledCheck)) {
            DISABLED_CHECKS.put(disabledCheck, System.currentTimeMillis() + duration);
            return true;
        }
        long endTime = DISABLED_CHECKS.get(disabledCheck);
        if (endTime != 0 && endTime < System.currentTimeMillis() + duration) {
            DISABLED_CHECKS.put(disabledCheck, System.currentTimeMillis() + duration);
            return true;
        }
        return false;
    }

    public static boolean disableCheck(Player player, String checkName) {
        checkName = checkName.toLowerCase();
        if (!CHECK_NAMES.contains(checkName))
            return false;
        DisabledCheckKey disabledCheck = new DisabledCheckKey(player.getUniqueId(), checkName);
        if (!DISABLED_CHECKS.containsKey(disabledCheck)) {
            DISABLED_CHECKS.put(disabledCheck, 0L);
            return true;
        }
        long endTime = DISABLED_CHECKS.get(disabledCheck);
        if (endTime != 0) {
            DISABLED_CHECKS.put(disabledCheck, 0L);
            return true;
        }
        return false;
    }

    public static boolean enableCheck(Player player, String checkName) {
        checkName = checkName.toLowerCase();
        if (!CHECK_NAMES.contains(checkName))
            return false;
        DisabledCheckKey disabledCheck = new DisabledCheckKey(player.getUniqueId(), checkName);
        DISABLED_CHECKS.remove(disabledCheck);
        return true;
    }

    public static DetectionStatus getCheckStatus(final Player player, final CheckSetting checkSetting) {
        if (player == null || checkSetting == null) {
            return DetectionStatus.DISABLED;
        }
        return getCheckStatusLowercase(player, checkSetting.apiName);
    }

    public static DetectionStatus getCheckStatus(Player player, String checkName) {
        checkName = checkName.toLowerCase();
        return getCheckStatusLowercase(player, checkName);
    }

    private static DetectionStatus getCheckStatusLowercase(final Player player, final String checkName) {
        if (!CHECK_NAMES.contains(checkName))
            return DetectionStatus.DISABLED;
        final DisabledCheckKey disabledCheck = new DisabledCheckKey(player.getUniqueId(), checkName);
        if (!DISABLED_CHECKS.containsKey(disabledCheck))
            return DetectionStatus.ENABLED;
        if (DISABLED_CHECKS.get(disabledCheck) == 0L)
            return DetectionStatus.DISABLED;
        return DISABLED_CHECKS.get(disabledCheck) < System.currentTimeMillis() ?
                DetectionStatus.ENABLED : DetectionStatus.TEMPORARILY_DISABLED;
    }

}
