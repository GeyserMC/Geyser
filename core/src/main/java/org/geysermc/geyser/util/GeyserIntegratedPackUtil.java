/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.PathPackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.UrlPackCodec;
import org.geysermc.geyser.api.pack.option.PriorityOption;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;
import org.geysermc.geyser.event.type.GeyserDefineResourcePacksEventImpl;
import org.geysermc.geyser.pack.ResourcePackHolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public interface GeyserIntegratedPackUtil {

    GeyserImpl instance = GeyserImpl.getInstance();
    UUID PACK_UUID = UUID.fromString("e5f5c938-a701-11eb-b2a3-047d7bb283ba");
    Path CACHE = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache");
    Path PACK_PATH = CACHE.resolve("GeyserIntegratedPack.mcpack");
    AtomicBoolean PACK_ENABLED = new AtomicBoolean(false);

    default void registerGeyserPack(GeyserDefineResourcePacksEventImpl event) {
        if (!instance.config().gameplay().enableIntegratedPack()) {
            return;
        }

        var pack = event.getPacks().get(PACK_UUID);
        if (pack != null) {
            if (pack.codec() instanceof UrlPackCodec) {
                // Must ensure correct place in pack stack
                pack.optionHolder().put(ResourcePackOption.Type.PRIORITY, PriorityOption.HIGH);
                instance.getLogger().info("Not adding our own copy of the integrated pack due to url pack codec presence!");
                PACK_ENABLED.set(true);
                return;
            }
            warnOptionalPackPresent(warnMessageLocation(pack.codec()));
            getPacks().remove(PACK_UUID);
        }

        try {
            Files.createDirectories(CACHE);
            Files.copy(GeyserImpl.getInstance().getBootstrap().getResourceOrThrow("GeyserIntegratedPack.mcpack"),
                PACK_PATH, StandardCopyOption.REPLACE_EXISTING);
            event.register(ResourcePack.create(PathPackCodec.path(PACK_PATH)), PriorityOption.HIGH);
            PACK_ENABLED.set(true);
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().error("Could not copy over Geyser integrated resource pack!", e);
        }
    }

    default boolean handlePossibleOptionalPack(ResourcePack pack) {
        if (!PACK_ENABLED.get()) {
            return false;
        }
        if (!Objects.equals(pack.uuid(), PACK_UUID)) {
            return false;
        }
        if (pack.codec() instanceof UrlPackCodec) {
            getPacks().remove(PACK_UUID);
            instance.getLogger().info("Overriding our own integrated pack with url pack codec delivered pack!");
            return false;
        }
        warnOptionalPackPresent(warnMessageLocation(pack.codec()));
        return true;
    }

    Map<UUID, ResourcePackHolder> getPacks();

    default String warnMessageLocation(PackCodec codec) {
        if (codec instanceof PathPackCodec pathPackCodec) {
            return "(found in: %s)".formatted(instance.getBootstrap().getConfigFolder().relativize(pathPackCodec.path()));
        }
        return "(registered with codec: %s)".formatted(codec);
    }

    default void warnOptionalPackPresent(String message) {
        instance.getLogger().warning("Detected duplicate GeyserOptionalPack registration! " +
            " It should be removed " + message + ", as Geyser now includes an improved version of this resource pack by default!"
        );
    }

    default boolean isIntegratedPackActive() {
        return instance.config().gameplay().enableIntegratedPack() && getPacks().containsKey(PACK_UUID);
    }
}
