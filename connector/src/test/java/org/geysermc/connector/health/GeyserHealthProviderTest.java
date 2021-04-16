/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.health;

import org.geysermc.connector.MockGeyserLogger;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class GeyserHealthProviderTest {
    @Before
    public void setupProvider() {
        GeyserHealthProvider healthProvider = new GeyserHealthProvider(new GeyserConfiguration.IHealthConfiguration() {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public int getPort() {
                return 9001;
            }
        }, new MockGeyserLogger());
        healthProvider.setDaemon(true);
        healthProvider.start();
    }

    private String getResponse(PrintWriter writer, BufferedReader reader, String input) throws IOException {
        writer.println(input);
        writer.println();
        writer.flush();
        return reader.readLine();
    }

    private void withSocket(SocketStreamsAcceptor socketStreamsAcceptor) throws IOException {
        try (
                Socket socket = new Socket("localhost", 9001);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
                PrintWriter printWriter = new PrintWriter(outputStreamWriter);
                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            socketStreamsAcceptor.acceptStreams(printWriter, bufferedReader);
        }
    }

    @Test
    public void testNormalHealthCheck() throws IOException {
        withSocket((pw, br) -> Assert.assertEquals("HTTP/2 200 OK", getResponse(pw, br, "GET /health HTTP/2")));
        withSocket((pw, br) -> Assert.assertEquals("HTTP/2 200 OK", getResponse(pw, br, "GET /health HTTP/1.2")));
        withSocket((pw, br) -> Assert.assertEquals("HTTP/2 200 OK", getResponse(pw, br, "GET /health HTTP/1")));
        withSocket((pw, br) -> Assert.assertEquals("HTTP/2 200 OK", getResponse(pw, br, "GET /health?search=some_query HTTP/2")));
    }

    @Test
    public void testInvalidRoute() throws IOException {
        withSocket((pw, br) -> Assert.assertEquals("HTTP/2 404 Not Found", getResponse(pw, br, "GET /heal HTTP/2")));
        withSocket((pw, br) -> Assert.assertEquals("HTTP/2 404 Not Found", getResponse(pw, br, "GET /health/nested HTTP/2")));
    }

    @Test
    public void testBadRequest() throws IOException {
        withSocket((pw, br) -> Assert.assertEquals("HTTP/2 400 Bad Request", getResponse(pw, br, "")));
        withSocket((pw, br) -> Assert.assertEquals("HTTP/2 400 Bad Request", getResponse(pw, br, "  GET /health HTTP/2")));
        withSocket((pw, br) -> Assert.assertEquals("HTTP/2 400 Bad Request", getResponse(pw, br, "GET")));
        withSocket((pw, br) -> Assert.assertEquals("HTTP/2 400 Bad Request", getResponse(pw, br, "/health")));
    }

    @Test
    public void testBadMethod() throws IOException {
        withSocket((pw, br) -> Assert.assertEquals("HTTP/2 405 Not Allowed", getResponse(pw, br, "POST /health HTTP/2")));
        withSocket((pw, br) -> Assert.assertEquals("HTTP/2 405 Not Allowed", getResponse(pw, br, "PATCH /health HTTP/2")));
    }

    @FunctionalInterface
    private interface SocketStreamsAcceptor {
        void acceptStreams(PrintWriter writer, BufferedReader reader) throws IOException;
    }
}
