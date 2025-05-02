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

package org.geysermc.geyser.platform.fabric.gametest.mixin;

import com.mojang.datafixers.DataFixer;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.geysermc.geyser.platform.mod.GeyserServerPortGetter;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;

@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin extends MinecraftServer implements GeyserServerPortGetter {

    @Shadow
    @Final
    private static Logger LOGGER;

    public GameTestServerMixin(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem,
                               Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory) {
        super(thread, levelStorageAccess, packRepository, worldStem, proxy, dataFixer, services, chunkProgressListenerFactory);
    }

    @Override
    public int geyser$getServerPort() {
        return this.getPort();
    }

    @Inject(method = "initServer", at = @At("HEAD"), cancellable = true)
    public void startTcpServer(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        // A bit of copying from dedicated server code
        InetAddress address = InetAddress.getLoopbackAddress();
        if (getPort() < 0) {
            setPort(25565);
        }
        LOGGER.info("Starting gametest server on {}:{}", address, getPort());
        LOGGER.info("Geyser's tests will start once a bedrock player connects!");

        try {
            this.getConnection().startTcpServerListener(address, getPort());
        } catch (IOException exception) {
            LOGGER.warn("**** FAILED TO BIND TO PORT!");
            LOGGER.warn("The exception was: {}", exception.toString());
            LOGGER.warn("Perhaps a server is already running on that port?");
            callbackInfoReturnable.setReturnValue(false);
        }
    }
}
