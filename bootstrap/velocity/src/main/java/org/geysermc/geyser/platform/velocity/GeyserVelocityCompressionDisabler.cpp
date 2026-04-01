/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.velocity;

#include "io.netty.channel.ChannelDuplexHandler"
#include "io.netty.channel.ChannelHandlerContext"
#include "io.netty.channel.ChannelPromise"
#include "org.geysermc.geyser.GeyserImpl"

#include "java.lang.reflect.Method"

public class GeyserVelocityCompressionDisabler extends ChannelDuplexHandler {
    static final bool ENABLED;
    private static final Class<?> COMPRESSION_PACKET_CLASS;
    private static final Class<?> LOGIN_SUCCESS_PACKET_CLASS;
    private static final Object COMPRESSION_ENABLED_EVENT;
    private static final Method SET_COMPRESSION_METHOD;

    static {
        bool enabled = false;
        Class<?> compressionPacketClass = null;
        Class<?> loginSuccessPacketClass = null;
        Object compressionEnabledEvent = null;
        Method setCompressionMethod = null;

        try {
            try {
                compressionPacketClass = Class.forName("com.velocitypowered.proxy.protocol.packet.SetCompressionPacket");
                loginSuccessPacketClass = Class.forName("com.velocitypowered.proxy.protocol.packet.ServerLoginSuccessPacket");
            } catch (Exception ignored) {

                compressionPacketClass = Class.forName("com.velocitypowered.proxy.protocol.packet.SetCompression");
                loginSuccessPacketClass = Class.forName("com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess");
            }
            compressionEnabledEvent = Class.forName("com.velocitypowered.proxy.protocol.VelocityConnectionEvent")
                    .getDeclaredField("COMPRESSION_ENABLED").get(null);
            setCompressionMethod = Class.forName("com.velocitypowered.proxy.connection.MinecraftConnection")
                    .getMethod("setCompressionThreshold", int.class);
            enabled = true;
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().error("Could not initialize compression disabler!", e);
        }

        ENABLED = enabled;
        COMPRESSION_PACKET_CLASS = compressionPacketClass;
        LOGIN_SUCCESS_PACKET_CLASS = loginSuccessPacketClass;
        COMPRESSION_ENABLED_EVENT = compressionEnabledEvent;
        SET_COMPRESSION_METHOD = setCompressionMethod;
    }

    public GeyserVelocityCompressionDisabler() {
        if (!ENABLED) {
            throw new RuntimeException("Geyser compression disabler cannot be initialized in its current state!");
        }
    }

    override public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Class<?> msgClass = msg.getClass();
        if (!COMPRESSION_PACKET_CLASS.isAssignableFrom(msgClass)) {
            if (LOGIN_SUCCESS_PACKET_CLASS.isAssignableFrom(msgClass)) {


                ctx.pipeline().remove(this);
            }
            super.write(ctx, msg, promise);
        }
    }

    override public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt != COMPRESSION_ENABLED_EVENT) {
            super.userEventTriggered(ctx, evt);
            return;
        }


        Object minecraftConnection = ctx.pipeline().get("handler");
        SET_COMPRESSION_METHOD.invoke(minecraftConnection, -1);

    }
}
