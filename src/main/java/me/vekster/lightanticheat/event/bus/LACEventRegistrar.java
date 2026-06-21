package me.vekster.lightanticheat.event.bus;

import me.vekster.lightanticheat.check.checks.combat.criticals.CriticalsA;
import me.vekster.lightanticheat.check.checks.combat.criticals.CriticalsB;
import me.vekster.lightanticheat.check.checks.combat.killaura.KillAuraA;
import me.vekster.lightanticheat.check.checks.combat.killaura.KillAuraB;
import me.vekster.lightanticheat.check.checks.combat.killaura.KillAuraC;
import me.vekster.lightanticheat.check.checks.combat.killaura.KillAuraD;
import me.vekster.lightanticheat.check.checks.combat.reach.ReachA;
import me.vekster.lightanticheat.check.checks.combat.reach.ReachB;
import me.vekster.lightanticheat.check.checks.interaction.airplace.AirPlaceA;
import me.vekster.lightanticheat.check.checks.interaction.blockbreak.BlockBreakA;
import me.vekster.lightanticheat.check.checks.interaction.blockbreak.BlockBreakB;
import me.vekster.lightanticheat.check.checks.interaction.blockplace.BlockPlaceA;
import me.vekster.lightanticheat.check.checks.interaction.blockplace.BlockPlaceB;
import me.vekster.lightanticheat.check.checks.interaction.fastbreak.FastBreakA;
import me.vekster.lightanticheat.check.checks.interaction.fastplace.FastPlaceA;
import me.vekster.lightanticheat.check.checks.interaction.ghostbreak.GhostBreakA;
import me.vekster.lightanticheat.check.checks.interaction.scaffold.ScaffoldA;
import me.vekster.lightanticheat.check.checks.interaction.scaffold.ScaffoldB;
import me.vekster.lightanticheat.check.checks.movement.boat.BoatA;
import me.vekster.lightanticheat.check.checks.movement.elytra.ElytraA;
import me.vekster.lightanticheat.check.checks.movement.elytra.ElytraB;
import me.vekster.lightanticheat.check.checks.movement.elytra.ElytraC;
import me.vekster.lightanticheat.check.checks.movement.fastclimb.FastClimbA;
import me.vekster.lightanticheat.check.checks.movement.flight.FlightA;
import me.vekster.lightanticheat.check.checks.movement.flight.FlightB;
import me.vekster.lightanticheat.check.checks.movement.flight.FlightC;
import me.vekster.lightanticheat.check.checks.movement.jump.JumpA;
import me.vekster.lightanticheat.check.checks.movement.jump.JumpB;
import me.vekster.lightanticheat.check.checks.movement.liquidwalk.LiquidWalkA;
import me.vekster.lightanticheat.check.checks.movement.liquidwalk.LiquidWalkB;
import me.vekster.lightanticheat.check.checks.movement.nofall.NoFallA;
import me.vekster.lightanticheat.check.checks.movement.nofall.NoFallB;
import me.vekster.lightanticheat.check.checks.movement.noslow.NoSlowA;
import me.vekster.lightanticheat.check.checks.movement.speed.SpeedA;
import me.vekster.lightanticheat.check.checks.movement.speed.SpeedB;
import me.vekster.lightanticheat.check.checks.movement.speed.SpeedC;
import me.vekster.lightanticheat.check.checks.movement.speed.SpeedD;
import me.vekster.lightanticheat.check.checks.movement.speed.SpeedE;
import me.vekster.lightanticheat.check.checks.movement.speed.SpeedF;
import me.vekster.lightanticheat.check.checks.movement.step.StepA;
import me.vekster.lightanticheat.check.checks.movement.trident.TridentA;
import me.vekster.lightanticheat.check.checks.movement.vehicle.VehicleA;
import me.vekster.lightanticheat.check.checks.packet.badpackets.BadPacketsA;
import me.vekster.lightanticheat.check.checks.packet.badpackets.BadPacketsB;
import me.vekster.lightanticheat.check.checks.packet.badpackets.BadPacketsC;
import me.vekster.lightanticheat.check.checks.packet.badpackets.BadPacketsD;
import me.vekster.lightanticheat.check.checks.packet.morepackets.MorePacketsA;
import me.vekster.lightanticheat.check.checks.packet.morepackets.MorePacketsB;
import me.vekster.lightanticheat.check.checks.packet.timer.TimerA;
import me.vekster.lightanticheat.check.checks.packet.timer.TimerB;
import me.vekster.lightanticheat.check.checks.player.autobot.AutoBotA;
import me.vekster.lightanticheat.check.checks.player.skinblinker.SkinBlinkerA;
import me.vekster.lightanticheat.event.packetrecive.LACAsyncPacketReceiveEvent;
import me.vekster.lightanticheat.event.playerattack.LACAsyncPlayerAttackEvent;
import me.vekster.lightanticheat.event.playerattack.LACPlayerAttackEvent;
import me.vekster.lightanticheat.event.playerbreakblock.LACAsyncPlayerBreakBlockEvent;
import me.vekster.lightanticheat.event.playerbreakblock.LACPlayerBreakBlockEvent;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.event.playermove.LACPlayerMoveEvent;
import me.vekster.lightanticheat.event.playerplaceblock.LACAsyncPlayerPlaceBlockEvent;
import me.vekster.lightanticheat.event.playerplaceblock.LACPlayerPlaceBlockEvent;
import me.vekster.lightanticheat.listener.invalidping.InvalidPingListener;
import me.vekster.lightanticheat.listener.unloadedchunk.UnloadedChunkListener;
import me.vekster.lightanticheat.player.LACPlayerListener;
import me.vekster.lightanticheat.util.player.connectionstability.ConnectionStabilityListener;
import org.bukkit.event.Listener;

