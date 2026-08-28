# Commands and Permissions

Source of truth: `src/main/java/me/vekster/lightanticheat/command/LACCommand.java:44-254` (branches), `src/main/java/me/vekster/lightanticheat/util/permission/ACPermission.java:1-18` (constants), `src/main/resources/plugin.yml:22-46` (descriptor).

Global gate (`LACCommand.java:46-49`): if `ConfigManager.Config.enabled == false` (`config.yml:1`), **every** invocation immediately replies `§8This plugin is disabled!` and returns — no permission check, no sub-command dispatch.

Usage errors use `config.yml:44` `messages.error-messages.invalid-format: "%prefix% Usage: &7%usage%"` via `LACCommand.java:36-42`. Permission errors use `config.yml:42` `no-permission: "%prefix% You don't have enough permissions"` via `LACCommand.java:29-34`. Both are colorized only for non-console senders (`hex = !(sender instanceof ConsoleCommandSender)`).

## Command

Primary: `/lightanticheat` — registered as `lightanticheat` in `plugin.yml:23`. Aliases (`plugin.yml:24`): `/light`, `/lac`, `/anticheat`, `/ac`. Tab-complete base offers `checks reload client cps tps ping alerts teleport` (`LACCommand.java:261-262`).

Error messages echo the **typed label** (`"/" + label + " ..."`) so usage matches the alias used.

### `/lightanticheat` (no args)

| Field | Value |
|---|---|
| Handler | `LACCommand.java:248-253` fallback when `args.length==0` or unmatched `args[0]` |
| Permission | none |
| Console | yes |
| Purpose | Show help: `ConfigManager.Config.Messages.CommandMessages.Help.message` (`config.yml:13-14` → `"%prefix% LightAntiCheat %version% is running"`) |

### `/lightanticheat client <player>`

| Field | Value |
|---|---|
| Handler | `LACCommand.java:54-75` |
| Permission | `lightanticheat.client` **OR** `lightanticheat.*` (`ACPermission.CLIENT`, `ALL`) |
| Console | yes |
| Purpose | Report target's client brand (`ClientBrandRecognizer.getClientBrand(player)`) via `messages.command-messages.client.message` (`config.yml:31-33` → `"%prefix% %name%&7's client is &f%client-brand%"`) |
| Target | `Bukkit.getPlayer(args[1])` — exact name, must be online. Offline/unknown → treated as invalid format |
| Failure | No permission → `no-permission`. `args.length==1` or `player==null` → `invalid-format` with usage `"/<label> client <player>"` |

### `/lightanticheat cps <player>`

| Field | Value |
|---|---|
| Handler | `LACCommand.java:77-98` |
| Permission | `lightanticheat.cps` **OR** `lightanticheat.*` (`ACPermission.CPS`) |
| Console | yes |
| Purpose | Report CPS via `CPSListener.getCps(player)` and `messages.command-messages.cps.message` (`config.yml:34-36`) |
| Target | `Bukkit.getPlayer(args[1])`, online required |
| Failure | Same pattern: `no-permission` or `invalid-format` `"/<label> cps <player>"` |

### `/lightanticheat reload`

| Field | Value |
|---|---|
| Handler | `LACCommand.java:100-113` |
| Permission | `lightanticheat.reload` **OR** `lightanticheat.*` (`ACPermission.RELOAD`) |
| Console | yes |
| Purpose | `ConfigManager.reloadConfig()` (`ConfigManager.java:253-295`): reloads `config.yml`, re-parses `listener-mode` and reconfigures `LACInputEngine`, reloads every `CheckSetting`, re-registers listeners, then replies with `messages.command-messages.reload.message` (`config.yml:15-17` → `"%prefix% Successfully reloaded the config &7(%time%ms)"`) with elapsed ms |
| Failure | `no-permission`; no args required — extra args beyond `reload` are ignored in dispatch |

### `/lightanticheat checks`

| Field | Value |
|---|---|
| Handler | `LACCommand.java:115-144` |
| Permission | `lightanticheat.checks` **OR** `lightanticheat.*` (`ACPermission.CHECKS`) |
| Console | yes |
| Purpose | List every `CheckName` (`CheckName.java:3-61`) with `§a` enabled / `§c` disabled, via `messages.command-messages.checks.message` (`config.yml:37-40` → `"%prefix% Checks: %checks%"`). Also fills per-type placeholders `%movement_checks%`, `%combat_checks%`, `%interaction_checks%`, `%inventory_checks%`, `%packet_checks%` |
| Failure | `no-permission`; takes no args |

### `/lightanticheat tps`

