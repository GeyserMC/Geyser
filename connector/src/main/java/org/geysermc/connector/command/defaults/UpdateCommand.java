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
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.network.BedrockProtocol;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.connector.utils.WebUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class UpdateCommand extends GeyserCommand {

    public GeyserConnector connector;

    public UpdateCommand(GeyserConnector connector, String name, String description, String permission) {
        super(name, description, permission);
        this.connector = connector;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        //Currently only Spigot works
        String platform = GeyserConnector.getInstance().getPlatformType().getPlatformName();
        if (!platform.equals("Spigot")) {
            sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.update.unsupported"));
            return;
        }

        sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.version.version", GeyserConnector.NAME, GeyserConnector.VERSION, MinecraftConstants.GAME_VERSION, BedrockProtocol.DEFAULT_BEDROCK_CODEC.getMinecraftVersion()));

        GeyserConnector.getInstance().getGeneralThreadPool().execute(new Runnable() {

            public void run() {
                sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.version.checking"));
                try {
                    Properties gitProp = new Properties();
                    gitProp.load(FileUtils.getResource("git.properties"));
                    String buildXML = WebUtils.getBody("https://ci.nukkitx.com/job/GeyserMC/job/Geyser/job/" + URLEncoder.encode(gitProp.getProperty("git.branch"), StandardCharsets.UTF_8.toString()) + "/lastSuccessfulBuild/api/xml?xpath=//buildNumber");

                    if (buildXML.startsWith("<buildNumber>")) {
                        int latestBuildNum = Integer.parseInt(buildXML.replaceAll("<(\\\\)?(/)?buildNumber>", "").trim());
                        int buildNum = Integer.parseInt(gitProp.getProperty("git.build.number"));
                        //Compare build numbers.
                        if (latestBuildNum == buildNum) {
                            sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.version.no_updates"));
                        } else {
                            sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.update.outdated", (latestBuildNum - buildNum)));

                            String downloadUrl = "https://ci.nukkitx.com/job/GeyserMC/job/Geyser/job/" + URLEncoder.encode(gitProp.getProperty("git.branch"), StandardCharsets.UTF_8.toString()) + "/lastSuccessfulBuild/artifact/bootstrap/" + platform.toLowerCase() + "/target/Geyser" + (platform.equals("Standalone") ? ".jar" : "-" + platform + ".jar");
                            String checksumData = WebUtils.getBody(downloadUrl + "/*fingerprint*/");

                            //TODO: Replace with regex. Gotta figure that one out though XD
                            int divIndex = checksumData.indexOf("<div class=\"md5sum\"");
                            divIndex = checksumData.indexOf(">", divIndex);
                            int endDivIndex = checksumData.indexOf("</div>", divIndex);
                            String remoteChecksum = checksumData.substring(divIndex + 1, endDivIndex).replace("MD5: ", "").trim();

                            File updateFolder = GeyserConnector.getInstance().getBootstrap().getConfigFolder().getParent().resolve("update").toFile();
                            updateFolder.mkdir();

                            File downloadedFile = new File(updateFolder, platform.equals("Standalone") ? "Geyser.jar" : "Geyser-" + platform + ".jar");

                            sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.update.downloading"));
                            WebUtils.downloadFile(downloadUrl, downloadedFile.getAbsolutePath());
                            sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.update.downloaded"));

                            //Verify file integrity.
                            if (Files.hash(downloadedFile, Hashing.md5()).toString().equals(remoteChecksum)) {
                                sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.commands.update.success"));
                            } else {
                                //Delete the file if the local checksum does not match remote.
                                downloadedFile.delete();
                                throw new AssertionError("checksums dont match");
                            }

                        }
                    } else {
                        throw new AssertionError("buildNumber missing");
                    }
                } catch (IOException | AssertionError | NumberFormatException e) {
                    GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.commands.update.failed"), e);
                    sender.sendMessage(ChatColor.RED + LanguageUtils.getLocaleStringLog("geyser.commands.update.failed"));
                }
            }
        });
    }
}
