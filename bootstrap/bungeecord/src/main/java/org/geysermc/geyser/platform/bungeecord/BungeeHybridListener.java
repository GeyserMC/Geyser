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

package org.geysermc.geyser.platform.bungeecord;

import io.netty.channel.Channel;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.packet.Handshake;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.player.FloodgatePlayerImpl;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.ReflectionUtils;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.hybrid.IntegratedHybridProvider;
import org.geysermc.geyser.hybrid.ProxyHybridProvider;

import java.lang.reflect.Field;

import static com.google.common.base.Preconditions.checkNotNull;

public final class BungeeHybridListener implements Listener {
    // TODO consolidate with Floodgate
    private static final Field CHANNEL_WRAPPER;
    private static final Field PLAYER_NAME;

    static {
        CHANNEL_WRAPPER =
                ReflectionUtils.getFieldOfType(InitialHandler.class, ChannelWrapper.class);
        checkNotNull(CHANNEL_WRAPPER, "ChannelWrapper field cannot be null");

        PLAYER_NAME = ReflectionUtils.getField(InitialHandler.class, "name");
        checkNotNull(PLAYER_NAME, "Initial name field cannot be null");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(PreLoginEvent event) {
        // well, no reason to check if the player will be kicked anyway
        if (event.isCancelled()) {
            return;
        }

        PendingConnection connection = event.getConnection();
        Connection player = getPlayer(connection);
        if (player != null) {
            connection.setOnlineMode(false);
            connection.setUniqueId(player.javaUuid());
            ReflectionUtils.setValue(connection, PLAYER_NAME, player.javaUsername());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onServerConnect(ServerConnectEvent event) {
        boolean sendFloodgateData = false; // TODO
        if (!sendFloodgateData) {
            return; // TODO just don't register event?
        }

        PendingConnection connection = event.getPlayer().getPendingConnection();
        Connection player = getPlayer(connection);
        if (player != null) {
            Handshake handshake = ReflectionUtils.getCastedValue(connection, "handshake");
            BedrockData data = ((FloodgatePlayerImpl) player).toBedrockData(); // FIXME
            String encryptedData = ((ProxyHybridProvider) GeyserImpl.getInstance().getHybridProvider())
                    .createEncryptedDataString(data);

            String address = handshake.getHost();

            // our data goes before all the other data
            int addressFinished = address.indexOf('\0');
            String originalAddress;
            String remaining;
            if (addressFinished != -1) {
                originalAddress = address.substring(0, addressFinished);
                remaining = address.substring(addressFinished);
            } else {
                originalAddress = address;
                remaining = "";
            }

            handshake.setHost(originalAddress + '\0' + encryptedData + remaining);
            // Bungeecord will add its data after our data
        }
    }

    @Nullable
    private Connection getPlayer(PendingConnection connection) {
        ChannelWrapper wrapper = ReflectionUtils.getCastedValue(connection, CHANNEL_WRAPPER);
        Channel channel = wrapper.getHandle();

        return channel.attr(IntegratedHybridProvider.SESSION_KEY).get(); // TODO re-use Floodgate's attribute key here?
    }
}
