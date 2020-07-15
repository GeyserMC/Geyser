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

import com.github.steveice10.mc.protocol.MinecraftConstants;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.GeyserEdition;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.connector.utils.WebUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class VersionCommand extends GeyserCommand {

    public GeyserConnector connector;

    public VersionCommand(GeyserConnector connector, String name, String description, String permission) {
        super(name, description, permission);
        this.connector = connector;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.version.version", GeyserConnector.NAME, GeyserConnector.VERSION, MinecraftConstants.GAME_VERSION, GeyserEdition.INSTANCE.getCodec().getMinecraftVersion()));

        // Disable update checking in dev mode
        //noinspection ConstantConditions - changes in production
        if (!GeyserConnector.VERSION.equals("DEV")) {
            sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.version.checking"));
            try {
                Properties gitProp = new Properties();
                gitProp.load(FileUtils.getResource("git.properties"));

                String buildXML = WebUtils.getBody("https://ci.nukkitx.com/job/GeyserMC/job/Geyser/job/" + URLEncoder.encode(gitProp.getProperty("git.branch"), StandardCharsets.UTF_8.toString()) + "/lastSuccessfulBuild/api/xml?xpath=//buildNumber");
                if (buildXML.startsWith("<buildNumber>")) {
                    int latestBuildNum = Integer.parseInt(buildXML.replaceAll("<(\\\\)?(/)?buildNumber>", "").trim());
                    int buildNum = Integer.parseInt(gitProp.getProperty("git.build.number"));
                    if (latestBuildNum == buildNum) {
                        sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.version.no_updates"));
                    } else {
                        sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.version.outdated", (latestBuildNum - buildNum), "http://ci.geysermc.org/"));
                    }
                } else {
                    throw new AssertionError("buildNumber missing");
                }
            } catch (IOException | AssertionError | NumberFormatException e) {
                GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.commands.version.failed"), e);
                sender.sendMessage(ChatColor.RED + LanguageUtils.getLocaleStringLog("geyser.commands.version.failed"));
            }
        }
    }
}
