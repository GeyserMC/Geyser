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
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.LoopbackUtil;
import org.geysermc.geyser.util.WebUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class ConnectionTestCommand extends GeyserCommand {
    public static String CONNECTION_TEST_MOTD = null;

    private final GeyserImpl geyser;

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
            sender.sendMessage("Provide the Bedrock server IP you are trying to connect with. Example: `test.geysermc.org:19132`");
            return;
        }

        // Still allow people to not supply a port and fallback to 19132
        String[] fullAddress = args[0].split(":", 2);
        int port;
        if (fullAddress.length == 2) {
            port = Integer.parseInt(fullAddress[1]);
        } else {
            port = 19132;
        }

        // Issue: do the ports not line up?
        if (port != geyser.getConfig().getBedrock().port()) {
            sender.sendMessage("The port you supplied (" + port + ") does not match the port supplied in Geyser's configuration ("
                    + geyser.getConfig().getBedrock().port() + "). You can change it under `bedrock` `port`.");
        }

        // Issue: is the `bedrock` `address` in the config different?
        if (!geyser.getConfig().getBedrock().address().equals("0.0.0.0")) {
            sender.sendMessage("The address specified in `bedrock` `address` is not \"0.0.0.0\" - this may cause issues unless this is deliberate and intentional.");
        }

        // Issue: did someone turn on enable-proxy-protocol and they didn't mean it?
        if (geyser.getConfig().getBedrock().isEnableProxyProtocol()) {
            sender.sendMessage("You have the `enable-proxy-protocol` setting enabled. " +
                    "Unless you're deliberately using additional software that REQUIRES this setting, you may not need it enabled.");
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Issue: SRV record?
                String ip = fullAddress[0];
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
                byte[] random = new byte[2];
                new Random().nextBytes(random);
                StringBuilder randomStr = new StringBuilder();
                for (byte b : random) {
                    randomStr.append(Integer.toHexString(b));
                }
                String connectionTestMotd = "Geyser Connection Test " + randomStr;
                CONNECTION_TEST_MOTD = connectionTestMotd;

                sender.sendMessage("Testing server connection now. Please wait...");
                JsonNode output;
                try {
                    output = WebUtils.getJson("https://checker.geysermc.org/ping?hostname=" + ip + "&port=" + port);
                } finally {
                    CONNECTION_TEST_MOTD = null;
                }

                JsonNode cache = output.get("cache");
                String when;
                if (cache.get("fromCache").asBoolean()) {
                    when = cache.get("secondsSince").asInt() + " seconds ago";
                } else {
                    when = "now";
                }

                if (output.get("success").asBoolean()) {
                    JsonNode ping = output.get("ping");
                    JsonNode pong = ping.get("pong");
                    String remoteMotd = pong.get("motd").asText();
                    if (!connectionTestMotd.equals(remoteMotd)) {
                        sender.sendMessage("The MOTD did not match when we pinged the server (we got '" + remoteMotd + "'). " +
                                "Did you supply the correct IP and port?");
                        sendLinks(sender);
                        return;
                    }

                    if (ping.get("tcpFirst").asBoolean()) {
                        sender.sendMessage("Your server hardware likely has some sort of firewall preventing people from joining easily. See LINK for more information.");
                        sendLinks(sender);
                        return;
                    }

                    sender.sendMessage("Your server is likely online and working as of " + when + "!");
                    sendLinks(sender);
                    return;
                }

                sender.sendMessage("Your server is likely unreachable from outside the network as of " + when + ".");
                sendLinks(sender);
            } catch (Exception e) {
                sender.sendMessage("Error while trying to check your connection!");
                geyser.getLogger().error("Error while trying to check your connection!", e);
            }
        });
    }

    private void sendLinks(GeyserCommandSource sender) {
        sender.sendMessage("If you still have issues, check to see if your hosting provider has a specific setup: " +
                "https://wiki.geysermc.org/geyser/supported-hosting-providers/" + ", see this page: "
                + "https://wiki.geysermc.org/geyser/fixing-unable-to-connect-to-world/" + ", or contact us on our Discord: " + "https://discord.gg/geysermc");
    }

    @Override
    public boolean isSuggestedOpOnly() {
        return true;
    }
}
