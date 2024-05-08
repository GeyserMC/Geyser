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

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.dump.DumpInfo;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.AsteriskSerializer;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.WebUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DumpCommand extends GeyserCommand {

    private final GeyserImpl geyser;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DUMP_URL = "https://dump.geysermc.org/";

    public DumpCommand(GeyserImpl geyser, String name, String description, String permission) {
        super(name, description, permission);

        this.geyser = geyser;
    }

    @Override
    public void execute(GeyserSession session, GeyserCommandSource sender, String[] args) {
        // Only allow the console to create dumps on Geyser Standalone
        if (!sender.isConsole() && geyser.getPlatformType() == PlatformType.STANDALONE) {
            sender.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.permission_fail", sender.locale()));
            return;
        }

        boolean showSensitive = false;
        boolean offlineDump = false;
        boolean addLog = false;
        if (args.length >= 1) {
            for (String arg : args) {
                switch (arg) {
                    case "full" -> showSensitive = true;
                    case "offline" -> offlineDump = true;
                    case "logs" -> addLog = true;
                }
            }
        }

        AsteriskSerializer.showSensitive = showSensitive;

        sender.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.commands.dump.collecting", sender.locale()));
        String dumpData;
        try {
            if (offlineDump) {
                DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
                // Make arrays easier to read
                prettyPrinter.indentArraysWith(new DefaultIndenter("    ", "\n"));
                dumpData = MAPPER.writer(prettyPrinter).writeValueAsString(new DumpInfo(addLog));
            } else {
                dumpData = MAPPER.writeValueAsString(new DumpInfo(addLog));
            }
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.commands.dump.collect_error", sender.locale()));
            geyser.getLogger().error(GeyserLocale.getLocaleStringLog("geyser.commands.dump.collect_error_short"), e);
            return;
        }

        String uploadedDumpUrl;

        if (offlineDump) {
            sender.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.commands.dump.writing", sender.locale()));

            try {
                FileOutputStream outputStream = new FileOutputStream(GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("dump.json").toFile());
                outputStream.write(dumpData.getBytes());
                outputStream.close();
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.commands.dump.write_error", sender.locale()));
                geyser.getLogger().error(GeyserLocale.getLocaleStringLog("geyser.commands.dump.write_error_short"), e);
                return;
            }

            uploadedDumpUrl = "dump.json";
        } else {
            sender.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.commands.dump.uploading", sender.locale()));

            String response;
            JsonNode responseNode;
            try {
                response = WebUtils.post(DUMP_URL + "documents", dumpData);
                responseNode = MAPPER.readTree(response);
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.commands.dump.upload_error", sender.locale()));
                geyser.getLogger().error(GeyserLocale.getLocaleStringLog("geyser.commands.dump.upload_error_short"), e);
                return;
            }

            if (!responseNode.has("key")) {
                sender.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.commands.dump.upload_error_short", sender.locale()) + ": " + (responseNode.has("message") ? responseNode.get("message").asText() : response));
                return;
            }

            uploadedDumpUrl = DUMP_URL + responseNode.get("key").asText();
        }

        sender.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.commands.dump.message", sender.locale()) + " " + ChatColor.DARK_AQUA + uploadedDumpUrl);
        if (!sender.isConsole()) {
            geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.commands.dump.created", sender.name(), uploadedDumpUrl));
        }
    }

    @NonNull
    @Override
    public List<String> subCommands() {
        return Arrays.asList("offline", "full", "logs");
    }

    @Override
    public boolean isSuggestedOpOnly() {
        return true;
    }
}
