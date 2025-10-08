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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.adapters.CommandManagerAdapter;
import org.geysermc.geyser.adapters.PlatformAdapters;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.CommandSourceConverter;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.command.standalone.StandaloneCloudCommandManager;
import org.geysermc.geyser.platform.mod.GeyserModBootstrap;
import org.geysermc.geyser.platform.mod.GeyserModUpdateListener;
import org.geysermc.geyser.platform.mod.command.ModCommandSource;
import org.geysermc.geyser.text.ChatColor;
import org.incendo.cloud.CommandManager;

public class GeyserFabricBootstrap extends GeyserModBootstrap implements ModInitializer {

    public GeyserFabricBootstrap() {
        super(new GeyserFabricPlatform());
    }

    @Override
    public void onInitialize() {
        // We love workarounds! Fabric doesn't allow us to have out adapters init before this, so we'll force it!
        FabricLoader.getInstance().getEntrypointContainers("geyser:adapter", ModInitializer.class)
            .forEach(entrypoint -> entrypoint.getEntrypoint().onInitialize());

        CommandManagerAdapter<?, ?> commandManagerAdapter = PlatformAdapters.getCommandManagerAdapter();

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

        ServerPlayConnectionEvents.JOIN.register((handler, $, $$) -> {
            ServerPlayer player = handler.getPlayer();

            GeyserModUpdateListener.onPlayReady(player, commandManagerAdapter);
        });

        this.onGeyserInitialize();

        CommandManager<GeyserCommandSource> cloud;

        if (commandManagerAdapter != null) {
            cloud = commandManagerAdapter.getCommandManager(
                ModCommandSource::new,
                CommandSourceConverter::layered,
                message -> GeyserImpl.getInstance().getLogger().info(ChatColor.toANSI(message + ChatColor.RESET))
            );
        } else { // Fallback to the standalone manager, this *shouldn't* happen, since there should be an adapter for all versions of Minecraft this will load on
            cloud = new StandaloneCloudCommandManager(GeyserImpl.getInstance());
            GeyserImpl.getInstance().getLogger().warning("No CommandManagerAdapter was found. Permissions will be handled by a standalone file. Commands will only be accessible to Geyser users.");
        }

        this.setCommandRegistry(new CommandRegistry(GeyserImpl.getInstance(), cloud));
    }

    @Override
    public boolean isServer() {
        return FabricLoader.getInstance().getEnvironmentType().equals(EnvType.SERVER);
    }
}
