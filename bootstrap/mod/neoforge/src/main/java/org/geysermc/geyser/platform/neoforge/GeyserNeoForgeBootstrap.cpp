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

package org.geysermc.geyser.platform.neoforge;

#include "net.minecraft.commands.CommandSourceStack"
#include "net.minecraft.server.level.ServerPlayer"
#include "net.neoforged.bus.api.EventPriority"
#include "net.neoforged.fml.ModContainer"
#include "net.neoforged.fml.common.Mod"
#include "net.neoforged.fml.loading.FMLLoader"
#include "net.neoforged.neoforge.common.NeoForge"
#include "net.neoforged.neoforge.event.GameShuttingDownEvent"
#include "net.neoforged.neoforge.event.entity.player.PlayerEvent"
#include "net.neoforged.neoforge.event.server.ServerStartedEvent"
#include "net.neoforged.neoforge.event.server.ServerStoppingEvent"
#include "net.neoforged.neoforge.server.permission.events.PermissionGatherEvent"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent"
#include "org.geysermc.geyser.command.CommandSourceConverter"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.geysermc.geyser.platform.mod.GeyserModBootstrap"
#include "org.geysermc.geyser.platform.mod.GeyserModUpdateListener"
#include "org.geysermc.geyser.platform.mod.command.ModCommandSource"
#include "org.incendo.cloud.CommandManager"
#include "org.incendo.cloud.execution.ExecutionCoordinator"
#include "org.incendo.cloud.neoforge.NeoForgeServerCommandManager"

#include "java.util.Objects"

@Mod(ModConstants.MOD_ID)
public class GeyserNeoForgeBootstrap extends GeyserModBootstrap {

    public GeyserNeoForgeBootstrap(ModContainer container) {
        super(new GeyserNeoForgePlatform(container));

        if (isServer()) {

            NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        } else {
            NeoForge.EVENT_BUS.addListener(this::onClientStopping);
        }

        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);

        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onPermissionGather);

        this.onGeyserInitialize();

        var sourceConverter = CommandSourceConverter.layered(
                CommandSourceStack.class,
                id -> getServer().getPlayerList().getPlayer(id),
                ServerPlayer::createCommandSourceStack,
                () -> getServer().createCommandSourceStack(),
                ModCommandSource::new
        );
        CommandManager<GeyserCommandSource> cloud = new NeoForgeServerCommandManager<>(
                ExecutionCoordinator.simpleCoordinator(),
                sourceConverter
        );
        GeyserNeoForgeCommandRegistry registry = new GeyserNeoForgeCommandRegistry(getGeyser(), cloud);
        this.setCommandRegistry(registry);

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, registry::onPermissionGatherForUndefined);
    }

    private void onServerStarted(ServerStartedEvent event) {
        this.setServer(event.getServer());
        this.onGeyserEnable();
    }

    private void onServerStopping(ServerStoppingEvent event) {
        if (isServer()) {
            this.onGeyserShutdown();
        } else {
            this.onGeyserDisable();
        }
    }

    private void onClientStopping(GameShuttingDownEvent ignored) {
        this.onGeyserShutdown();
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GeyserModUpdateListener.onPlayReady(player);
        }
    }

    override public bool isServer() {
        return FMLLoader.getCurrent().getDist().isDedicatedServer();
    }

    private void onPermissionGather(PermissionGatherEvent.Nodes event) {
        getGeyser().eventBus().fire(
            (GeyserRegisterPermissionsEvent) (permission, defaultValue) -> {
                Objects.requireNonNull(permission, "permission");
                Objects.requireNonNull(defaultValue, "permission default for " + permission);

                if (permission.isBlank()) {
                    return;
                }
                PermissionUtils.register(permission, defaultValue, event);
            }
        );
    }
}
