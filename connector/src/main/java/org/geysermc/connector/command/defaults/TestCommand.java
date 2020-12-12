/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.command.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.steveice10.mc.auth.exception.request.AuthPendingException;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.command.GeyserCommand;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class TestCommand extends GeyserCommand {

    public GeyserConnector connector;

    public TestCommand(GeyserConnector connector, String name, String description, String permission) {
        super(name, description, permission);
        this.connector = connector;
    }

    private static final String clientId = "204cefd1-4818-4de1-b98d-513fae875d88";

    @Override
    public void execute(CommandSender sender, String[] args) {
        try {
            MsaAuthenticationService msaAuthenticationService = new MsaAuthenticationService(clientId);

            MsaAuthenticationService.MsCodeResponse response = msaAuthenticationService.getAuthCode();

            sender.sendMessage("Please enter the code '" + response.user_code + "' on " + response.verification_uri);

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        msaAuthenticationService.login();

//                        AuthenticationService authService = new AuthenticationService("");
//                        authService.setAccessToken(authToken);
//
//                        GeyserConnector.getInstance().getLogger().debug(new MinecraftProtocol(authService).toString());

                        sender.sendMessage(msaAuthenticationService.getAccessToken());

                        this.cancel();
                    } catch (RequestException e) {
                        // Ignore pending auth exceptions
                        if (e instanceof AuthPendingException) {
                            return;
                        }

                        e.printStackTrace();

                        if (!sender.isConsole()) {
                            sender.sendMessage(e.getMessage());
                        }

                        this.cancel();
                    }
                }
            }, 0, response.interval * 1000);

        } catch (RequestException e) {
            e.printStackTrace();

            if (!sender.isConsole()) {
                sender.sendMessage(e.getMessage());
            }
        }
    }


    public static String post(String reqURL, String contentType, String postContent) throws IOException {
        URL url = null;
        url = new URL(reqURL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", contentType);
        con.setRequestProperty("User-Agent", "MCAuthLib");
        con.setDoOutput(true);

        OutputStream out = con.getOutputStream();
        out.write(postContent.getBytes(StandardCharsets.UTF_8));
        out.close();

        return connectionToString(con);
    }

    /**
     * Get the string output from the passed {@link HttpURLConnection}
     *
     * @param con The connection to get the string from
     * @return The body of the returned page
     * @throws IOException
     */
    private static String connectionToString(HttpURLConnection con) throws IOException {
        // Send the request (we dont use this but its required for getErrorStream() to work)
        con.getResponseCode();

        // Read the error message if there is one if not just read normally
        InputStream inputStream = con.getErrorStream();
        if (inputStream == null) {
            inputStream = con.getInputStream();
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        StringBuffer content = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
            content.append("\n");
        }

        in.close();
        con.disconnect();

        return content.toString();
    }
}
