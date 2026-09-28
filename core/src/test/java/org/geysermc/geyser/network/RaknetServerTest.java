/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network;

import com.sun.net.httpserver.HttpServer;
import org.geysermc.geyser.GeyserImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class RaknetServerTest {
    private static final InetSocketAddress FIRST_PROXY = new InetSocketAddress("198.51.100.7", 19132);
    private static final InetSocketAddress SECOND_PROXY = new InetSocketAddress("198.51.100.8", 19132);
    private static final InetSocketAddress CLIENT = new InetSocketAddress("203.0.113.10", 19132);

    private final AtomicInteger fetches = new AtomicInteger();
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> addresses = new AtomicReference<>("198.51.100.7\n");
    private HttpServer lists;
    private GeyserImpl geyser;

    @BeforeEach
    void setUp() throws Exception {
        lists = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        lists.createContext("/proxies", exchange -> {
            fetches.incrementAndGet();
            byte[] body = addresses.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        lists.start();
        geyser = mock(GeyserImpl.class, RETURNS_DEEP_STUBS);
        var bedrock = geyser.config().advanced().bedrock();
        when(bedrock.useHaproxyProtocol()).thenReturn(true);
        when(bedrock.mtu()).thenReturn(1400);
        when(bedrock.haproxyProtocolWhitelistedIps()).thenReturn(List.of(
            "http://127.0.0.1:" + lists.getAddress().getPort() + "/proxies"));
    }

    @AfterEach
    void tearDown() {
        if (lists != null) {
            lists.stop(0);
        }
    }

    @Test
    void loadsUrlOncePerListenerAndRefreshesOnRestart() throws Exception {
        withListener(server -> {
            assertEquals(1, fetches.get());
            assertTrue(server.onConnectionRequest(FIRST_PROXY, CLIENT));
            assertFalse(server.onConnectionRequest(SECOND_PROXY, CLIENT));
            addresses.set("198.51.100.8\n");
            assertTrue(server.onConnectionRequest(FIRST_PROXY, CLIENT));
            assertFalse(server.onConnectionRequest(SECOND_PROXY, CLIENT));
            assertEquals(1, fetches.get());
        });
        withListener(server -> {
            assertEquals(2, fetches.get());
            assertFalse(server.onConnectionRequest(FIRST_PROXY, CLIENT));
            assertTrue(server.onConnectionRequest(SECOND_PROXY, CLIENT));
            assertEquals(2, fetches.get());
        });
    }

    @Test
    void failedListFetchDoesNotAllowEveryProxy() throws Exception {
        status.set(503);
        withListener(server -> {
            assertFalse(server.onConnectionRequest(FIRST_PROXY, CLIENT));
            assertFalse(server.onConnectionRequest(SECOND_PROXY, CLIENT));
            assertEquals(1, fetches.get());
        });
    }

    @Test
    void disabledProxyProtocolDoesNotFetchConfiguredUrls() throws Exception {
        when(geyser.config().advanced().bedrock().useHaproxyProtocol()).thenReturn(false);
        withListener(server -> {
            assertTrue(server.onConnectionRequest(FIRST_PROXY, CLIENT));
            assertEquals(0, fetches.get());
        });
    }

    @Test
    void emptyWhitelistKeepsAllowingAnyProxy() throws Exception {
        when(geyser.config().advanced().bedrock().haproxyProtocolWhitelistedIps()).thenReturn(List.of());
        withListener(server -> {
            assertTrue(server.onConnectionRequest(FIRST_PROXY, CLIENT));
            assertTrue(server.onConnectionRequest(SECOND_PROXY, CLIENT));
            assertEquals(0, fetches.get());
        });
    }

    private void withListener(Consumer<RaknetServer> assertions) throws Exception {
        try (var instance = mockStatic(GeyserImpl.class);
             var requests = mockStatic(ConnectionRequests.class)) {
            instance.when(GeyserImpl::getInstance).thenReturn(geyser);
            requests.when(() -> ConnectionRequests.accept(any(), any(), any())).thenReturn(true);
            var server = new RaknetServer(geyser, 1);
            try {
                server.bind(new InetSocketAddress("127.0.0.1", 0)).get(5, TimeUnit.SECONDS);
                assertions.accept(server);
            } finally {
                server.shutdown();
            }
        }
    }
}
