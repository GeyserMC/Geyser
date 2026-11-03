/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.registry.mappings;

import com.sun.net.httpserver.HttpServer;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomMappingsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomSkullsEvent;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.event.type.GeyserDefineCustomMappingsEventImpl;
import org.geysermc.geyser.scoreboard.network.util.GeyserMockContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class MappingsConfigReaderTest {
    private static final String INVALID = "{ \"format_version\": 1, \"skulls\": {";

    @TempDir
    Path configFolder;

    @AfterEach
    void clearRegisteredFiles() {
        withGeyser(MappingsConfigReader::defineCustomMappings);
    }

    @Test
    void registeredFileIsRead() throws URISyntaxException {
        Path file = skullsResource();
        Map<GeyserDefineCustomSkullsEvent.SkullTextureType, List<String>> map = new EnumMap<>(GeyserDefineCustomSkullsEvent.SkullTextureType.class);

        withGeyser(geyser -> {
            geyser.eventBus().subscribe(geyser, GeyserDefineCustomMappingsEvent.class, event -> event.register(file));
            MappingsConfigReader.defineCustomMappings(geyser);
            MappingsConfigReader.loadCustomMappingsFromJson(MappingsType.SKULLS, map::put);
        });

        assertEquals("GeyserMC", map.get(GeyserDefineCustomSkullsEvent.SkullTextureType.USERNAME).getFirst());
    }

    @Test
    void downloadKeepsTheLastValidCopy() throws IOException, URISyntaxException {
        String valid = Files.readString(skullsResource());
        AtomicReference<String> body = new AtomicReference<>(valid);
        AtomicInteger status = new AtomicInteger(200);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();

        try {
            withGeyser(geyser -> {
                GeyserDefineCustomMappingsEventImpl event = new GeyserDefineCustomMappingsEventImpl();
                assertTrue(event.registerUrl(base + "/skulls.json"));

                body.set(INVALID);
                assertTrue(event.registerUrl(base + "/skulls.json"));
                status.set(404);
                assertTrue(event.registerUrl(base + "/skulls.json"));
                assertFalse(event.registerUrl(base + "/missing.json"));

                assertEquals(3, event.mappingsFiles().size());
                for (Path file : event.mappingsFiles()) {
                    assertEquals(valid, readString(file));
                }
            });
        } finally {
            server.stop(0);
        }
    }

    private void withGeyser(Consumer<GeyserImpl> test) {
        GeyserMockContext.mockContext(context -> {
            GeyserImpl geyser = context.mockOrSpy(GeyserImpl.class);
            GeyserBootstrap bootstrap = context.mock(GeyserBootstrap.class);
            when(bootstrap.getConfigFolder()).thenReturn(configFolder);
            when(geyser.getBootstrap()).thenReturn(bootstrap);
            when(geyser.platformType()).thenReturn(PlatformType.STANDALONE);
            test.accept(geyser);
        });
    }

    private Path skullsResource() throws URISyntaxException {
        return Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource("configuration/custom-skulls.json")).toURI());
    }

    private static String readString(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
