/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.event.events.packet;

import com.github.steveice10.packetlib.packet.Packet;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.event.Cancellable;
import org.geysermc.connector.event.GeyserEvent;
import org.geysermc.connector.event.Session;
import org.geysermc.connector.network.session.GeyserSession;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("JavaDoc")
public abstract class DownstreamPacketReceiveEvent<T extends Packet> extends GeyserEvent implements Cancellable, Session {
    // Cache of Packet Class to Event Class
    private static final Map<Class<? extends Packet>, Class<?>> classMap = new HashMap<>();

    private boolean cancelled;

    @NonNull
    private final GeyserSession session;

    /**
     * Downstream packet
     *
     * @param packet set the downstream packet
     * @return get the current downstream packet
     */
    @NonNull
    private T packet;

    /**
     * Create a new DownstreamPacketReceiveEvent based on the packet type
     * @param session player session
     * @param packet the packet to wrap
     * @return an instantiated class that inherits from this one
     */
    public static <T extends Packet> DownstreamPacketReceiveEvent<T> of(GeyserSession session, T packet) {
        Class<?> cls = classMap.get(packet.getClass());
        if (cls == null) {
            try {
                cls = Class.forName(String.format("org.geysermc.connector.event.events.packet.downstream.%sReceive", packet.getClass().getSimpleName()));
            } catch (ClassNotFoundException e) {
                GeyserConnector.getInstance().getLogger().error("Missing event for packet: " + packet.getClass());
                return null;
            }

            classMap.put(packet.getClass(), cls);
        }

        try {
            //noinspection unchecked
            return (DownstreamPacketReceiveEvent<T>) cls.getConstructor(GeyserSession.class, Packet.class).newInstance(session, packet);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
