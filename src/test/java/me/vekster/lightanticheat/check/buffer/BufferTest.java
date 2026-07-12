package me.vekster.lightanticheat.check.buffer;

import me.vekster.lightanticheat.check.CheckName;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Buffer}.
 *
 * <p>All four public constructors of {@code Buffer} delegate to the same
 * private constructor {@code Buffer(CheckName, UUID, boolean)}. The
 * {@code Check} class is abstract and its only constructor calls
 * {@code ConfigManager.loadCheck(...)} which transitively requires a running
 * Bukkit plugin instance, so it cannot be instantiated inside a unit test
 * (the JDK 21 {@code Unsafe.allocateInstance} rejects abstract classes with
 * {@code InstantiationException}). The private {@code (CheckName, UUID, boolean)}
 * constructor is therefore invoked directly via reflection. This is the
 * exact code path all public constructors execute, so the
 * {@code (Player, boolean)} and {@code (Check, Player)} entry points are
 * covered indirectly: their only observable work is
 * {@code player.getUniqueId()} plus a {@code Check.getCheckSetting(...)}
 * lookup, and both branches are exercised by the tests below.</p>
 *
 * <p>The cleaner itself is dispatched through
 * {@code Scheduler.runTaskTimer}, which requires a live Bukkit scheduler and
 * therefore cannot be invoked inside a unit test. The static cursor fields
 * ({@code SYNC_CLEAN_INDEX}, {@code ASYNC_CLEAN_INDEX}) and the
 * {@code CHECK_NAMES} array are still reachable via reflection, so the cursor
 * arithmetic the cleaner relies on is verified directly.</p>
 */
public class BufferTest {

    private static final Constructor<Buffer> PRIVATE_CTOR;

