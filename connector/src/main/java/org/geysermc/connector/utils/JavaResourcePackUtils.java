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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.ResourcePackStatus;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientResourcePackStatusPacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.packet.TransferPacket;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.geysermc.common.window.SimpleFormWindow;
import org.geysermc.common.window.response.SimpleFormResponse;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.packconverter.api.PackConverter;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

public class JavaResourcePackUtils {

    public static final int WINDOW_ID = 5533;

    public static boolean handleBedrockResponse(GeyserSession session, String formData) {
        SimpleFormWindow window = session.getResourcePackCache().getForm();
        if (window == null) return true;
        session.getResourcePackCache().setForm(null);
        window.setResponse(formData);
        SimpleFormResponse response = (SimpleFormResponse) window.getResponse();
        if (response == null || response.getClickedButtonId() == 1) {
            ClientResourcePackStatusPacket packet = new ClientResourcePackStatusPacket(ResourcePackStatus.DECLINED);
            session.sendDownstreamPacket(packet);
        } else if (response.getClickedButtonId() == 0) {
            ClientResourcePackStatusPacket packet = new ClientResourcePackStatusPacket(ResourcePackStatus.ACCEPTED);
            session.sendDownstreamPacket(packet);

            Path cache = session.getConnector().getBootstrap().getConfigFolder().resolve("cache");
            if (!cache.toFile().exists()) {
                //noinspection ResultOfMethodCallIgnored
                cache.toFile().mkdir();
            }
            Path javaPacks = cache.resolve("javaPacks");
            if (!javaPacks.toFile().exists()) {
                //noinspection ResultOfMethodCallIgnored
                javaPacks.toFile().mkdir();
            }
            Path translatedPacks = cache.resolve("translatedPacks");
            if (!translatedPacks.toFile().exists()) {
                //noinspection ResultOfMethodCallIgnored
                translatedPacks.toFile().mkdir();
            }
            session.getConnector().getGeneralThreadPool().execute(() -> {
                try {
                    Arrays.fill(session.getResourcePackCache().getBedrockResourcePacks(), null);
                    session.getResourcePackCache().getBedrockCustomItems().clear();
                    session.getResourcePackCache().getJavaToCustomModelDataToBedrockId().clear();
                    session.getResourcePackCache().getBedrockCustomIdToProperBedrockId().clear();
                    session.getResourcePackCache().setCustomModelDataActive(false);
                    ResourcePack pack = null;
//                    if (!session.getResourcePackCache().getResourcePackHash().isEmpty()) {
//                        // Search through our translated packs to see if we've already converted this pack
//                        for (File file : translatedPacks.toFile().listFiles()) {
//                            if (file.getName().equals(session.getResourcePackCache().getResourcePackHash() + ".zip")) {
//                                pack = ResourcePack.loadPack(file);
//                                break;
//                            }
//                        }
//                    }
                    if (pack == null) {
                        System.out.println("Downloading resource pack requested by " + session.getName());
                        session.sendMessage(LocaleUtils.getLocaleString("resourcepack.downloading", session.getClientData().getLanguageCode()));
                        String packName;
                        if (session.getResourcePackCache().getResourcePackHash().isEmpty()) {
                            packName = session.getName() + "-" + System.currentTimeMillis() + ".zip";
                        } else {
                            packName = session.getResourcePackCache().getResourcePackHash() + ".zip";
                        }
                        WebUtils.downloadFile(session.getResourcePackCache().getResourcePackUrl(), javaPacks.resolve(packName).toString());
                        PackConverter converter = new PackConverter(javaPacks.resolve(packName),
                                translatedPacks.resolve(packName));
                        converter.convert();
                        converter.pack();
                        // Custom items
                        if (!converter.getCustomModelData().isEmpty()) {
                            // Get the last registered Bedrock index
                            int index = ItemRegistry.ITEMS.size();
                            for (Map.Entry<String, Int2ObjectMap<String>> map : converter.getCustomModelData().entrySet()) {
                                ItemEntry itemEntry = ItemRegistry.getItemEntry("minecraft:" + map.getKey());
                                // Start the registry of Java custom model data ID to Bedrock registered ID
                                Int2IntMap customModelDataToBedrockId = new Int2IntOpenHashMap(map.getValue().size());
                                for (Int2ObjectMap.Entry<String> customModelData : map.getValue().int2ObjectEntrySet()) {
                                    index++;
                                    // Put in our custom Bedrock identifier and the given Bedrock index
                                    session.getResourcePackCache().getBedrockCustomItems().add(new StartGamePacket.ItemEntry(customModelData.getValue(), (short) index));
                                    // Put in the Java custom model data key and the Bedrock index (for item searching)
                                    customModelDataToBedrockId.put(customModelData.getIntKey(), index);
                                    session.getResourcePackCache().getBedrockCustomIdToProperBedrockId().put(index, itemEntry.getBedrockId());
                                }
                                // Put in the final lookup of Java ID to custom model data indexes to Bedrock ID
                                session.getResourcePackCache().getJavaToCustomModelDataToBedrockId().put(
                                        itemEntry.getJavaId(), customModelDataToBedrockId);
                            }
                            session.getResourcePackCache().setBedrockBehaviorPack(ResourcePack.loadPack(translatedPacks.resolve("bp_" + packName).toFile()));
                        }
                        //noinspection ResultOfMethodCallIgnored
                        javaPacks.resolve(packName).toFile().delete();
                        pack = ResourcePack.loadPack(translatedPacks.resolve(packName).toFile());
                    }
                    session.getResourcePackCache().setBedrockResourcePack(pack);
                } catch (Exception e) {
                    e.printStackTrace();
                    ClientResourcePackStatusPacket failPacket = new ClientResourcePackStatusPacket(ResourcePackStatus.FAILED_DOWNLOAD);
                    session.sendDownstreamPacket(failPacket);
                    return;
                }

                String fullAddress = session.getClientData().getServerAddress();
                String address = fullAddress.substring(0, fullAddress.lastIndexOf(":"));
                int port = Integer.parseInt(fullAddress.substring(fullAddress.lastIndexOf(":") + 1));
                TransferPacket transferPacket = new TransferPacket();
                transferPacket.setAddress(address);
                transferPacket.setPort(port);
                session.sendUpstreamPacket(transferPacket);
                session.setTransferring(true);
            });
        }

        return true;
    }

}
