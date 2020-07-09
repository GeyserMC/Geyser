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
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.dump.DumpInfo;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.connector.utils.WebUtils;

import java.io.IOException;

public class DumpCommand extends GeyserCommand {

    private final GeyserConnector connector;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DUMP_URL = "https://dump.geysermc.org/";

    public DumpCommand(GeyserConnector connector, String name, String description, String permission) {
        super(name, description, permission);

        this.connector = connector;

        final SimpleFilterProvider filter = new SimpleFilterProvider();
        filter.addFilter("dump_user_auth", SimpleBeanPropertyFilter.serializeAllExcept(new String[] {"password"}));

        MAPPER.setFilterProvider(filter);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.dump.collecting"));
        String dumpData = "";
        try {
            dumpData = MAPPER.writeValueAsString(new DumpInfo());
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + LanguageUtils.getLocaleStringLog("geyser.commands.dump.collect_error"));
            connector.getLogger().error(LanguageUtils.getLocaleStringLog("geyser.commands.dump.collect_error_short"), e);
            return;
        }

        sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.dump.uploading"));
        String response;
        JsonNode responseNode;
        try {
            response = WebUtils.post(DUMP_URL + "documents", dumpData);
            responseNode = MAPPER.readTree(response);
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + LanguageUtils.getLocaleStringLog("geyser.commands.dump.upload_error"));
            connector.getLogger().error(LanguageUtils.getLocaleStringLog("geyser.commands.dump.upload_error_short"), e);
            return;
        }

        if (!responseNode.has("key")) {
            sender.sendMessage(ChatColor.RED + LanguageUtils.getLocaleStringLog("geyser.commands.dump.upload_error_short") + ": " + (responseNode.has("message") ? responseNode.get("message").asText() : response));
            return;
        }

        String uploadedDumpUrl = DUMP_URL + responseNode.get("key").asText();
        sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.dump.message") + " " + ChatColor.DARK_AQUA + uploadedDumpUrl);
        if (!sender.isConsole()) {
            connector.getLogger().info(LanguageUtils.getLocaleStringLog("geyser.commands.dump.created", sender.getName(), uploadedDumpUrl));
        }
    }
}
