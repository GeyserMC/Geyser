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

package org.geysermc.geyser.util;

import com.fasterxml.jackson.databind.JsonNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.command.CommandSender;
import org.geysermc.geyser.network.MinecraftProtocol;
import org.geysermc.geyser.text.GeyserLocale;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class VersionCheckUtils {

    public static void checkForOutdatedFloodgate(GeyserLogger logger) {
        try {
            // This class was removed in Floodgate 2.1.0-SNAPSHOT - if it still exists, Floodgate will not work
            // with this version of Geyser
            Class.forName("org.geysermc.floodgate.util.TimeSyncerHolder");
            logger.warning(GeyserLocale.getLocaleStringLog("geyser.bootstrap.floodgate.outdated", Constants.FLOODGATE_DOWNLOAD_LOCATION));
        } catch (ClassNotFoundException ignored) {
            // Nothing to worry about; we want this exception
        }
    }

    public static void checkForGeyserUpdate(Supplier<CommandSender> recipient) {
        CompletableFuture.runAsync(() -> {
            try {
                JsonNode json = WebUtils.getJson("https://api.geysermc.org/v2/versions/geyser");
                JsonNode bedrock = json.get("bedrock").get("protocol");
                int protocolVersion = bedrock.get("id").asInt();
                if (MinecraftProtocol.getBedrockCodec(protocolVersion) != null) {
                    // We support the latest version! No need to print a message.
                    return;
                }

                final String newBedrockVersion = bedrock.get("name").asText();

                // Delayed for two reasons: save unnecessary processing, and wait to load locale if this is on join.
                CommandSender sender = recipient.get();

                // Overarching component is green - geyser.version.new component cannot be green or else the link blue is overshadowed
                Component message = Component.text().color(NamedTextColor.GREEN)
                        .append(Component.text(GeyserLocale.getPlayerLocaleString("geyser.version.new", sender.getLocale(), newBedrockVersion))
                                .replaceText(TextReplacementConfig.builder()
                                        .match("\\{1\\}") // Replace "Download here: {1}" so we can use fancy text component yesyes
                                        .replacement(Component.text()
                                                .content(Constants.GEYSER_DOWNLOAD_LOCATION)
                                                .color(NamedTextColor.BLUE)
                                                .decoration(TextDecoration.UNDERLINED, TextDecoration.State.TRUE)
                                                .clickEvent(ClickEvent.openUrl(Constants.GEYSER_DOWNLOAD_LOCATION)))
                                        .build()))
                        .build();
                sender.sendMessage(message);
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().error("Error whilst checking for Geyser update!", e);
            }
        });
    }

    private VersionCheckUtils() {
    }
}
