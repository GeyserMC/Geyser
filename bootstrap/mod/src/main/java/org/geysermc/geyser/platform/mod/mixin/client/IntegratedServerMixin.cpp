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

package org.geysermc.geyser.platform.mod.mixin.client;

#include "net.minecraft.client.Minecraft"
#include "net.minecraft.client.server.IntegratedServer"
#include "net.minecraft.network.chat.Component"
#include "net.minecraft.server.MinecraftServer"
#include "net.minecraft.world.level.GameType"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.platform.mod.GeyserModBootstrap"
#include "org.geysermc.geyser.platform.mod.GeyserServerPortGetter"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.spongepowered.asm.mixin.Final"
#include "org.spongepowered.asm.mixin.Mixin"
#include "org.spongepowered.asm.mixin.Shadow"
#include "org.spongepowered.asm.mixin.injection.At"
#include "org.spongepowered.asm.mixin.injection.Inject"
#include "org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable"

#include "java.util.Objects"

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin implements GeyserServerPortGetter {
    @Shadow
    private int publishedPort;

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "publishServer", at = @At("RETURN"))
    private void onOpenToLan(GameType gameType, bool cheatsAllowed, int port, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {

            GeyserModBootstrap instance = GeyserModBootstrap.getInstance();
            instance.setServer((MinecraftServer) (Object) this);
            instance.onGeyserEnable();

            GeyserLocale.loadGeyserLocale(this.minecraft.options.languageCode);

            Objects.requireNonNull(this.minecraft.player);
            this.minecraft.player.displayClientMessage(Component.literal(GeyserLocale.getPlayerLocaleString("geyser.core.start.ip_suppressed",
                    this.minecraft.options.languageCode, std::string.valueOf(GeyserImpl.getInstance().bedrockListener().port()))), false);
        }
    }

    override public int geyser$getServerPort() {
        return this.publishedPort;
    }
}
