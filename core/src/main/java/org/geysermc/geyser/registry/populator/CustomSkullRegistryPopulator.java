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

package org.geysermc.geyser.registry.populator;

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.configuration.GeyserCustomSkullConfiguration;
import org.geysermc.geyser.pack.SkullResourcePackManager;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.CustomSkull;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.geyser.skin.SkinProvider;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class CustomSkullRegistryPopulator {

    public static void populate() {
        SkullResourcePackManager.SKULL_SKINS.clear(); // Remove skins after reloading
        BlockRegistries.CUSTOM_SKULLS.set(Object2ObjectMaps.emptyMap());

        if (!GeyserImpl.getInstance().getConfig().isAddCustomSkullBlocks()) {
            return;
        }

        GeyserCustomSkullConfiguration skullConfig;
        try {
            GeyserBootstrap bootstrap = GeyserImpl.getInstance().getBootstrap();
            Path skullConfigPath = bootstrap.getConfigFolder().resolve("custom-skulls.yml");
            File skullConfigFile = FileUtils.fileOrCopiedFromResource(skullConfigPath.toFile(), "custom-skulls.yml", Function.identity(), bootstrap);
            skullConfig = FileUtils.loadConfig(skullConfigFile, GeyserCustomSkullConfiguration.class);
        } catch (IOException e) {
            GeyserImpl.getInstance().getLogger().severe(GeyserLocale.getLocaleStringLog("geyser.config.failed"), e);
            return;
        }

        BlockRegistries.CUSTOM_SKULLS.set(new Object2ObjectOpenHashMap<>());

        List<String> textures = new ArrayList<>(skullConfig.getTextures());
        // TODO see if this can be cleaned up any better
        for (String username : skullConfig.getPlayerUsernames()) {
            try {
                String texture = SkinProvider.requestTexturesFromUsername(username).get();
                if (texture == null) {
                    GeyserImpl.getInstance().getLogger().error("Unable to request skull textures for " + username + " This skull will not be added as a custom block.");
                    continue;
                }
                textures.add(texture);
            } catch (InterruptedException | ExecutionException e) {
                GeyserImpl.getInstance().getLogger().error("Unable to request skull textures for " + username + " This skull will not be added as a custom block.", e);
            }
        }
        for (String uuid : skullConfig.getPlayerUUIDs()) {
            try {
                String uuidDigits = uuid.replace("-", "");
                if (uuidDigits.length() != 32) {
                    GeyserImpl.getInstance().getLogger().error("Invalid skull uuid " + uuid + " This skull will not be added as a custom block.");
                    continue;
                }
                String texture = SkinProvider.requestTexturesFromUUID(uuid).get();
                if (texture == null) {
                    GeyserImpl.getInstance().getLogger().error("Unable to request skull textures for " + uuid + " This skull will not be added as a custom block.");
                    continue;
                }
                textures.add(texture);
            } catch (InterruptedException | ExecutionException e) {
                GeyserImpl.getInstance().getLogger().error("Unable to request skull textures for " + uuid + " This skull will not be added as a custom block.", e);
            }
        }

        for (String texture : textures) {
            try {
                SkinManager.GameProfileData profileData = SkinManager.GameProfileData.loadFromJson(texture);
                if (profileData == null) {
                    GeyserImpl.getInstance().getLogger().warning("Skull texture " + texture + " contained no skins and will not be added as a custom block.");
                    continue;
                }
                try {
                    String skinUrl = profileData.skinUrl();
                    String skinHash = skinUrl.substring(skinUrl.lastIndexOf("/") + 1);
                    SkullResourcePackManager.cacheSkullSkin(skinUrl, skinHash);
                    BlockRegistries.CUSTOM_SKULLS.register(skinHash, new CustomSkull(skinHash));
                } catch (IOException e) {
                    GeyserImpl.getInstance().getLogger().error("Failed to cache skin for skull texture " + texture + " This skull will not be added as a custom block.", e);
                }
            } catch (IOException e) {
                GeyserImpl.getInstance().getLogger().error("Skull texture " + texture + " is invalid and will not be added as a custom block.", e);
            }
        }

        GeyserImpl.getInstance().getLogger().debug("Registered " + BlockRegistries.CUSTOM_SKULLS.get().size() + " custom skulls as custom blocks.");
    }
}