| Field | Value |
|---|---|
| Handler | `LACCommand.java:146-156` |
| Permission | `lightanticheat.tps` **OR** `lightanticheat.*` (`ACPermission.TPS`) |
| Console | yes |
| Purpose | Report TPS via `messages.command-messages.tps.message` (`config.yml:21-23` → `"%prefix% TPS from the last 30 seconds: &f%tps%"`), placeholder filled from `TPSCalculator` |
| Failure | `no-permission` |

### `/lightanticheat ping <player>`

| Field | Value |
|---|---|
| Handler | `LACCommand.java:158-179` |
| Permission | `lightanticheat.ping` **OR** `lightanticheat.*` (`ACPermission.PING`) |
| Console | yes |
| Purpose | Report target ping & stability via `messages.command-messages.ping.message` (`config.yml:24-30`) and `swapPlayer` (version-bridged ping) |
| Target | `Bukkit.getPlayer(args[1])`, online required |
| Failure | `no-permission` or `invalid-format` `"/<label> ping <player>"` when arg missing or player null |

### `/lightanticheat alerts`

| Field | Value |
|---|---|
| Handler | `LACCommand.java:181-202` |
| Permission | `lightanticheat.alerts.toggle` **OR** `lightanticheat.alerts` **OR** `lightanticheat.*` (`ACPermission.ALERTS_TOGGLE`, `ALERTS`, `ALL`) |
| Console | **no** — `!(sender instanceof Player)` → `§8This command is only available for players` (`LACCommand.java:187-190`) |
| Purpose | Toggle `LACPlayer.cache.alerts` for the sender; replies `messages.command-messages.alerts.toggled-on-message` / `toggled-off-message` (`config.yml:18-20`) |
| Failure | `no-permission`; console → player-only msg; takes no args |

This is the only runtime check for `lightanticheat.alerts.toggle`. Note: alert **broadcast** (`Logger.java:49`) and updater join notification (`Updater.java:105`) require `lightanticheat.alerts.notify` or `lightanticheat.alerts` (see permissions table), distinct from toggling.

### `/lightanticheat teleport <world> <x> <y> <z>`
### `/lightanticheat teleport <world> <x> <y> <z> <yaw> <pitch>`

| Field | Value |
|---|---|
| Handler | `LACCommand.java:204-246` |
| Permission | `lightanticheat.alerts.teleport` **OR** `lightanticheat.alerts` **OR** `lightanticheat.*` (`ACPermission.ALERTS_TELEPORT`, `ALERTS`, `ALL`) |
| Console | **no** — same player-only guard (`LACCommand.java:214-217`) |
| Purpose | Teleport sender to arbitrary coordinates. Used as click-action for violation/punishment alerts (`config.yml:55-56,64-65` → `on-click: "/light teleport %teleport-location%"`). 5-arg form uses yaw=0/pitch=0; 7-arg form parses yaw/pitch as `float`. Delegates to `FoliaUtil.teleportPlayer` (`FoliaUtil.java:135-147`, `LACCommand.java:242-244`) |
| Syntax | Exactly `args.length==5` (`teleport` + 4) or `args.length==7` (`teleport` + 6). Any other length → `invalid-format` with usage `"/<label> teleport <world> <x> <y> <z>"` |
| Target | `Bukkit.getWorld(args[1])` — must exist. `x,y,z` as `double`, optional `yaw,pitch` as `float`; `NumberFormatException` → `invalid-format` same usage. Tab-complete suggests `world world_nether world_the_end x y z yaw pitch` (`LACCommand.java:268-280`) |
| Failure | `no-permission`; player-only msg for console; invalid world/coords → `invalid-format` |

## Permissions

### Constants (`ACPermission.java:1-18`)

```java
lightanticheat.checks          // CHECKS
lightanticheat.reload          // RELOAD
lightanticheat.alerts          // ALERTS
lightanticheat.alerts.notify   // ALERTS_NOTIFY
lightanticheat.alerts.toggle   // ALERTS_TOGGLE
lightanticheat.alerts.teleport // ALERTS_TELEPORT
lightanticheat.client          // CLIENT
lightanticheat.tps             // TPS
lightanticheat.ping            // PING
lightanticheat.cps             // CPS
lightanticheat.bypass          // BYPASS
lightanticheat.*               // ALL
 + per-check: lightanticheat.bypass.<lowercase_check_enum>
```

