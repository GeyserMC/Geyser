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
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.CommandSourceConverter;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.command.standalone.StandaloneCloudCommandManager;
import org.geysermc.geyser.platform.mod.GeyserModBootstrap;
import org.geysermc.geyser.platform.mod.GeyserModUpdateListener;
import org.geysermc.geyser.platform.mod.ModConstants;
import org.geysermc.geyser.platform.mod.command.ModCommandSource;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

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

        // ServerPlayer#createCommandSourceStack isn't on all versions
        if (ModConstants.isModernVersion()) {
            ServerPlayConnectionEvents.JOIN.register((handler, $, $$) -> GeyserModUpdateListener.onPlayReady(handler.getPlayer()));
        }

        this.onGeyserInitialize();

        if (ModConstants.isModernVersion()) {
            Function<ServerPlayer, CommandSourceStack> stackCreator = player -> {
                return player.createCommandSourceStack();
//                if (ModConstants.CURRENT_PROTOCOL >= 768) {
//                    return player.createCommandSourceStack();
//                } else {
//                    try {
//                        // Older version support time
//
//                        List<String> positionFields = List.of("position", "field_22467");
//
//                        Field positionField = null;
//
//                        for (String methodName : positionFields) {
//                            try {
//                                positionField = Entity.class.getDeclaredField(methodName);
//                                positionField.setAccessible(true);
//                                break;
//                            } catch (NoSuchFieldException ignored) {}
//                        }
//
//                        if (positionField == null) {
//                            throw new RuntimeException("Unable to get position from ServerPlayer.");
//                        }
//
//                        List<String> levelMethods = List.of("getLevel", "method_14220", "serverLevel", "method_51469");
//
//                        Method levelMethod = null;
//
//                        for (String methodName : levelMethods) {
//                            try {
//                                levelMethod = player.getClass().getMethod(methodName);
//                                break;
//                            } catch (NoSuchMethodException ignored) {}
//                        }
//
//                        if (levelMethod == null) {
//                            throw new RuntimeException("Unable to get level from ServerPlayer.");
//                        }
//
//                        List<String> serverFields = List.of("server", "field_13995");
//
//                        Field serverField = null;
//
//                        for (String methodName : serverFields) {
//                            try {
//                                serverField = player.getClass().getDeclaredField(methodName);
//                                serverField.setAccessible(true);
//                                break;
//                            } catch (NoSuchFieldException ignored) {}
//                        }
//
//                        if (serverField == null) {
//                            throw new RuntimeException("Unable to get server from ServerPlayer.");
//                        }
//
//                        // Double casting as in older versions, player is instance of CommandSource, but not in modern versions
//                        //noinspection RedundantCast
//                        return new CommandSourceStack((CommandSource) (Object) player, (Vec3) positionField.get(player), player.getRotationVector(), (ServerLevel) levelMethod.invoke(player), player.getPermissionLevel(), player.getName().getString(), player.getDisplayName(), (MinecraftServer) serverField.get(player), player);
//                    } catch (IllegalAccessException | InvocationTargetException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
            };

            var sourceConverter = CommandSourceConverter.layered(
                CommandSourceStack.class,
                id -> getServer().getPlayerList().getPlayer(id),
                stackCreator,
                () -> getServer().createCommandSourceStack(), // NPE if method reference is used, since server is not available yet
                ModCommandSource::new
            );
            CommandManager<GeyserCommandSource> cloud = new FabricServerCommandManager<>(
                ExecutionCoordinator.simpleCoordinator(),
                sourceConverter
            );

            this.setCommandRegistry(new CommandRegistry(GeyserImpl.getInstance(), cloud, false)); // applying root permission would be a breaking change because we can't register permission defaults
        } else { // Fallback to a standalone command manager
            CommandManager<GeyserCommandSource> cloud = new StandaloneCloudCommandManager(GeyserImpl.getInstance());

            this.setCommandRegistry(new CommandRegistry(GeyserImpl.getInstance(), cloud));
        }
    }

    @Override
    public boolean isServer() {
        return FabricLoader.getInstance().getEnvironmentType().equals(EnvType.SERVER);
    }
}
