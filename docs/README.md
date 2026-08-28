# LightAntiCheat — Documentation

LightAntiCheat is a Spigot/Paper anti-cheat (Folia-supported via `folia-supported: true` in `plugin.yml`) that validates player movement, combat, interaction, packet, inventory, and player behaviour against vanilla expectations; it offers two input transports selectable via `listener-mode` — `packet` (PacketEvents `2.13.0`, `scope=provided`, not shaded, required only in `packet` mode) and `nms` (built-in Netty injection via `LightInjector`, no PacketEvents required) — with per-check `enabled`/`detection`/`punishment`/`setback` tuning, configurable violation thresholds and console-dispatched punishment commands, plus alerts, file logging, and Discord webhooks gated by cooldowns.

## Documentation Index

- [Getting Started](getting-started.md)
- [Commands and Permissions](commands-and-permissions.md)
- [Configuration](configuration.md)
- [Punishments, Alerts, and Placeholders](punishments-alerts-and-placeholders.md)
- [Movement Checks](checks/movement.md)
- [Combat Checks](checks/combat.md)
- [Interaction Checks](checks/interaction.md)
- [Packet / Inventory / Player Checks](checks/packet-inventory-player.md)
- [Integrations](integrations.md)
- [Architecture](architecture.md)
- [Developer API](developer-api.md)
- [Development](development.md)
- [Troubleshooting](troubleshooting.md)

## Reading Paths

### Server Owner

1. [Getting Started](getting-started.md) — requirements, installation, `listener-mode` (`packet`/`nms`), Folia and startup lifecycle.
2. [Configuration](configuration.md) — canonical `config.yml` domains, updater, and transactional reload.
3. [Commands and Permissions](commands-and-permissions.md) — `/lightanticheat` sub-commands and permission nodes.
4. [Punishments, Alerts, and Placeholders](punishments-alerts-and-placeholders.md) — violation → punishment flow, cooldown channels, placeholders.
5. Checks — [Movement](checks/movement.md), [Combat](checks/combat.md), [Interaction](checks/interaction.md), [Packet / Inventory / Player](checks/packet-inventory-player.md).
6. [Integrations](integrations.md) — `plugin.yml` descriptor vs source hooks and missing-plugin behaviour.
7. [Troubleshooting](troubleshooting.md) — PacketEvents errors, `listener-mode` handling, punishment and alert diagnostics.

### Developer

1. [Architecture](architecture.md) — input pipeline, provider boundary, owner-thread and event-bus design.
2. [Developer API](developer-api.md) — `LACApi`, `DetectionStatus`, `LACViolationEvent`/`LACPunishmentEvent`, and `api.enabled` semantics.
3. [Development](development.md) — build, Java target, dependencies, and test layout.
4. Checks — [Movement](checks/movement.md), [Combat](checks/combat.md), [Interaction](checks/interaction.md), [Packet / Inventory / Player](checks/packet-inventory-player.md).
5. [Integrations](integrations.md) — hook contracts and platform probes.

## Version and Artifact

Current artifact coordinates are defined in `pom.xml:1-10` as `groupId: com.arkflame`, `artifactId: LightAntiCheat-Plus`, `version: 2.0.2`, `packaging: jar`. The shaded output is `target/LightAntiCheat-Plus-2.0.2.jar` per `pom.xml:113` (`finalName`). The `version` in `src/main/resources/plugin.yml` is filtered from `pom.xml` at build time. Refer to `pom.xml` for the authoritative version.

## Canonical Defaults

`src/main/resources/config.yml` is the canonical source for exact default values, key paths, and types as bundled in the JAR and copied on first run. This documentation summarizes that file; when in doubt, read `config.yml` itself.