| Permission | Used where | Default | Purpose |
|---|---|---|---|
| `lightanticheat.*` (`ALL`) | Every command branch as wildcard (`LACCommand.java:55,78,101,116,147,159,183,206`) | `op` (`plugin.yml:26-34` children grant) | Grants all listed below |
| `lightanticheat.checks` | `checks` command (`LACCommand.java:116`) | `op` (`plugin.yml:45-46`) | View check list |
| `lightanticheat.reload` | `reload` command (`LACCommand.java:101`) | `op` (`plugin.yml:43-44`) | Reload config |
| `lightanticheat.alerts` | `alerts` toggle fallback, `teleport` fallback (`LACCommand.java:182,205`), alert broadcast gate (`Logger.java:49`, `Updater.java:106`) | `op` (`plugin.yml:35-36`) | View alert broadcasts; also satisfies toggle/teleport/notify checks |
| `lightanticheat.alerts.notify` (`ALERTS_NOTIFY`) | Alert broadcast filter (`Logger.java:49` `!hasPermission(ALERTS_NOTIFY) && !hasPermission(ALERTS)`), updater join notify (`Updater.java:105`) | **not declared** | Receive violation/punishment hover broadcasts when `cache.alerts` is on |
| `lightanticheat.alerts.toggle` (`ALERTS_TOGGLE`) | `alerts` command (`LACCommand.java:182`) | **not declared** | Toggle own alerts |
| `lightanticheat.alerts.teleport` (`ALERTS_TELEPORT`) | `teleport` command (`LACCommand.java:205`) | **not declared** | Teleport to violation location |
| `lightanticheat.client` | `client` command (`LACCommand.java:55`) | `op` (`plugin.yml:37-38`) | Query client brand |
| `lightanticheat.tps` | `tps` command (`LACCommand.java:147`) | `op` (`plugin.yml:41-42`) | Query TPS |
| `lightanticheat.ping` | `ping` command (`LACCommand.java:159`) | **not declared** | Query player ping |
| `lightanticheat.cps` | `cps` command (`LACCommand.java:78`) | `op` (`plugin.yml:39-40`) | Query CPS |
| `lightanticheat.bypass` (`BYPASS`) | `CheckUtil.java:70` `hasPermission(BYPASS)` | **not declared** | Bypass **all** checks (when `permission.disable-all-bypass-permissions == false`) |
| `lightanticheat.bypass.<check>` | `CheckSetting.java:11`, `CheckUtil.java:74-75` | **not declared** | Per-check bypass, e.g. `lightanticheat.bypass.flight_a` |

### Per-check bypass (`lightanticheat.bypass.<lowercase_check_enum>`)

Construction: `CheckSetting.java:8-12` — `apiName = name().toLowerCase(Locale.ROOT)` (e.g. `FLIGHT_A` → `flight_a`), `bypassPermission = "lightanticheat.bypass." + apiName`. Applies to every `CheckName` value (`CheckName.java:3-61`).

Activation (`CheckUtil.java:69-77`):

```java
if (!Config.Permission.disableAllBypassPermissions) {          // config.yml:111
    if (hasPermission(lacPlayer.cooldown, player, BYPASS))     // "lightanticheat.bypass"
        return false; // skip check
    if (Config.Permission.perCheckBypassPermission              // config.yml:109  default false
        && hasPermission(lacPlayer.cooldown, player, checkSetting.bypassPermission))
        return false; // skip this check
}
```

- `permission.disable-all-bypass-permissions: false` (`config.yml:111`) — master kill-switch. When `true`, both generic and per-check bypasses are ignored.
- `permission.per-check-bypass-permission: false` (`config.yml:108-109`) — when `false`, only `lightanticheat.bypass` is honored; per-check nodes are ignored even if granted.
- When enabled, `lightanticheat.bypass.flight_a` bypasses `Flight_A` only, `lightanticheat.bypass.killaura_a` bypasses `KillAura_A`, etc. Names are lowercased enum names, not titles.

### Descriptor vs runtime

`plugin.yml:22-46` declares **only**:

```
lightanticheat.*
lightanticheat.alerts, lightanticheat.client, lightanticheat.cps,
lightanticheat.tps, lightanticheat.reload, lightanticheat.checks
```

All with `default: op`. The following runtime permissions are **not declared** in `plugin.yml` and thus default to `op: false` (no implicit grant) unless explicitly set via permissions plugin:

```
lightanticheat.ping
lightanticheat.alerts.notify, lightanticheat.alerts.toggle, lightanticheat.alerts.teleport
lightanticheat.bypass, lightanticheat.bypass.*
```

`lightanticheat.*` children in descriptor (`plugin.yml:28-34`) grant `alerts client cps tps reload checks` only — they do **not** include `ping`, `alerts.notify/toggle/teleport`, or `bypass`. Those require explicit grant or wildcard via permissions plugin that respects `*` semantically.

### Notes

- All permission checks in commands use `sender.hasPermission(String)` OR `hasPermission(ALL)` — granting `lightanticheat.*` via plugin always passes even if descriptor children are incomplete.
- Bypass permission resolution is cached per-player via `CooldownUtil.hasPermission(PlayerCooldown, Player, String, boolean)` — async-aware wrapper around `player.hasPermission`.
- `ALERTS_NOTIFY` vs `ALERTS`: logger checks both (`Logger.java:49`); granting either suffices to receive alerts. Similarly, command gates accept `ALERTS` as fallback for toggle/teleport.
