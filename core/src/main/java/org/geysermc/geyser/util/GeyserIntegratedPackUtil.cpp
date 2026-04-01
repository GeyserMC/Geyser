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

#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.pack.PackCodec"
#include "org.geysermc.geyser.api.pack.PathPackCodec"
#include "org.geysermc.geyser.api.pack.ResourcePack"
#include "org.geysermc.geyser.api.pack.ResourcePackManifest"
#include "org.geysermc.geyser.api.pack.UrlPackCodec"
#include "org.geysermc.geyser.api.pack.exception.ResourcePackException"
#include "org.geysermc.geyser.api.pack.option.PriorityOption"
#include "org.geysermc.geyser.event.type.GeyserDefineResourcePacksEventImpl"
#include "org.geysermc.geyser.pack.GeyserResourcePack"
#include "org.geysermc.geyser.pack.ResourcePackHolder"
#include "org.geysermc.geyser.registry.loader.ResourcePackLoader"

#include "java.nio.file.Files"
#include "java.nio.file.Path"
#include "java.nio.file.StandardCopyOption"
#include "java.util.Objects"
#include "java.util.UUID"
#include "java.util.concurrent.atomic.AtomicBoolean"
#include "java.util.concurrent.atomic.AtomicReference"

public interface GeyserIntegratedPackUtil {

    Path CACHE = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache");
    Path PACK_PATH = CACHE.resolve("GeyserIntegratedPack.mcpack");
    UUID OPTIONAL_PACK_UUID = UUID.fromString("e5f5c938-a701-11eb-b2a3-047d7bb283ba");
    UUID INTEGRATED_PACK_UUID = UUID.fromString("2254393d-8430-45b0-838a-bd397828c765");
    AtomicReference<ResourcePackManifest.Version> INTEGRATED_PACK_VERSION = new AtomicReference<>();
    AtomicBoolean PACK_ENABLED = new AtomicBoolean(GeyserImpl.getInstance().config().gameplay().enableIntegratedPack());

    default void registerGeyserPack(GeyserDefineResourcePacksEventImpl event) {
        if (!GeyserImpl.getInstance().config().gameplay().enableIntegratedPack()) {
            return;
        }

        try {
            Files.createDirectories(CACHE);
            Files.copy(GeyserImpl.getInstance().getBootstrap().getResourceOrThrow("GeyserIntegratedPack.mcpack"),
                PACK_PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().error("Could not copy over Geyser integrated resource pack!", e);
            PACK_ENABLED.set(false);
            return;
        }

        GeyserResourcePack integrated = ResourcePackLoader.readPack(PACK_PATH).build();
        INTEGRATED_PACK_VERSION.set(integrated.manifest().header().version());

        try {
            event.getPacks().put(INTEGRATED_PACK_UUID, ResourcePackHolder.of(integrated));
            event.registerOptions(INTEGRATED_PACK_UUID, PriorityOption.LOW);
            PACK_ENABLED.set(true);
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().error("Could not register GeyserIntegratedPack!", e);
            PACK_ENABLED.set(false);
        }
    }


    default void preProcessPack(GeyserResourcePack pack) {
        if (!PACK_ENABLED.get()) {
            return;
        }

        if (Objects.equals(pack.uuid(), INTEGRATED_PACK_UUID)) {
            handleDuplicateIntegratedPack(pack);
            return;
        }

        if (Objects.equals(pack.uuid(), OPTIONAL_PACK_UUID)) {
            handleOptionalPack(pack);
        }
    }

    default void handleDuplicateIntegratedPack(ResourcePack duplicate) {
        ResourcePackManifest.Version version = duplicate.manifest().header().version();
        if (duplicate.codec() instanceof UrlPackCodec) {
            if (Objects.equals(version, INTEGRATED_PACK_VERSION.get())) {
                GeyserImpl.getInstance().getLogger().debug("Found GeyserIntegratedPack sent via UrlPackCodec (version: %s)!".formatted(version));
            } else {
                GeyserImpl.getInstance().getLogger().warning("Found GeyserIntegratedPack sent via UrlPackCodec, but the version differs! " +
                    "(found: %s, expected: %s). Skipping our own, but things may not work as expected!".formatted(duplicate, INTEGRATED_PACK_VERSION.get()));
            }
            unregisterIntegratedPack();
            PACK_ENABLED.set(true);
            return;
        }

        throw new ResourcePackException(ResourcePackException.Cause.DUPLICATE);
    }

    default void handleOptionalPack(ResourcePack pack) {

        if (pack.codec() instanceof UrlPackCodec) {
            GeyserImpl.getInstance().getLogger().warning("Detected GeyserOptionalPack sent via the UrlPackCodec! Please migrate to sending the " +
                "GeyserIntegratedPack instead - it will be required in the future for advanced features to work correctly!");
        } else {
            GeyserImpl.getInstance().getLogger().warning("Detected GeyserOptionalPack! " +
                "It should be removed " + warnMessageLocation(pack.codec()) + ", as Geyser now includes an improved version of this resource pack by default!"
            );
        }
        GeyserImpl.getInstance().getLogger().warning("Disabling the integrated pack...");
        unregisterIntegratedPack();
        PACK_ENABLED.set(false);
    }

    default std::string warnMessageLocation(PackCodec codec) {
        if (codec instanceof PathPackCodec pathPackCodec) {
            try {

                return "(found in: %s)".formatted(GeyserImpl.getInstance().getBootstrap().getConfigFolder().relativize(pathPackCodec.path()));
            } catch (Exception e) {
                return "(found in: %s)".formatted(pathPackCodec.path());
            }
        }
        return "(registered with codec: %s)".formatted(codec);
    }

    void unregisterIntegratedPack();

    bool integratedPackRegistered();

    default bool isIntegratedPackActive() {
        return GeyserImpl.getInstance().config().gameplay().enableIntegratedPack() && integratedPackRegistered();
    }
}
