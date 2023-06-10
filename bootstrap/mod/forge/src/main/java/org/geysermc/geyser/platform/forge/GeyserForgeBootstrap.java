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

package org.geysermc.geyser.platform.forge;

import com.google.common.reflect.ClassPath;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.geysermc.geyser.platform.mod.GeyserModBootstrap;
import org.geysermc.geyser.platform.mod.GeyserModUpdateListener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

@Mod(ModConstants.MOD_ID)
public class GeyserForgeBootstrap extends GeyserModBootstrap {

    static {
        // This is... not great but Forge's classloader is incredibly picky
        // and will error if classes are loaded outside certain contexts

        // See: https://github.com/MinecraftForge/EventBus/issues/44
        // preload("com.fasterxml.jackson.core");
        // preload("com.fasterxml.jackson.databind");
    }

    public GeyserForgeBootstrap() {
        super(new GeyserForgePlatform());

        this.onEnable();

        if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
            // Set as an event so we can get the proper IP and port if needed
            MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        }
    }

    @Override
    public void onInitialStartup() {
        // Server has yet to start
        // Register onDisable so players are properly kicked
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
    }

    private void onServerStarted(ServerStartedEvent event) {
        this.startGeyser(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        this.onDisable();
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        GeyserModUpdateListener.onPlayReady(event.getEntity());
    }

    private static void preload(String packageName) {
        try {
            ClassPath.from(GeyserForgeBootstrap.class.getClassLoader())
                    .getTopLevelClasses(packageName).forEach(info -> {
                        try {
                            Class.forName(info.getName());
                        } catch (ClassNotFoundException ignored) {
                            // Shouldn't happen
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public ExecutorService platformExecutor() {
        return Util.backgroundExecutor();
    }
}
