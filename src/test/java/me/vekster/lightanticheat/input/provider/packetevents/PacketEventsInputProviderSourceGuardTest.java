package me.vekster.lightanticheat.input.provider.packetevents;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class PacketEventsInputProviderSourceGuardTest {

    private static String providerSource;
    private static String mainSource;
    private static String closeBlock;

    @BeforeAll
    static void loadSources() throws IOException {
        providerSource = readFile("src/main/java/me/vekster/lightanticheat/input/provider/packetevents/PacketEventsInputProvider.java");
        mainSource = readFile("src/main/java/me/vekster/lightanticheat/Main.java");

        int closeIndex = providerSource.indexOf("void close()");
        if (closeIndex >= 0) {
            closeBlock = providerSource.substring(closeIndex);
            // limit to next 800 chars or to next method/class boundary to isolate close() body
            // keep full remainder to ensure PacketEvents.getAPI() check is strict within close
        } else {
            closeBlock = "";
        }
    }

    private static String readFile(String relative) throws IOException {
        Path p = Paths.get(relative);
        if (!Files.exists(p)) {
            // fallback: try project root via user.dir
            Path alt = Paths.get(System.getProperty("user.dir"), relative);
            if (Files.exists(alt)) {
                p = alt;
            }
        }
        if (!Files.exists(p)) {
            fail("Required source file not found: " + relative + " (resolved: " + p.toAbsolutePath() + ")");
        }
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    @Test
    void usesPacketEventsGetAPI() {
        assertTrue(providerSource.contains("PacketEvents.getAPI()"),
                "PacketEventsInputProvider must call PacketEvents.getAPI()");
    }

    @Test
    void typesAndStoresPacketEventsAPI() {
        assertTrue(providerSource.contains("PacketEventsAPI"),
                "PacketEventsInputProvider must type/store PacketEventsAPI");
        assertTrue(providerSource.contains("PacketEventsAPI<?>"),
                "PacketEventsInputProvider must use PacketEventsAPI<?> generic");
    }

    @Test
    void checksIsLoaded() {
        assertTrue(providerSource.contains("isLoaded()"),
                "PacketEventsInputProvider must check api.isLoaded()");
    }

    @Test
    void checksIsInitialized() {
        assertTrue(providerSource.contains("isInitialized()"),
                "PacketEventsInputProvider must check api.isInitialized()");
    }

    @Test
    void checksIsTerminated() {
        assertTrue(providerSource.contains("isTerminated()"),
                "PacketEventsInputProvider must check api.isTerminated()");
    }

    @Test
    void storesRegisteredApi() {
        assertTrue(providerSource.contains("registeredApi"),
                "PacketEventsInputProvider must store registeredApi field");
        assertTrue(providerSource.contains("private volatile PacketEventsAPI"),
                "PacketEventsInputProvider must declare 'private volatile PacketEventsAPI<?> registeredApi'");
        assertTrue(providerSource.contains("registeredApi = api"),
                "PacketEventsInputProvider must assign registeredApi = api on success");
        assertTrue(providerSource.contains("registeredApi = null"),
                "PacketEventsInputProvider must clear registeredApi on failure/close");
    }

    @Test
    void unregistersThroughStoredApiNotViaStaticGetAPIInClose() {
        // must unregister via stored API
        assertTrue(providerSource.contains("registeredApi.getEventManager().unregisterListener")
                        || providerSource.contains("api.getEventManager().unregisterListener"),
                "PacketEventsInputProvider must unregister through stored API (registeredApi/api.getEventManager().unregisterListener)");
        // close() must use stored api, not PacketEvents.getAPI()
        assertFalse(closeBlock.contains("PacketEvents.getAPI()"),
                "PacketEventsInputProvider.close() must NOT call PacketEvents.getAPI(); must unregister through stored registeredApi");
        // close copies registeredApi to local then nulls field before unregister
        assertTrue(closeBlock.contains("registeredApi"),
                "close() must reference registeredApi");
        assertTrue(closeBlock.contains("unregisterListener"),
                "close() must call unregisterListener");
    }

    @Test
    void acceptsBothPluginNameCasings() {
        assertTrue(providerSource.contains("\"packetevents\""),
                "findPacketEventsPlugin must check \"packetevents\" (lowercase)");
        assertTrue(providerSource.contains("\"PacketEvents\""),
                "findPacketEventsPlugin must check \"PacketEvents\" (capitalized)");
        assertTrue(providerSource.contains("findPacketEventsPlugin"),
                "PacketEventsInputProvider must contain findPacketEventsPlugin helper");
    }

    @Test
    void doesNotUseClassForNamePacketEvents() {
        assertFalse(providerSource.contains("Class.forName(\"com.github.retrooper.packetevents.PacketEvents\")"),
                "PacketEventsInputProvider must NOT use Class.forName(\"com.github.retrooper.packetevents.PacketEvents\")");
        assertFalse(providerSource.contains("Class.forName(\"com.github.retrooper.packetevents"),
                "PacketEventsInputProvider must NOT use Class.forName for packetevents");
    }

    @Test
    void doesNotUseReflectionGetMethod() {
        assertFalse(providerSource.contains("api.getClass().getMethod"),
                "PacketEventsInputProvider must NOT use api.getClass().getMethod reflection");
        assertFalse(providerSource.contains(".getMethod("),
                "PacketEventsInputProvider must NOT use reflection getMethod");
    }

    @Test
    void doesNotCallPacketEventsSetAPI() {
        assertFalse(providerSource.contains("PacketEvents.setAPI"),
                "PacketEventsInputProvider must NOT call PacketEvents.setAPI");
    }

    @Test
    void doesNotCallLoadInitTerminateLifecycle() {
        assertFalse(providerSource.contains(".load()"),
                "PacketEventsInputProvider must NOT call .load()");
        assertFalse(providerSource.contains(".init()"),
                "PacketEventsInputProvider must NOT call .init()");
        assertFalse(providerSource.contains(".terminate()"),
                "PacketEventsInputProvider must NOT call .terminate() (isTerminated() is allowed)");
    }

    @Test
    void mainDoesNotContainPacketEventsImportOrFQN() {
        assertFalse(mainSource.contains("PacketEvents"),
                "Main.java must NOT contain PacketEvents import or FQN");
        assertFalse(mainSource.contains("com.github.retrooper.packetevents"),
                "Main.java must NOT contain packetevents FQN");
        assertFalse(mainSource.contains("import com.github.retrooper"),
                "Main.java must NOT import packetevents");
    }

    @Test
    void mainDoesNotContainIsPacketEventsAvailable() {
        assertFalse(mainSource.contains("isPacketEventsAvailable"),
                "Main.java must NOT contain isPacketEventsAvailable");
        assertFalse(mainSource.contains("isPacketAvailable"),
                "Main.java must NOT contain isPacketAvailable helper");
    }
}
