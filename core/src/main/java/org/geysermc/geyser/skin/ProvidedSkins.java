/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.skin;

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.skin.Skin;
import org.geysermc.geyser.util.AssetUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public final class ProvidedSkins {
    private static final ProvidedSkin[] PROVIDED_SKINS = {
            new ProvidedSkin("textures/entity/player/slim/alex.png", true),
            new ProvidedSkin("textures/entity/player/slim/ari.png", true),
            new ProvidedSkin("textures/entity/player/slim/efe.png", true),
            new ProvidedSkin("textures/entity/player/slim/kai.png", true),
            new ProvidedSkin("textures/entity/player/slim/makena.png", true),
            new ProvidedSkin("textures/entity/player/slim/noor.png", true),
            new ProvidedSkin("textures/entity/player/slim/steve.png", true),
            new ProvidedSkin("textures/entity/player/slim/sunny.png", true),
            new ProvidedSkin("textures/entity/player/slim/zuri.png", true),
            new ProvidedSkin("textures/entity/player/wide/alex.png", false),
            new ProvidedSkin("textures/entity/player/wide/ari.png", false),
            new ProvidedSkin("textures/entity/player/wide/efe.png", false),
            new ProvidedSkin("textures/entity/player/wide/kai.png", false),
            new ProvidedSkin("textures/entity/player/wide/makena.png", false),
            new ProvidedSkin("textures/entity/player/wide/noor.png", false),
            new ProvidedSkin("textures/entity/player/wide/steve.png", false),
            new ProvidedSkin("textures/entity/player/wide/sunny.png", false),
            new ProvidedSkin("textures/entity/player/wide/zuri.png", false)
    };

    public static ProvidedSkin getDefaultPlayerSkin(UUID uuid) {
        return PROVIDED_SKINS[Math.floorMod(uuid.hashCode(), PROVIDED_SKINS.length)];
    }

    private ProvidedSkins() {
    }

    public static final class ProvidedSkin {
        private Skin data;
        private final boolean slim;

        ProvidedSkin(String asset, boolean slim) {
            this.slim = slim;

            Path folder = GeyserImpl.getInstance().getBootstrap().getConfigFolder()
                    .resolve("cache")
                    .resolve("default_player_skins")
                    .resolve(slim ? "slim" : "wide");
            String assetName = asset.substring(asset.lastIndexOf('/') + 1);

            Path location = folder.resolve(assetName);
            AssetUtils.addTask(!Files.exists(location), new AssetUtils.ClientJarTask("assets/minecraft/" + asset,
                    (stream) -> AssetUtils.saveFile(location, stream),
                    () -> {
                        try {
                            // TODO lazy initialize?
                            BufferedImage image;
                            try (InputStream stream = Files.newInputStream(location)) {
                                image = ImageIO.read(stream);
                            }

                            byte[] byteData = SkinProvider.bufferedImageToImageData(image);
                            image.flush();

                            String identifier = "geysermc:" + assetName + "_" + (slim ? "slim" : "wide");
                            this.data = new Skin(identifier, byteData, true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
            }));
        }

        public Skin getData() {
            // Fall back to the default skin if we can't load our skins, or it's not loaded yet.
            return Objects.requireNonNullElse(data, SkinProvider.EMPTY_SKIN);
        }

        public boolean isSlim() {
            return slim;
        }
    }

    public static void init() {
        // no-op
    }

    static {
        Path folder = GeyserImpl.getInstance().getBootstrap().getConfigFolder()
                .resolve("cache")
                .resolve("default_player_skins");
        folder.toFile().mkdirs();
        // Two directories since there are two skins for each model: one slim, one wide
        folder.resolve("slim").toFile().mkdir();
        folder.resolve("wide").toFile().mkdir();
    }
}
