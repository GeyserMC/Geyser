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

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.network.BedrockProtocol;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.connector.utils.WebUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

public class VersionCommand extends GeyserCommand {

    private final GeyserConnector connector;

    public VersionCommand(GeyserConnector connector, String name, String description, String permission) {
        super(name, description, permission);

        this.connector = connector;
    }

    @Override
    public void execute(GeyserSession session, CommandSender sender, String[] args) {
        String bedrockVersions;
        List<BedrockPacketCodec> supportedCodecs = BedrockProtocol.SUPPORTED_BEDROCK_CODECS;
        if (supportedCodecs.size() > 1) {
            bedrockVersions = supportedCodecs.get(0).getMinecraftVersion() + " - " + supportedCodecs.get(supportedCodecs.size() - 1).getMinecraftVersion();
        } else {
            bedrockVersions = BedrockProtocol.DEFAULT_BEDROCK_CODEC.getMinecraftVersion();
        }

        sender.sendMessage(LanguageUtils.getPlayerLocaleString("geyser.commands.version.version", sender.getLocale(), GeyserConnector.NAME, GeyserConnector.VERSION, GeyserConnector.MINECRAFT_VERSION, bedrockVersions));

        // Disable update checking in dev mode and for players in Geyser Standalone
        //noinspection ConstantConditions - changes in production
        if (!GeyserConnector.VERSION.equals("DEV") && !(!sender.isConsole() && connector.getPlatformType() == PlatformType.STANDALONE)) {
            sender.sendMessage(LanguageUtils.getPlayerLocaleString("geyser.commands.version.checking", sender.getLocale()));
            try {
                Properties gitProp = new Properties();
                gitProp.load(FileUtils.getResource("git.properties"));

                String buildXML = WebUtils.getBody("https://ci.opencollab.dev/job/GeyserMC/job/Geyser/job/" + URLEncoder.encode(gitProp.getProperty("git.branch"), StandardCharsets.UTF_8.toString()) + "/lastSuccessfulBuild/api/xml?xpath=//buildNumber");
                if (buildXML.startsWith("<buildNumber>")) {
                    int latestBuildNum = Integer.parseInt(buildXML.replaceAll("<(\\\\)?(/)?buildNumber>", "").trim());
                    int buildNum = Integer.parseInt(gitProp.getProperty("git.build.number"));
                    if (latestBuildNum == buildNum) {
                        sender.sendMessage(LanguageUtils.getPlayerLocaleString("geyser.commands.version.no_updates", sender.getLocale()));
                    } else {
                        sender.sendMessage(LanguageUtils.getPlayerLocaleString("geyser.commands.version.outdated", sender.getLocale(), (latestBuildNum - buildNum), "https://ci.geysermc.org/"));
                    }
                } else {
                    throw new AssertionError("buildNumber missing");
                }
            } catch (IOException | AssertionError | NumberFormatException e) {
                GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.commands.version.failed"), e);
                sender.sendMessage(ChatColor.RED + LanguageUtils.getPlayerLocaleString("geyser.commands.version.failed", sender.getLocale()));
            }
        }
    }
}
