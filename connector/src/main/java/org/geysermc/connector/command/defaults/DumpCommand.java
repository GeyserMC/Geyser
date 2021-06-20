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

package org.geysermc.connector.command.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.common.serializer.AsteriskSerializer;
import org.geysermc.connector.dump.DumpInfo;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.connector.utils.WebUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DumpCommand extends GeyserCommand {

    private final GeyserConnector connector;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DUMP_URL = "https://dump.geysermc.org/";

    public DumpCommand(GeyserConnector connector, String name, String description, String permission) {
        super(name, description, permission);

        this.connector = connector;
    }

    @Override
    public void execute(GeyserSession session, CommandSender sender, String[] args) {
        // Only allow the console to create dumps on Geyser Standalone
        if (!sender.isConsole() && connector.getPlatformType() == PlatformType.STANDALONE) {
            sender.sendMessage(LanguageUtils.getPlayerLocaleString("geyser.bootstrap.command.permission_fail", sender.getLocale()));
            return;
        }

        boolean showSensitive = false;
        boolean offlineDump = false;
        if (args.length >= 1) {
            for (String arg : args) {
                switch (arg) {
                    case "full":
                        showSensitive = true;
                        break;
                    case "offline":
                        offlineDump = true;
                        break;

                }
            }
        }

        AsteriskSerializer.showSensitive = showSensitive;

        sender.sendMessage(LanguageUtils.getPlayerLocaleString("geyser.commands.dump.collecting", sender.getLocale()));
        String dumpData = "";
        try {
            if (offlineDump) {
                dumpData = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(new DumpInfo());
            } else {
                dumpData = MAPPER.writeValueAsString(new DumpInfo());
            }
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + LanguageUtils.getPlayerLocaleString("geyser.commands.dump.collect_error", sender.getLocale()));
            connector.getLogger().error(LanguageUtils.getLocaleStringLog("geyser.commands.dump.collect_error_short"), e);
            return;
        }

        String uploadedDumpUrl = "";

        if (offlineDump) {
            sender.sendMessage(LanguageUtils.getPlayerLocaleString("geyser.commands.dump.writing", sender.getLocale()));

            try {
                FileOutputStream outputStream = new FileOutputStream(GeyserConnector.getInstance().getBootstrap().getConfigFolder().resolve("dump.json").toFile());
                outputStream.write(dumpData.getBytes());
                outputStream.close();
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + LanguageUtils.getPlayerLocaleString("geyser.commands.dump.write_error", sender.getLocale()));
                connector.getLogger().error(LanguageUtils.getLocaleStringLog("geyser.commands.dump.write_error_short"), e);
                return;
            }

            uploadedDumpUrl = "dump.json";
        } else {
            sender.sendMessage(LanguageUtils.getPlayerLocaleString("geyser.commands.dump.uploading", sender.getLocale()));

            String response;
            JsonNode responseNode;
            try {
                response = WebUtils.post(DUMP_URL + "documents", dumpData);
                responseNode = MAPPER.readTree(response);
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + LanguageUtils.getPlayerLocaleString("geyser.commands.dump.upload_error", sender.getLocale()));
                connector.getLogger().error(LanguageUtils.getLocaleStringLog("geyser.commands.dump.upload_error_short"), e);
                return;
            }

            if (!responseNode.has("key")) {
                sender.sendMessage(ChatColor.RED + LanguageUtils.getPlayerLocaleString("geyser.commands.dump.upload_error_short", sender.getLocale()) + ": " + (responseNode.has("message") ? responseNode.get("message").asText() : response));
                return;
            }

            uploadedDumpUrl = DUMP_URL + responseNode.get("key").asText();
        }

        sender.sendMessage(LanguageUtils.getPlayerLocaleString("geyser.commands.dump.message", sender.getLocale()) + " " + ChatColor.DARK_AQUA + uploadedDumpUrl);
        if (!sender.isConsole()) {
            connector.getLogger().info(LanguageUtils.getLocaleStringLog("geyser.commands.dump.created", sender.getName(), uploadedDumpUrl));
        }
    }

    @Override
    public List<String> getSubCommands() {
        return Arrays.asList("offline", "full");
    }
}