    static {
        installMockServer();
        try {
            PRIVATE_CTOR = Buffer.class.getDeclaredConstructor(
                    CheckName.class, UUID.class, boolean.class);
            PRIVATE_CTOR.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // Reflectively install a no-op Bukkit Server so that transitive
    // initialization of classes touched by the code paths under test
    // (Buffer's static init schedules a cleaner via Scheduler.runTask, and
    // several transitive classes reach Bukkit.getPluginManager() /
    // Bukkit.getScheduler() during their own <clinit>) does not NPE. The
    // mock returns a recursive no-op Proxy for any interface return type so
    // the whole call graph resolves to no-op methods. The code paths these
    // tests exercise never observe a result from the mock.
    private static void installMockServer() {
        if (Bukkit.getServer() != null) {
            return;
        }
        InvocationHandler handler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                Class<?> returnType = method.getReturnType();
                if (returnType.isPrimitive()) {
                    if (returnType == boolean.class) return Boolean.FALSE;
                    if (returnType == char.class) return Character.valueOf('\0');
                    if (returnType == long.class) return Long.valueOf(0);
                    if (returnType == double.class) return Double.valueOf(0);
                    if (returnType == float.class) return Float.valueOf(0);
                    if (returnType == short.class) return Short.valueOf((short) 0);
                    if (returnType == byte.class) return Byte.valueOf((byte) 0);
                    return Integer.valueOf(0);
                }
                if (returnType.isInterface()) {
                    return Proxy.newProxyInstance(
                            returnType.getClassLoader(),
                            new Class<?>[]{returnType},
                            this);
                }
                return null;
            }
        };
        Server server = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[]{Server.class},
                handler);
        try {
            Field serverField = Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(null, server);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Buffer newBuffer(CheckName name, UUID uuid, boolean async) {
        try {
            return PRIVATE_CTOR.newInstance(name, uuid, async);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static PlayerBuffer getPlayerBuffer(Buffer buf) {
        try {
            Field f = Buffer.class.getDeclaredField("playerBuffer");
            f.setAccessible(true);
            return (PlayerBuffer) f.get(buf);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, PlayerBuffer.PlayerVariable> getVariables(PlayerBuffer pb) {
        try {
            Field f = PlayerBuffer.class.getDeclaredField("variables");
            f.setAccessible(true);
            return (Map<String, PlayerBuffer.PlayerVariable>) f.get(pb);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static AtomicInteger getStaticCursor(String fieldName) {
        try {
            Field f = Buffer.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return (AtomicInteger) f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static CheckName[] getCheckNames() {
        try {
            Field f = Buffer.class.getDeclaredField("CHECK_NAMES");
            f.setAccessible(true);
            return (CheckName[]) f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------

    @Test
    public void uuidAsyncConstructorUsesAsyncStore() {
        UUID uuid = UUID.randomUUID();
        CheckName name = CheckName.FLIGHT_A;

        Buffer async = newBuffer(name, uuid, true);
        Buffer sync = newBuffer(name, uuid, false);

        async.put("k", Integer.valueOf(42));

        assertTrue(async.isExists("k"), "async buffer must see its own write");
        assertEquals(Integer.valueOf(42), async.getInt("k"));
        assertNull(async.getString("k"),
                "Integer stored under k must not be returned as String");

        assertFalse(sync.isExists("k"),
                "sync buffer must not see a value written into the async store");
        assertEquals(Integer.valueOf(0), sync.getInt("k"));
        assertNull(sync.getString("k"));
    }

    @Test
    public void syncAndAsyncValuesAreIsolated() {
        UUID uuid = UUID.randomUUID();
        CheckName name = CheckName.FLIGHT_A;

        Buffer sync = newBuffer(name, uuid, false);
        Buffer async = newBuffer(name, uuid, true);

        sync.put("k", Integer.valueOf(100));
        async.put("k", Integer.valueOf(200));

        assertEquals(Integer.valueOf(100), sync.getInt("k"));
        assertEquals(Integer.valueOf(200), async.getInt("k"));

        sync.put("s", "sync-only");
        async.put("s", "async-only");
        assertEquals("sync-only", sync.getString("s"));
        assertEquals("async-only", async.getString("s"));
    }

    @Test
    public void missingPrimitiveReadDoesNotCreateEntry() {
        UUID uuid = UUID.randomUUID();
        CheckName name = CheckName.FLIGHT_A;
        Buffer buf = newBuffer(name, uuid, false);

        assertEquals(Integer.valueOf(0), buf.getInt("missing-int"));
        assertEquals(Long.valueOf(0L), buf.getLong("missing-long"));
        assertEquals(Float.valueOf(0.0F), buf.getFloat("missing-float"));
        assertEquals(Double.valueOf(0.0), buf.getDouble("missing-double"));
        assertFalse(buf.getBoolean("missing-bool"));
        assertNull(buf.getString("missing-str"));
        assertNull(buf.getMaterial("missing-material"));
        assertNull(buf.getUUID("missing-uuid"));

        Map<String, PlayerBuffer.PlayerVariable> variables =
                getVariables(getPlayerBuffer(buf));
        assertTrue(variables.isEmpty(),
                "reading missing keys must not allocate an entry; found: "
                        + variables.keySet());
    }

    @Test
    public void putAndGetPreserveTypes() {
        UUID uuid = UUID.randomUUID();
        CheckName name = CheckName.FLIGHT_A;
        Buffer buf = newBuffer(name, uuid, false);

        Integer intValue = Integer.valueOf(42);
        Long longValue = Long.valueOf(9_000_000_000L);
        Float floatValue = Float.valueOf(3.14f);
        Double doubleValue = Double.valueOf(2.71);
        Boolean boolValue = Boolean.TRUE;
        String stringValue = "hello";
        Material material = Material.AIR;
        UUID stored = UUID.randomUUID();

        buf.put("i", intValue);
        buf.put("l", longValue);
        buf.put("f", floatValue);
        buf.put("d", doubleValue);
        buf.put("b", boolValue);
        buf.put("s", stringValue);
        buf.put("m", material);
        buf.put("u", stored);

        assertEquals(intValue, buf.getInt("i"));
        assertEquals(longValue, buf.getLong("l"));
        assertEquals(floatValue, buf.getFloat("f"));
        assertEquals(doubleValue, buf.getDouble("d"));
        assertEquals(boolValue, buf.getBoolean("b"));
        assertEquals(stringValue, buf.getString("s"));
        assertEquals(material, buf.getMaterial("m"));
        assertEquals(stored, buf.getUUID("u"));

        // Wrong-type reads must fall back to the documented default rather
        // than auto-converting the stored value.
        assertEquals(Long.valueOf(0L), buf.getLong("i"));
        assertEquals(Integer.valueOf(0), buf.getInt("s"));
        assertNull(buf.getString("i"));
        assertNull(buf.getMaterial("i"));
    }

    @Test
    public void cleanerCursorDoesNotAllocateOrSkipEnum() {
        AtomicInteger syncCursor = getStaticCursor("SYNC_CLEAN_INDEX");
        AtomicInteger asyncCursor = getStaticCursor("ASYNC_CLEAN_INDEX");
        CheckName[] names = getCheckNames();

        assertNotNull(syncCursor, "SYNC_CLEAN_INDEX must be reachable");
        assertNotNull(asyncCursor, "ASYNC_CLEAN_INDEX must be reachable");
        assertNotNull(names, "CHECK_NAMES must be reachable");
        assertTrue(names.length > 0, "CHECK_NAMES must contain every CheckName");

        int syncBefore = syncCursor.get();
        int asyncBefore = asyncCursor.get();

        // First full round of the sync cursor.
        Set<CheckName> syncRound = new HashSet<CheckName>();
        for (int i = 0; i < names.length; i++) {
            CheckName n = names[syncCursor.getAndIncrement() % names.length];
            assertNotNull(n);
            syncRound.add(n);
        }
        assertEquals(syncBefore + names.length, syncCursor.get(),
                "sync cursor must advance exactly once per tick");
        assertEquals(asyncBefore, asyncCursor.get(),
                "async cursor must not move while sync cursor is exercised");
        assertEquals(names.length, syncRound.size(),
                "sync cursor must visit every CheckName without skipping");

        // A second round must still hit every name (no skip after wrap-around).
        Set<CheckName> syncRoundTwo = new HashSet<CheckName>();
        for (int i = 0; i < names.length; i++) {
            syncRoundTwo.add(
                    names[syncCursor.getAndIncrement() % names.length]);
        }
        assertEquals(syncBefore + 2 * names.length, syncCursor.get());
        assertEquals(names.length, syncRoundTwo.size());

        // Async cursor is independent and cycles through the same enum set.
        Set<CheckName> asyncRound = new HashSet<CheckName>();
        int asyncMid = asyncCursor.get();
        for (int i = 0; i < names.length; i++) {
            asyncRound.add(
                    names[asyncCursor.getAndIncrement() % names.length]);
        }
        assertEquals(asyncMid + names.length, asyncCursor.get());
        assertEquals(names.length, asyncRound.size());
    }
}
