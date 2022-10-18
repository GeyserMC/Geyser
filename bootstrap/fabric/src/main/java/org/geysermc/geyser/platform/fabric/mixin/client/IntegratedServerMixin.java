/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.fabric.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import org.geysermc.geyser.platform.fabric.GeyserFabricMod;
import org.geysermc.geyser.platform.fabric.GeyserServerPortGetter;
import org.geysermc.geyser.text.GeyserLocale;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(IntegratedServer.class)
public class IntegratedServerMixin implements GeyserServerPortGetter {
    @Shadow
    private int publishedPort;

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "publishServer", at = @At("RETURN"))
    private void onOpenToLan(GameType gameType, boolean cheatsAllowed, int port, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            // If the LAN is opened, starts Geyser.
            GeyserFabricMod.getInstance().startGeyser((MinecraftServer) (Object) this);
            // Ensure player locale has been loaded, in case it's different from Java system language
            GeyserLocale.loadGeyserLocale(this.minecraft.options.languageCode);
            // Give indication that Geyser is loaded
            this.minecraft.player.displayClientMessage(Component.literal(GeyserLocale.getPlayerLocaleString("geyser.core.start",
                    this.minecraft.options.languageCode, "localhost", String.valueOf(this.publishedPort))), false);
        }
    }

    @Override
    public int geyser$getServerPort() {
        return this.publishedPort;
    }
}
