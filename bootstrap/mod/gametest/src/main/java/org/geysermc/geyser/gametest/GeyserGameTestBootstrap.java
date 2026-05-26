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

package org.geysermc.geyser.gametest;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.CommandSourceConverter;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.platform.mod.GeyserModBootstrap;
import org.geysermc.geyser.platform.mod.GeyserModUpdateListener;
import org.geysermc.geyser.platform.mod.command.ModCommandSource;
import org.geysermc.geyser.util.metrics.MetricsPlatform;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;

public class GeyserGameTestBootstrap extends GeyserModBootstrap implements ModInitializer {

    public GeyserGameTestBootstrap() {
        super(new GeyserGameTestPlatform());
    }

    @Override
    public void onInitialize() {
        GeyserGameTestTypes.bootstrap();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            // client == datagen, don't run Geyser
            return;
        }

        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
            onGeyserShutdown();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, _, _) -> {
            GeyserModUpdateListener.onPlayReady(handler.getPlayer());
        });

        this.onGeyserInitialize();


        var sourceConverter = CommandSourceConverter.layered(
                CommandSourceStack.class,
                id -> getServer().getPlayerList().getPlayer(id),
                ServerPlayer::createCommandSourceStack,
                () -> getServer().createCommandSourceStack(), // NPE if method reference is used, since server is not available yet
                ModCommandSource::new
        );
        CommandManager<GeyserCommandSource> cloud = new FabricServerCommandManager<>(
                ExecutionCoordinator.simpleCoordinator(),
                sourceConverter
        );
        this.setCommandRegistry(new CommandRegistry(GeyserImpl.getInstance(), cloud, false)); // applying root permission would be a breaking change because we can't register permission defaults

        // Have to manually start Geyser for game test environment
        GeyserImpl.start();
    }

    @Override
    public boolean isServer() {
        return false;
    }

    @Override
    public @Nullable MetricsPlatform createMetricsPlatform() {
        return null;
    }
}
