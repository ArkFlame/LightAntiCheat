package me.vekster.lightanticheat.check;

import java.util.List;
import java.util.Locale;

public class CheckSetting {

    public CheckSetting(CheckName name) {
        this.name = name;
        this.apiName = name.name().toLowerCase(Locale.ROOT);
        this.bypassPermission = "lightanticheat.bypass." + this.apiName;
    }

    public CheckName name;
    public boolean enabled;
    public boolean punishable;
    public int punishmentVio;
    public double minTps;
    public int maxPing;
    public boolean detectJava;
    public boolean detectBedrock;
    public boolean setback;
    public int setbackVio;
    public List<String> punishmentCommands;
    public final String apiName;
    public final String bypassPermission;

}
