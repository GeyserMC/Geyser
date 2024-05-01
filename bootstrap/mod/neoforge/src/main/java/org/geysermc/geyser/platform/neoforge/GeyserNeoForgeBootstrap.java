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

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.platform.mod.GeyserModBootstrap;
import org.geysermc.geyser.platform.mod.GeyserModUpdateListener;

@Mod(ModConstants.MOD_ID)
public class GeyserNeoForgeBootstrap extends GeyserModBootstrap {

    private final GeyserNeoForgePermissionHandler permissionHandler = new GeyserNeoForgePermissionHandler();

    public GeyserNeoForgeBootstrap() {
        super(new GeyserNeoForgePlatform());

        if (isServer()) {
            // Set as an event so we can get the proper IP and port if needed
            NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        } else {
            NeoForge.EVENT_BUS.addListener(this::onClientStopping);
        }

        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this.permissionHandler::onPermissionGather);

        this.onGeyserInitialize();
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
        GeyserModUpdateListener.onPlayReady(event.getEntity());
    }

    @Override
    public boolean isServer() {
        return FMLLoader.getDist().isDedicatedServer();
    }

    @Override
    public boolean hasPermission(@NonNull Player source, @NonNull String permissionNode) {
        return this.permissionHandler.hasPermission(source, permissionNode);
    }

    @Override
    public boolean hasPermission(@NonNull CommandSourceStack source, @NonNull String permissionNode, int permissionLevel) {
        return this.permissionHandler.hasPermission(source, permissionNode, permissionLevel);
    }
}
