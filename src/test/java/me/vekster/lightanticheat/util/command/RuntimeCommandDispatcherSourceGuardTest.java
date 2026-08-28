package me.vekster.lightanticheat.util.command;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RuntimeCommandDispatcherSourceGuardTest {

    private static final String DISPATCHER_REL = "src/main/java/me/vekster/lightanticheat/util/command/RuntimeCommandDispatcher.java";
    private static final String HANDLER_REL = "src/main/java/me/vekster/lightanticheat/util/violation/ViolationHandler.java";
    private static final String SCHEDULER_REL = "src/main/java/me/vekster/lightanticheat/util/scheduler/Scheduler.java";
    private static final String FOLIA_REL = "src/main/java/me/vekster/lightanticheat/util/scheduler/gamescheduler/FoliaScheduler.java";
    private static final String BUKKIT_REL = "src/main/java/me/vekster/lightanticheat/util/scheduler/gamescheduler/BukkitScheduler.java";

    private static Path projectRoot() {
        Path cur = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 8; i++) {
            if (Files.exists(cur.resolve("pom.xml")) && Files.exists(cur.resolve(DISPATCHER_REL))) {
                return cur;
            }
            Path parent = cur.getParent();
            if (parent == null) {
                break;
            }
            cur = parent;
        }
        // fallback to absolute cwd which is project root when running via Maven surefire
        return Paths.get("").toAbsolutePath();
    }

    private static String readSource(final String relative) throws IOException {
        final Path path = projectRoot().resolve(relative);
        // Java 8 compatible standard file read; equivalent to java.nio.file.Files.readString on Java 11+
        // uses java.nio.file.Files.readAllBytes
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    // ---- Dispatcher isolation ----

    @Test
    @DisplayName("dispatcher: contains Scheduler.globalThread")
    public void dispatcherContainsGlobalThread() throws IOException {
        final String src = readSource(DISPATCHER_REL);
        Assertions.assertTrue(src.contains("Scheduler.globalThread"),
                "RuntimeCommandDispatcher must delegate to Scheduler.globalThread");
    }

    @Test
    @DisplayName("dispatcher: contains Bukkit.dispatchCommand")
    public void dispatcherContainsBukkitDispatchCommand() throws IOException {
        final String src = readSource(DISPATCHER_REL);
        Assertions.assertTrue(src.contains("Bukkit.dispatchCommand"),
                "RuntimeCommandDispatcher must contain Bukkit.dispatchCommand");
    }

    @Test
    @DisplayName("dispatcher: contains catch (CommandException")
    public void dispatcherContainsCatchCommandException() throws IOException {
        final String src = readSource(DISPATCHER_REL);
        Assertions.assertTrue(src.contains("catch (CommandException"),
                "RuntimeCommandDispatcher must catch CommandException");
    }

    @Test
    @DisplayName("dispatcher: contains boolean result handling for dispatchCommand")
    public void dispatcherContainsBooleanResultHandling() throws IOException {
        final String src = readSource(DISPATCHER_REL);
        Assertions.assertTrue(src.contains("dispatchCommand"),
                "must contain dispatchCommand");
        final boolean hasFalse = src.contains("false");
        final boolean hasWasNotHandled = src.contains("was not handled");
        Assertions.assertTrue(hasFalse || hasWasNotHandled,
                "must contain boolean result handling: 'false' or 'was not handled' alongside dispatchCommand");
        // also ensure explicit boolean handled variable or !handled check exists
        final boolean hasHandled = src.contains("handled");
        Assertions.assertTrue(hasHandled,
                "must contain boolean result handling variable 'handled'");
    }

    @Test
    @DisplayName("dispatcher: does NOT contain forbidden types and handlers")
    public void dispatcherDoesNotContainForbidden() throws IOException {
        final String src = readSource(DISPATCHER_REL);
        Assertions.assertFalse(src.contains("org.bukkit.entity.Player"),
                "must NOT contain org.bukkit.entity.Player");
        Assertions.assertFalse(src.contains("LACPlayer"),
                "must NOT contain LACPlayer");
        Assertions.assertFalse(src.contains("LACPunishmentEvent"),
                "must NOT contain LACPunishmentEvent");
        Assertions.assertFalse(src.contains("PlaceholderConvertor"),
                "must NOT contain PlaceholderConvertor");
        Assertions.assertFalse(src.contains("swapAll"),
                "must NOT contain swapAll");
        Assertions.assertFalse(src.contains("renderPunishmentCommand"),
                "must NOT contain renderPunishmentCommand");
        Assertions.assertFalse(src.contains("catch (Exception"),
                "must NOT contain catch (Exception");
        Assertions.assertFalse(src.contains("catch (Throwable"),
                "must NOT contain catch (Throwable");
    }

    // ---- Handler custody ----

    @Test
    @DisplayName("handler: contains renderPunishmentCommand and dispatchConsoleBatch")
    public void handlerContainsRenderAndDispatch() throws IOException {
        final String src = readSource(HANDLER_REL);
        Assertions.assertTrue(src.contains("renderPunishmentCommand"),
                "ViolationHandler must contain renderPunishmentCommand");
        Assertions.assertTrue(src.contains("RuntimeCommandDispatcher.dispatchConsoleBatch"),
                "ViolationHandler must contain RuntimeCommandDispatcher.dispatchConsoleBatch");
    }

    @Test
    @DisplayName("handler: does NOT contain Bukkit.dispatchCommand and Scheduler.runTask(false")
    public void handlerDoesNotContainDirectDispatchOrSchedulerRunTaskFalse() throws IOException {
        final String src = readSource(HANDLER_REL);
        Assertions.assertFalse(src.contains("Bukkit.dispatchCommand"),
                "ViolationHandler must NOT contain Bukkit.dispatchCommand");
        Assertions.assertFalse(src.contains("Scheduler.runTask(false"),
                "ViolationHandler must NOT contain Scheduler.runTask(false");
    }

    @Test
    @DisplayName("handler: render before dispatch before violations reset ordering")
    public void handlerOrdering() throws IOException {
        final String src = readSource(HANDLER_REL);
        final int idxRender = src.indexOf("renderPunishmentCommand");
        final int idxDispatch = src.indexOf("dispatchConsoleBatch");
        final int idxReset = src.indexOf("lacPlayer.violations = new PlayerViolations()");
        Assertions.assertTrue(idxRender >= 0, "renderPunishmentCommand must be present");
        Assertions.assertTrue(idxDispatch >= 0, "dispatchConsoleBatch must be present");
        Assertions.assertTrue(idxReset >= 0, "lacPlayer.violations = new PlayerViolations() must be present");
        Assertions.assertTrue(idxRender < idxDispatch,
                "renderPunishmentCommand must appear before dispatchConsoleBatch");
        Assertions.assertTrue(idxDispatch < idxReset,
                "dispatchConsoleBatch must appear before lacPlayer.violations = new PlayerViolations()");
    }

    // ---- Scheduler mapping ----

    @Test
    @DisplayName("scheduler: globalThread delegates to SCHEDULER.runTask(false, task)")
    public void schedulerGlobalThreadDelegation() throws IOException {
        final String src = readSource(SCHEDULER_REL);
        Assertions.assertTrue(src.contains("globalThread"),
                "Scheduler.java must contain globalThread");
        Assertions.assertTrue(src.contains("SCHEDULER.runTask(false, task)"),
                "Scheduler.globalThread must delegate to SCHEDULER.runTask(false, task)");
    }

    @Test
    @DisplayName("folia scheduler: false runTask branch calls FoliaUtil.runTask")
    public void foliaSchedulerDelegation() throws IOException {
        final String src = readSource(FOLIA_REL);
        Assertions.assertTrue(src.contains("FoliaUtil.runTask"),
                "FoliaScheduler false runTask branch must call FoliaUtil.runTask");
        // ensure the runTask(boolean, Runnable) method exists
        Assertions.assertTrue(src.contains("runTask(boolean"),
                "FoliaScheduler must contain runTask(boolean, Runnable)");
    }

    @Test
    @DisplayName("bukkit scheduler: runTask calls Bukkit.getScheduler().runTask")
    public void bukkitSchedulerDelegation() throws IOException {
        final String src = readSource(BUKKIT_REL);
        Assertions.assertTrue(src.contains("Bukkit.getScheduler().runTask"),
                "BukkitScheduler runTask must call Bukkit.getScheduler().runTask");
    }
}
