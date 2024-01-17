/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.command.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.LoopbackUtil;
import org.geysermc.geyser.util.WebUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class ConnectionTestCommand extends GeyserCommand {
    /*
     * The MOTD is temporarily changed during the connection test.
     * This allows us to check if we are pinging the correct Geyser instance
     */
    public static String CONNECTION_TEST_MOTD = null;

    private final GeyserImpl geyser;

    private final Random random = new Random();

    public ConnectionTestCommand(GeyserImpl geyser, String name, String description, String permission) {
        super(name, description, permission);
        this.geyser = geyser;
    }

    @Override
    public void execute(@Nullable GeyserSession session, GeyserCommandSource sender, String[] args) {
        // Only allow the console to create dumps on Geyser Standalone
        if (!sender.isConsole() && geyser.getPlatformType() == PlatformType.STANDALONE) {
            sender.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.permission_fail", sender.locale()));
            return;
        }

        if (args.length == 0) {
            sender.sendMessage("Provide the server IP and port you are trying to test Bedrock connections for. Example: `test.geysermc.org:19132`");
            return;
        }

        // Replace "<" and ">" symbols if they are present to avoid the common issue of people including them
        String[] fullAddress = args[0].replace("<", "").replace(">", "").split(":", 2);

        // Still allow people to not supply a port and fallback to 19132
        int port;
        if (fullAddress.length == 2) {
            try {
                port = Integer.parseInt(fullAddress[1]);
            } catch (NumberFormatException e) {
                // can occur if e.g. "/geyser connectiontest <ip>:<port> is ran
                sender.sendMessage("Not a valid port! Specify a valid numeric port.");
                return;
            }
        } else {
            port = 19132;
        }
        String ip = fullAddress[0];

        // Issue: people commonly checking placeholders
        if (ip.equals("ip")) {
            sender.sendMessage(ip + " is not a valid IP, and instead a placeholder. Please specify the IP to check.");
            return;
        }

        // Issue: checking 0.0.0.0 won't work
        if (ip.equals("0.0.0.0")) {
            sender.sendMessage("Please specify the IP that you would connect with. 0.0.0.0 in the config tells Geyser to the listen on the server's IPv4.");
            return;
        }

        // Issue: people testing local ip
        if (ip.equals("localhost") || ip.startsWith("127.") || ip.startsWith("10.") || ip.startsWith("192.168.")) {
            sender.sendMessage("This tool checks if connections from other networks are possible, so you cannot check a local IP.");
            return;
        }

        // Issue: port out of bounds
        if (port <= 0 || port >= 65535) {
            sender.sendMessage("The port you specified is invalid! Please specify a valid port.");
            return;
        }

        // Issue: do the ports not line up?
        if (port != geyser.getConfig().getBedrock().port()) {
            if (fullAddress.length == 2) {
                sender.sendMessage("The port you are testing with (" + port + ") is not the same as you set in your Geyser configuration ("
                    + geyser.getConfig().getBedrock().port() + ")");
                sender.sendMessage("Re-run the command with the port in the config, or change the `bedrock` `port` in the config.");
                if (geyser.getConfig().getBedrock().isCloneRemotePort()) {
                    sender.sendMessage("You have `clone-remote-port` enabled. This option ignores the `bedrock` `port` in the config, and uses the Java server port instead.");
                }
            } else {
                sender.sendMessage("You did not specify the port to check (add it with \":<port>\"), " +
                        "and the default port 19132 does not match the port in your Geyser configuration ("
                        + geyser.getConfig().getBedrock().port() + ")!");
                sender.sendMessage("Re-run the command with that port, or change the port in the config under `bedrock` `port`.");
            }
        }

        // Issue: is the `bedrock` `address` in the config different?
        if (!geyser.getConfig().getBedrock().address().equals("0.0.0.0")) {
            sender.sendMessage("The address specified in `bedrock` `address` is not \"0.0.0.0\" - this may cause issues unless this is deliberate and intentional.");
        }

        // Issue: did someone turn on enable-proxy-protocol, and they didn't mean it?
        if (geyser.getConfig().getBedrock().isEnableProxyProtocol()) {
            sender.sendMessage("You have the `enable-proxy-protocol` setting enabled. " +
                    "Unless you're deliberately using additional software that REQUIRES this setting, you may not need it enabled.");
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Issue: SRV record?
                String[] record = WebUtils.findSrvRecord(geyser, ip);
                if (record != null && !ip.equals(record[3]) && !record[2].equals(String.valueOf(port))) {
                    sender.sendMessage("Bedrock Edition does not support SRV records. Try connecting to your server using the address " + record[3] + " and the port " + record[2]
                            + ". If that fails, re-run this command with that address and port.");
                    return;
                }

                // Issue: does Loopback need applying?
                if (LoopbackUtil.needsLoopback(GeyserImpl.getInstance().getLogger())) {
                    sender.sendMessage("Loopback is not applied on this computer! You will have issues connecting from the same computer. " +
                            "See here for steps on how to resolve: " + "https://wiki.geysermc.org/geyser/fixing-unable-to-connect-to-world/#using-geyser-on-the-same-computer");
                }

                // Generate some random, unique bits that another server wouldn't provide
                byte[] randomBytes = new byte[2];
                this.random.nextBytes(randomBytes);
                StringBuilder randomStr = new StringBuilder();
                for (byte b : randomBytes) {
                    randomStr.append(Integer.toHexString(b));
                }
                String connectionTestMotd = "Geyser Connection Test " + randomStr;
                CONNECTION_TEST_MOTD = connectionTestMotd;

                sender.sendMessage("Testing server connection now. Please wait...");
                JsonNode output;
                try {
                    String hostname = URLEncoder.encode(ip, StandardCharsets.UTF_8);
                    output = WebUtils.getJson("https://checker.geysermc.org/ping?hostname=" + hostname + "&port=" + port);
                } finally {
                    CONNECTION_TEST_MOTD = null;
                }

                if (output.get("success").asBoolean()) {
                    JsonNode cache = output.get("cache");
                    String when;
                    if (cache.get("fromCache").asBoolean()) {
                        when = cache.get("secondsSince").asInt() + " seconds ago";
                    } else {
                        when = "now";
                    }

                    JsonNode ping = output.get("ping");
                    JsonNode pong = ping.get("pong");
                    String remoteMotd = pong.get("motd").asText();
                    if (!connectionTestMotd.equals(remoteMotd)) {
                        sender.sendMessage("The MOTD did not match when we pinged the server (we got '" + remoteMotd + "'). " +
                                "Did you supply the correct IP and port of your server?");
                        sendLinks(sender);
                        return;
                    }

                    if (ping.get("tcpFirst").asBoolean()) {
                        sender.sendMessage("Your server hardware likely has some sort of firewall preventing people from joining easily. See https://geysermc.link/ovh-firewall for more information.");
                        sendLinks(sender);
                        return;
                    }

                    sender.sendMessage("Your server is likely online and working as of " + when + "!");
                    sendLinks(sender);
                    return;
                }

                sender.sendMessage("Your server is likely unreachable from outside the network!");
                JsonNode message = output.get("message");
                if (message != null && !message.asText().isEmpty()) {
                    sender.sendMessage("Got the error message: " + message.asText());
                }
                sendLinks(sender);
            } catch (Exception e) {
                sender.sendMessage("An error occurred while trying to check your connection! Check the console for more information.");
                geyser.getLogger().error("Error while trying to check your connection!", e);
            }
        });
    }

    private void sendLinks(GeyserCommandSource sender) {
        sender.sendMessage("If you still face issues, check the setup guide for instructions: " +
                "https://wiki.geysermc.org/geyser/setup/");
        sender.sendMessage("If that does not work, see " + "https://wiki.geysermc.org/geyser/fixing-unable-to-connect-to-world/" + ", or contact us on Discord: " + "https://discord.gg/geysermc");
    }

    @Override
    public boolean isSuggestedOpOnly() {
        return true;
    }
}
