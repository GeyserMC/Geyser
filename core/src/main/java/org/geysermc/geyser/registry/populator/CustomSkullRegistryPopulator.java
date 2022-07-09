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
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.pack.SkullResourcePackManager;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.CustomSkull;
import org.geysermc.geyser.skin.SkinManager;

import java.io.IOException;
import java.util.Map;

public class CustomSkullRegistryPopulator {

    public static void populate() {
        SkullResourcePackManager.SKULL_SKINS.clear(); // Remove skins after reloading
        if (!GeyserImpl.getInstance().getConfig().isAddCustomSkullBlocks()) {
            BlockRegistries.CUSTOM_SKULLS.set(Object2ObjectMaps.emptyMap());
            return;
        }

        Map<String, CustomSkull> customSkulls = new Object2ObjectOpenHashMap<>();
        for (String skullProfile : GeyserImpl.getInstance().getConfig().getCustomSkullProfiles()) {
            try {
                SkinManager.GameProfileData profileData = SkinManager.GameProfileData.loadFromJson(skullProfile);
                if (profileData == null) {
                    GeyserImpl.getInstance().getLogger().warning("Skull profile " + skullProfile + " contained no skins and will not be added as a custom block.");
                    continue;
                }
                try {
                    String skinUrl = profileData.skinUrl();
                    String skinHash = skinUrl.substring(skinUrl.lastIndexOf("/") + 1);
                    SkullResourcePackManager.cacheSkullSkin(skinUrl, skinHash);
                    customSkulls.put(skinHash, new CustomSkull(skinHash));
                } catch (IOException e) {
                    GeyserImpl.getInstance().getLogger().error("Failed to cache skin for skull profile " + skullProfile + " This skull will not be added as a custom block.", e);
                }
            } catch (IOException e) {
                GeyserImpl.getInstance().getLogger().error("Skull profile " + skullProfile + " is invalid and will not be added as a custom block.", e);
            }
        }
        GeyserImpl.getInstance().getLogger().debug("Registered " + customSkulls.size() + " custom skulls as custom blocks.");
        BlockRegistries.CUSTOM_SKULLS.set(customSkulls);
    }
}
