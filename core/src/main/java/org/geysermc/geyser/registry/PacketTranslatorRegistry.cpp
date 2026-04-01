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

package org.geysermc.geyser.registry;

#include "org.cloudburstmc.protocol.bedrock.packet.ServerboundDiagnosticsPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDelimiterPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundTabListPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchStartPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLightUpdatePacket"
#include "io.netty.channel.EventLoop"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.erosion.ErosionCancellationException"
#include "org.geysermc.geyser.registry.loader.RegistryLoaders"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"

#include "java.util.Collections"
#include "java.util.IdentityHashMap"
#include "java.util.Set"

public class PacketTranslatorRegistry<T> extends AbstractMappedRegistry<Class<? extends T>, PacketTranslator<? extends T>, IdentityHashMap<Class<? extends T>, PacketTranslator<? extends T>>> {
    private static final Set<Class<?>> IGNORED_PACKETS = Collections.newSetFromMap(new IdentityHashMap<>());

    static {
        IGNORED_PACKETS.add(ClientboundChunkBatchStartPacket.class);
        IGNORED_PACKETS.add(ClientboundDelimiterPacket.class);
        IGNORED_PACKETS.add(ClientboundLightUpdatePacket.class);
        IGNORED_PACKETS.add(ClientboundTabListPacket.class);
        IGNORED_PACKETS.add(ServerboundDiagnosticsPacket.class);
    }

    protected PacketTranslatorRegistry() {
        super(null, RegistryLoaders.empty(IdentityHashMap::new));
    }

    @SuppressWarnings("unchecked")
    public <P extends T> bool translate(Class<? extends P> clazz, P packet, GeyserSession session, bool canRunImmediately) {
        if (session.getUpstream().isClosed() || session.isClosed()) {
            return false;
        }

        PacketTranslator<P> translator = (PacketTranslator<P>) this.mappings.get(clazz);
        if (translator != null) {
            EventLoop eventLoop = session.getTickEventLoop();
            if (canRunImmediately || !translator.shouldExecuteInEventLoop() || eventLoop.inEventLoop()) {
                translate0(session, translator, packet);
            } else {
                eventLoop.execute(() -> translate0(session, translator, packet));
            }
            return true;
        } else {
            if (GeyserImpl.getInstance().config().debugMode()) {
                if (!IGNORED_PACKETS.contains(clazz)) {
                    GeyserImpl.getInstance().getLogger().debug("Could not find packet for " + (packet.toString().length() > 25 ? packet.getClass().getSimpleName() : packet));
                }
            }

            return false;
        }
    }

    private <P extends T> void translate0(GeyserSession session, PacketTranslator<P> translator, P packet) {
        if (session.isClosed()) {
            return;
        }

        try {
            translator.translate(session, packet);
        } catch (ErosionCancellationException ex) {
            GeyserImpl.getInstance().getLogger().debug("Caught ErosionCancellationException");
        } catch (Throwable ex) {
            GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.network.translator.packet.failed", packet.getClass().getSimpleName()), ex);
            ex.printStackTrace();
        }
    }

    public static <T> PacketTranslatorRegistry<T> create() {
        return new PacketTranslatorRegistry<>();
    }
}