public final class LACEventRegistrar {

    private LACEventRegistrar() {
    }

    public static void register(Listener listener) {
        LACEventBus.unregister(listener);

        if (listener instanceof CriticalsA) {
            CriticalsA typed = (CriticalsA) listener;
            LACEventBus.register(LACEventType.PLAYER_ATTACK, LACEventPriority.NORMAL, typed, "onHit",
                    event -> typed.onHit((LACPlayerAttackEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_ATTACK, LACEventPriority.NORMAL, typed, "onAsyncHit",
                    event -> typed.onAsyncHit((LACAsyncPlayerAttackEvent) event));
        } else if (listener instanceof CriticalsB) {
            CriticalsB typed = (CriticalsB) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_ATTACK, LACEventPriority.NORMAL, typed, "onAsyncHit",
                    event -> typed.onAsyncHit((LACAsyncPlayerAttackEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof KillAuraA) {
            KillAuraA typed = (KillAuraA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_ATTACK, LACEventPriority.NORMAL, typed, "onAsyncHit",
                    event -> typed.onAsyncHit((LACAsyncPlayerAttackEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof KillAuraB) {
            KillAuraB typed = (KillAuraB) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_ATTACK, LACEventPriority.NORMAL, typed, "onAsyncHit",
                    event -> typed.onAsyncHit((LACAsyncPlayerAttackEvent) event));
        } else if (listener instanceof KillAuraC) {
            KillAuraC typed = (KillAuraC) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_ATTACK, LACEventPriority.NORMAL, typed, "onAsyncHit",
                    event -> typed.onAsyncHit((LACAsyncPlayerAttackEvent) event));
            LACEventBus.register(LACEventType.PLAYER_ATTACK, LACEventPriority.NORMAL, typed, "onHit",
                    event -> typed.onHit((LACPlayerAttackEvent) event));
        } else if (listener instanceof KillAuraD) {
            KillAuraD typed = (KillAuraD) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_ATTACK, LACEventPriority.NORMAL, typed, "multiAuraAsync",
                    event -> typed.multiAuraAsync((LACAsyncPlayerAttackEvent) event));
            LACEventBus.register(LACEventType.PLAYER_ATTACK, LACEventPriority.NORMAL, typed, "multiAura",
                    event -> typed.multiAura((LACPlayerAttackEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_ATTACK, LACEventPriority.NORMAL, typed, "shieldAsync",
                    event -> typed.shieldAsync((LACAsyncPlayerAttackEvent) event));
            LACEventBus.register(LACEventType.PLAYER_ATTACK, LACEventPriority.NORMAL, typed, "shield",
                    event -> typed.shield((LACPlayerAttackEvent) event));
        } else if (listener instanceof ReachA) {
            ReachA typed = (ReachA) listener;
            LACEventBus.register(LACEventType.PLAYER_ATTACK, LACEventPriority.NORMAL, typed, "onHit",
                    event -> typed.onHit((LACPlayerAttackEvent) event));
        } else if (listener instanceof ReachB) {
            ReachB typed = (ReachB) listener;
            LACEventBus.register(LACEventType.PLAYER_ATTACK, LACEventPriority.NORMAL, typed, "onHit",
                    event -> typed.onHit((LACPlayerAttackEvent) event));
        } else if (listener instanceof AirPlaceA) {
            AirPlaceA typed = (AirPlaceA) listener;
            LACEventBus.register(LACEventType.PLAYER_PLACE_BLOCK, LACEventPriority.LOW, typed, "beforeBlockPlace",
                    event -> typed.beforeBlockPlace((LACPlayerPlaceBlockEvent) event));
            LACEventBus.register(LACEventType.PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, typed, "onBlockPlace",
                    event -> typed.onBlockPlace((LACPlayerPlaceBlockEvent) event));
        } else if (listener instanceof BlockBreakA) {
            BlockBreakA typed = (BlockBreakA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_BREAK_BLOCK, LACEventPriority.NORMAL, typed, "onAsyncBlockBreak",
                    event -> typed.onAsyncBlockBreak((LACAsyncPlayerBreakBlockEvent) event));
            LACEventBus.register(LACEventType.PLAYER_BREAK_BLOCK, LACEventPriority.NORMAL, typed, "onBlockBreak",
                    event -> typed.onBlockBreak((LACPlayerBreakBlockEvent) event));
        } else if (listener instanceof BlockBreakB) {
            BlockBreakB typed = (BlockBreakB) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_BREAK_BLOCK, LACEventPriority.NORMAL, typed, "onAsyncBlockBreak",
                    event -> typed.onAsyncBlockBreak((LACAsyncPlayerBreakBlockEvent) event));
        } else if (listener instanceof BlockPlaceA) {
            BlockPlaceA typed = (BlockPlaceA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, typed, "onAsyncBlockBreak",
                    event -> typed.onAsyncBlockBreak((LACAsyncPlayerPlaceBlockEvent) event));
            LACEventBus.register(LACEventType.PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, typed, "onBlockBreak",
                    event -> typed.onBlockBreak((LACPlayerPlaceBlockEvent) event));
        } else if (listener instanceof BlockPlaceB) {
            BlockPlaceB typed = (BlockPlaceB) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, typed, "onAsyncBlockPlace",
                    event -> typed.onAsyncBlockPlace((LACAsyncPlayerPlaceBlockEvent) event));
        } else if (listener instanceof FastBreakA) {
            FastBreakA typed = (FastBreakA) listener;
            LACEventBus.register(LACEventType.PLAYER_BREAK_BLOCK, LACEventPriority.NORMAL, typed, "onBlockBreak",
                    event -> typed.onBlockBreak((LACPlayerBreakBlockEvent) event));
            LACEventBus.register(LACEventType.PLAYER_BREAK_BLOCK, LACEventPriority.LOW, typed, "beforeBlockBreak",
                    event -> typed.beforeBlockBreak((LACPlayerBreakBlockEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "onMovement",
                    event -> typed.onMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof FastPlaceA) {
            FastPlaceA typed = (FastPlaceA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, typed, "onAsyncBlockPlace",
                    event -> typed.onAsyncBlockPlace((LACAsyncPlayerPlaceBlockEvent) event));
            LACEventBus.register(LACEventType.PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, typed, "onBlockPlace",
                    event -> typed.onBlockPlace((LACPlayerPlaceBlockEvent) event));
        } else if (listener instanceof GhostBreakA) {
            GhostBreakA typed = (GhostBreakA) listener;
            LACEventBus.register(LACEventType.PLAYER_BREAK_BLOCK, LACEventPriority.NORMAL, typed, "onBlockBreak",
                    event -> typed.onBlockBreak((LACPlayerBreakBlockEvent) event));
        } else if (listener instanceof ScaffoldA) {
            ScaffoldA typed = (ScaffoldA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, typed, "onAsyncBlockPlace",
                    event -> typed.onAsyncBlockPlace((LACAsyncPlayerPlaceBlockEvent) event));
        } else if (listener instanceof ScaffoldB) {
            ScaffoldB typed = (ScaffoldB) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, typed, "onAsyncBlockPlace",
                    event -> typed.onAsyncBlockPlace((LACAsyncPlayerPlaceBlockEvent) event));
        } else if (listener instanceof BoatA) {
            BoatA typed = (BoatA) listener;
            LACEventBus.register(LACEventType.PLAYER_MOVE, LACEventPriority.NORMAL, typed, "boatFlight",
                    event -> typed.boatFlight((LACPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.PLAYER_MOVE, LACEventPriority.NORMAL, typed, "boatSpeed",
                    event -> typed.boatSpeed((LACPlayerMoveEvent) event));
        } else if (listener instanceof ElytraA) {
            ElytraA typed = (ElytraA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "theSameSpeed",
                    event -> typed.theSameSpeed((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "tooLowSpeed",
                    event -> typed.tooLowSpeed((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof ElytraB) {
            ElytraB typed = (ElytraB) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof ElytraC) {
            ElytraC typed = (ElytraC) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof FastClimbA) {
            FastClimbA typed = (FastClimbA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof FlightA) {
            FlightA typed = (FlightA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, typed, "scaffoldAsyncBlockPlace",
                    event -> typed.scaffoldAsyncBlockPlace((LACAsyncPlayerPlaceBlockEvent) event));
        } else if (listener instanceof FlightB) {
            FlightB typed = (FlightB) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, typed, "scaffoldAsyncBlockPlace",
                    event -> typed.scaffoldAsyncBlockPlace((LACAsyncPlayerPlaceBlockEvent) event));
        } else if (listener instanceof FlightC) {
            FlightC typed = (FlightC) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, typed, "scaffoldAsyncBlockPlace",
                    event -> typed.scaffoldAsyncBlockPlace((LACAsyncPlayerPlaceBlockEvent) event));
        } else if (listener instanceof JumpA) {
            JumpA typed = (JumpA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof JumpB) {
            JumpB typed = (JumpB) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof LiquidWalkA) {
            LiquidWalkA typed = (LiquidWalkA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof LiquidWalkB) {
            LiquidWalkB typed = (LiquidWalkB) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof NoFallA) {
            NoFallA typed = (NoFallA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_BREAK_BLOCK, LACEventPriority.NORMAL, typed, "scaffoldBlockBreak",
                    event -> typed.scaffoldBlockBreak((LACAsyncPlayerBreakBlockEvent) event));
        } else if (listener instanceof NoFallB) {
            NoFallB typed = (NoFallB) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof NoSlowA) {
            NoSlowA typed = (NoSlowA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof SpeedA) {
            SpeedA typed = (SpeedA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "totalHorizontal",
                    event -> typed.totalHorizontal((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "airHorizontal",
                    event -> typed.airHorizontal((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeAirMovement",
                    event -> typed.beforeAirMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof SpeedB) {
            SpeedB typed = (SpeedB) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof SpeedC) {
            SpeedC typed = (SpeedC) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof SpeedD) {
            SpeedD typed = (SpeedD) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof SpeedE) {
            SpeedE typed = (SpeedE) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.HIGH, typed, "afterMovement",
                    event -> typed.afterMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onTeleportHorizontal",
                    event -> typed.onTeleportHorizontal((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onTeleportVertical",
                    event -> typed.onTeleportVertical((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onHorizontal",
                    event -> typed.onHorizontal((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onVertical",
                    event -> typed.onVertical((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof SpeedF) {
            SpeedF typed = (SpeedF) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof StepA) {
            StepA typed = (StepA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof TridentA) {
            TridentA typed = (TridentA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "beforeMovement",
                    event -> typed.beforeMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof VehicleA) {
            VehicleA typed = (VehicleA) listener;
            LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, typed, "vehicleSpeedAndFlight",
                    event -> typed.vehicleSpeedAndFlight((LACAsyncPacketReceiveEvent) event));
        } else if (listener instanceof BadPacketsA) {
            BadPacketsA typed = (BadPacketsA) listener;
            LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, typed, "onAsyncPacketReceive",
                    event -> typed.onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
        } else if (listener instanceof BadPacketsB) {
            BadPacketsB typed = (BadPacketsB) listener;
            LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, typed, "onAsyncPacketReceive",
                    event -> typed.onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
        } else if (listener instanceof BadPacketsC) {
            BadPacketsC typed = (BadPacketsC) listener;
            LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, typed, "onAsyncPacketReceive",
                    event -> typed.onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
        } else if (listener instanceof BadPacketsD) {
            BadPacketsD typed = (BadPacketsD) listener;
            LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, typed, "onAsyncPacketReceive",
                    event -> typed.onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
        } else if (listener instanceof MorePacketsA) {
            MorePacketsA typed = (MorePacketsA) listener;
            LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, typed, "onAsyncPacketReceiveA",
                    event -> typed.onAsyncPacketReceiveA((LACAsyncPacketReceiveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, typed, "onAsyncPacketReceiveB",
                    event -> typed.onAsyncPacketReceiveB((LACAsyncPacketReceiveEvent) event));
        } else if (listener instanceof MorePacketsB) {
            MorePacketsB typed = (MorePacketsB) listener;
            LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, typed, "onAsyncPacketReceive",
                    event -> typed.onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
        } else if (listener instanceof TimerA) {
            TimerA typed = (TimerA) listener;
            LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, typed, "onAsyncPacketReceive",
                    event -> typed.onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof TimerB) {
            TimerB typed = (TimerB) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovementA",
                    event -> typed.onAsyncMovementA((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovementB",
                    event -> typed.onAsyncMovementB((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovementC",
                    event -> typed.onAsyncMovementC((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovementD",
                    event -> typed.onAsyncMovementD((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovementE",
                    event -> typed.onAsyncMovementE((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovementF",
                    event -> typed.onAsyncMovementF((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof AutoBotA) {
            AutoBotA typed = (AutoBotA) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onHeadRotation",
                    event -> typed.onHeadRotation((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onMovement",
                    event -> typed.onMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof SkinBlinkerA) {
            SkinBlinkerA typed = (SkinBlinkerA) listener;
            LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, typed, "onAsyncPacketReceive",
                    event -> typed.onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onMovement",
                    event -> typed.onMovement((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof InvalidPingListener) {
            InvalidPingListener typed = (InvalidPingListener) listener;
            LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, typed, "onAsyncPacketReceive",
                    event -> typed.onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
        } else if (listener instanceof UnloadedChunkListener) {
            UnloadedChunkListener typed = (UnloadedChunkListener) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onMovement",
                    event -> typed.onMovement((LACPlayerMoveEvent) event));
        } else if (listener instanceof LACPlayerListener) {
            LACPlayerListener typed = (LACPlayerListener) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOWEST, typed, "eventHistory",
                    event -> typed.eventHistory((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.LOWEST, typed, "packetHistory",
                    event -> typed.packetHistory((LACAsyncPacketReceiveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOWEST, typed, "activePotionEffects",
                    event -> typed.activePotionEffects((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOWEST, typed, "entitiesAsync",
                    event -> typed.entitiesAsync((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.PLAYER_MOVE, LACEventPriority.LOWEST, typed, "lastGlidingRiptidingFlight",
                    event -> typed.lastGlidingRiptidingFlight((LACPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.PLAYER_MOVE, LACEventPriority.LOWEST, typed, "lastInsideVehicle",
                    event -> typed.lastInsideVehicle((LACPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOW, typed, "lastVelocityChangeNotGround",
                    event -> typed.lastVelocityChangeNotGround((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.PLAYER_PLACE_BLOCK, LACEventPriority.HIGH, typed, "lastBlockPlace",
                    event -> typed.lastBlockPlace((LACPlayerPlaceBlockEvent) event));
            LACEventBus.register(LACEventType.PLAYER_BREAK_BLOCK, LACEventPriority.HIGH, typed, "lastBlockBreak",
                    event -> typed.lastBlockBreak((LACPlayerBreakBlockEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.LOWEST, typed, "lastSwing",
                    event -> typed.lastSwing((LACAsyncPacketReceiveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOWEST, typed, "lastPowderSnowWalk",
                    event -> typed.lastPowderSnowWalk((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOWEST, typed, "lastSlimeHoneyBlock",
                    event -> typed.lastSlimeHoneyBlock((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOWEST, typed, "stopLongBypassAfterRedirection",
                    event -> typed.stopLongBypassAfterRedirection((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOWEST, typed, "lastBlockExplosion",
                    event -> typed.lastBlockExplosion((LACAsyncPlayerMoveEvent) event));
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.LOWEST, typed, "lastInWater",
                    event -> typed.lastInWater((LACAsyncPlayerMoveEvent) event));
        } else if (listener instanceof ConnectionStabilityListener) {
            ConnectionStabilityListener typed = (ConnectionStabilityListener) listener;
            LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, typed, "onAsyncMovement",
                    event -> typed.onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        }
    }

}
