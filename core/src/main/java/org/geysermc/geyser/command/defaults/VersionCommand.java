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
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.WebUtils;

import java.io.IOException;
import java.util.List;

public class VersionCommand extends GeyserCommand {

    private final GeyserImpl geyser;

    public VersionCommand(GeyserImpl geyser, String name, String description, String permission) {
        super(name, description, permission);

        this.geyser = geyser;
    }

    @Override
    public void execute(GeyserSession session, GeyserCommandSource sender, String[] args) {
        String bedrockVersions;
        List<BedrockCodec> supportedCodecs = GameProtocol.SUPPORTED_BEDROCK_CODECS;
        if (supportedCodecs.size() > 1) {
            bedrockVersions = supportedCodecs.get(0).getMinecraftVersion() + " - " + supportedCodecs.get(supportedCodecs.size() - 1).getMinecraftVersion();
        } else {
            bedrockVersions = GameProtocol.SUPPORTED_BEDROCK_CODECS.get(0).getMinecraftVersion();
        }
        String javaVersions;
        List<String> supportedJavaVersions = GameProtocol.getJavaVersions();
        if (supportedJavaVersions.size() > 1) {
            javaVersions = supportedJavaVersions.get(0) + " - " + supportedJavaVersions.get(supportedJavaVersions.size() - 1);
        } else {
            javaVersions = supportedJavaVersions.get(0);
        }

        sender.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.commands.version.version", sender.locale(),
                GeyserImpl.NAME, GeyserImpl.VERSION, javaVersions, bedrockVersions));

        // Disable update checking in dev mode and for players in Geyser Standalone
        if (!GeyserImpl.getInstance().isProductionEnvironment() || (!sender.isConsole() && geyser.getPlatformType() == PlatformType.STANDALONE)) {
            return;
        }

        if (GeyserImpl.IS_DEV) {
            // TODO cloud use language string
            sender.sendMessage("You are running a development build of Geyser! Please report any bugs you find on our Discord server: %s"
                    .formatted("https://discord.gg/geysermc"));
            //sender.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.core.dev_build", sender.locale(), "https://discord.gg/geysermc"));
            return;
        }

        sender.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.commands.version.checking", sender.locale()));
        try {
            int buildNumber = this.geyser.buildNumber();
            JsonNode response = WebUtils.getJson("https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest");
            int latestBuildNumber = response.get("build").asInt();

            if (latestBuildNumber == buildNumber) {
                sender.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.commands.version.no_updates", sender.locale()));
                return;
            }

            sender.sendMessage(GeyserLocale.getPlayerLocaleString(
                    "geyser.commands.version.outdated",
                    sender.locale(), (latestBuildNumber - buildNumber), "https://geysermc.org/download"
            ));
        } catch (IOException e) {
            GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.commands.version.failed"), e);
            sender.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.commands.version.failed", sender.locale()));
        }
    }

    @Override
    public boolean isSuggestedOpOnly() {
        return true;
    }
}
