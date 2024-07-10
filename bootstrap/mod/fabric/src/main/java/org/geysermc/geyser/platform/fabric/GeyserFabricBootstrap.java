/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.platform.mod.GeyserModBootstrap;
import org.geysermc.geyser.platform.mod.GeyserModUpdateListener;

public class GeyserFabricBootstrap extends GeyserModBootstrap implements ModInitializer {

    public GeyserFabricBootstrap() {
        super(new GeyserFabricPlatform());
    }

    @Override
    public void onInitialize() {
        if (isServer()) {
            // Set as an event, so we can get the proper IP and port if needed
            ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
                this.setServer(server);
                onGeyserEnable();
            });
        } else {
            ClientLifecycleEvents.CLIENT_STOPPING.register(($)-> {
                onGeyserShutdown();
            });
        }

        // These are only registered once
        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
            if (isServer()) {
                onGeyserShutdown();
            } else {
                onGeyserDisable();
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, $, $$) -> GeyserModUpdateListener.onPlayReady(handler.getPlayer()));

        this.onGeyserInitialize();
    }

    @Override
    public boolean isServer() {
        return FabricLoader.getInstance().getEnvironmentType().equals(EnvType.SERVER);
    }

    @Override
    public boolean hasPermission(@NonNull Player source, @NonNull String permissionNode) {
        return Permissions.check(source, permissionNode);
    }

    @Override
    public boolean hasPermission(@NonNull CommandSourceStack source, @NonNull String permissionNode, int permissionLevel) {
        return Permissions.check(source, permissionNode, permissionLevel);
    }
}
