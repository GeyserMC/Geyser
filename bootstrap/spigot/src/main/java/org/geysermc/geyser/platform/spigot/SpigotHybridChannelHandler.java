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

package org.geysermc.geyser.platform.spigot;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.spigot.util.ClassNames;
import org.geysermc.geyser.floodgate.IntegratedFloodgateProvider;

import javax.annotation.Nonnull;

import static org.geysermc.floodgate.core.util.ReflectionUtils.setValue;

@ChannelHandler.Sharable
public final class SpigotHybridChannelHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(@Nonnull ChannelHandlerContext ctx, @Nonnull Object packet) throws Exception {
        Connection session = ctx.channel().attr(IntegratedFloodgateProvider.SESSION_KEY).get();
        // TODO generify this code within Floodgate
        if (ClassNames.LOGIN_START_PACKET.isInstance(packet)) {
            Object networkManager = ctx.channel().pipeline().get("packet_handler");
            Object packetListener = ClassNames.PACKET_LISTENER.get(networkManager);

            setValue(networkManager, "spoofedUUID", session.javaUuid());

            // check if the server is actually in the Login state
            if (!ClassNames.LOGIN_LISTENER.isInstance(packetListener)) {
                // player is not in the login state, abort

                // I would've liked to close the channel for security reasons, but our big friend
                // ProtocolSupport, who likes to break things, doesn't work otherwise
                ctx.pipeline().remove(this);
                return;
            }

            // set the player his GameProfile, we can't change the username without this
            GameProfile gameProfile = new GameProfile(
                    // TODO testing only
                    session.javaUuid(), session.javaUsername()
            );
            setValue(packetListener, ClassNames.LOGIN_PROFILE, gameProfile);

            // we have to fake the offline player (login) cycle
            // just like on Spigot:

            // LoginListener#initUUID
            // new LoginHandler().fireEvents();

            // and the tick of LoginListener will do the rest

            ClassNames.INIT_UUID.invoke(packetListener);

            Object loginHandler = ClassNames.LOGIN_HANDLER_CONSTRUCTOR.newInstance(packetListener);
            ClassNames.FIRE_LOGIN_EVENTS.invoke(loginHandler);

            ctx.pipeline().remove(this);
            return;
        }
        ctx.fireChannelRead(packet);
    }
}
