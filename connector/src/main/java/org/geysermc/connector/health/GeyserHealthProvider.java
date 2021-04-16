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

import org.geysermc.connector.GeyserLogger;
import org.geysermc.connector.configuration.GeyserConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class GeyserHealthProvider extends Thread {

    private final GeyserConfiguration.IHealthConfiguration healthConfiguration;
    private final GeyserLogger logger;

    public GeyserHealthProvider(GeyserConfiguration.IHealthConfiguration healthConfiguration, GeyserLogger logger) {
        this.healthConfiguration = healthConfiguration;
        this.logger = logger;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        try {
            ServerSocket socket = new ServerSocket(healthConfiguration.getPort());
            logger.info("Health provider created.");
            while (true) {
                GeyserHelpResponder responder = new GeyserHelpResponder(socket.accept());
                responder.setDaemon(true);
                responder.start();
            }
        } catch (IOException ex) {
            logger.error("Failed to initialize server socket.", ex);
        }
    }

    private static class GeyserHelpResponder extends Thread {
        private final Socket inSocket;

        private GeyserHelpResponder(Socket inSocket) {
            this.inSocket = inSocket;
        }

        @Override
        public void run() {
            try (
                    InputStreamReader inputStreamReader = new InputStreamReader(inSocket.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(inSocket.getOutputStream());
                    PrintWriter printWriter = new PrintWriter(outputStreamWriter)
            ) {
                String request = bufferedReader.readLine();
                if (request == null) return;

                while (true) {
                    String ignore = bufferedReader.readLine();
                    if (ignore == null || ignore.length() == 0) break;
                }

                int initialSplit = request.indexOf(' ');
                if (initialSplit == -1) {
                    respondBadRequest(printWriter);
                    return;
                }

                String method = request.substring(0, initialSplit);

                if (!method.equalsIgnoreCase("GET")) {
                    respondNotFound(printWriter);
                    return;
                }

                int routeSplit = request.indexOf(' ', initialSplit + 1);
                if (routeSplit == -1) {
                    respondBadRequest(printWriter);
                    return;
                }

                String route = request.substring(initialSplit + 1, routeSplit);
                int routeQuerySplit = route.indexOf('?');
                if (routeQuerySplit != -1) {
                    route = route.substring(0, routeQuerySplit);
                }

                if (route.equalsIgnoreCase("/health")) {
                    printWriter.printf("HTTP/2 200 OK%n%n");
                } else {
                    respondNotFound(printWriter);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void respondBadRequest(PrintWriter writer) {
            writer.printf("HTTP/2 400 Bad Request%n%n");
        }

        private void respondNotFound(PrintWriter writer) {
            writer.printf("HTTP/2 404 Not Found%n%n");
        }
    }
}
