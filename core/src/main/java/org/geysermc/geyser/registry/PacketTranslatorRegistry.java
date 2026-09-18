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

import io.netty.channel.EventLoop;
import org.cloudburstmc.protocol.bedrock.packet.ServerboundDiagnosticsPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.erosion.ErosionCancellationException;
import org.geysermc.geyser.registry.loader.RegistryLoaders;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomReportDetailsPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundPostEffectsPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPopPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundResetChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandSuggestionsPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCustomChatCompletionsPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDeleteChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDelimiterPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundGameTestHighlightPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRecipeBookSettingsPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundServerDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundTestInstanceBlockStatus;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.debug.ClientboundDebugBlockValuePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.debug.ClientboundDebugChunkValuePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.debug.ClientboundDebugEntityValuePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.debug.ClientboundDebugEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.debug.ClientboundDebugSamplePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundHurtAnimationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundProjectilePowerPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatEndPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatEnterPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundPlaceGhostRecipePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundAddTransientBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchStartPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundChunksBiomesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLightUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetSimulationDistancePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundTagQueryPacket;
import org.geysermc.mcprotocollib.protocol.packet.ping.clientbound.ClientboundPongResponsePacket;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class PacketTranslatorRegistry<T> extends AbstractMappedRegistry<Class<? extends T>, PacketTranslator<? extends T>, IdentityHashMap<Class<? extends T>, PacketTranslator<? extends T>>> {
    private static final Set<Class<?>> IGNORED_PACKETS = Collections.newSetFromMap(new IdentityHashMap<>());

    static {
        //// Java clientbound

        /// Unnecessary for us

        IGNORED_PACKETS.add(ClientboundChunkBatchStartPacket.class); // we don't track chunk batch sizes/periods
        IGNORED_PACKETS.add(ClientboundDelimiterPacket.class); // Not implemented, spams logs
        IGNORED_PACKETS.add(ClientboundLightUpdatePacket.class); // Light is handled on Bedrock for us
        IGNORED_PACKETS.add(ClientboundSetSimulationDistancePacket.class);
        IGNORED_PACKETS.add(ClientboundPongResponsePacket.class);

        // Java uses this packet to display a block state at a position for exactly 1 second.
        // This block is purely visible, and has no physical implication on the world.
        // This packet is only used by vanilla for falling blocks (as of 26.3). As such,
        // we can ignore this packet for now without causing too much trouble.
        IGNORED_PACKETS.add(ClientboundAddTransientBlockPacket.class);

        IGNORED_PACKETS.add(ClientboundCustomReportDetailsPacket.class); // Details for in Java client's crash reports, unnecessary for us

        IGNORED_PACKETS.add(ClientboundDebugBlockValuePacket.class);
        IGNORED_PACKETS.add(ClientboundDebugChunkValuePacket.class);
        IGNORED_PACKETS.add(ClientboundDebugEntityValuePacket.class);
        IGNORED_PACKETS.add(ClientboundDebugEventPacket.class);
        IGNORED_PACKETS.add(ClientboundDebugSamplePacket.class);

        IGNORED_PACKETS.add(ClientboundPlayerCombatEnterPacket.class); // Doesn't do anything on Java
        IGNORED_PACKETS.add(ClientboundPlayerCombatEndPacket.class); // Doesn't do anything on Java

        IGNORED_PACKETS.add(ClientboundServerDataPacket.class); // Just MOTD and server icon
        IGNORED_PACKETS.add(ClientboundTagQueryPacket.class); // Only sent in response to Serverbound*TagQueryPacket

        /// Unable to implement on bedrock
        IGNORED_PACKETS.add(ClientboundTabListPacket.class);
        IGNORED_PACKETS.add(ClientboundPostEffectsPacket.class);
        IGNORED_PACKETS.add(ClientboundPlaceGhostRecipePacket.class);
        IGNORED_PACKETS.add(ClientboundRecipeBookSettingsPacket.class);
        IGNORED_PACKETS.add(ClientboundResetChatPacket.class); // I think
        IGNORED_PACKETS.add(ClientboundCommandSuggestionsPacket.class); // I think
        IGNORED_PACKETS.add(ClientboundCustomChatCompletionsPacket.class); // I think
        IGNORED_PACKETS.add(ClientboundDeleteChatPacket.class); // I think

        IGNORED_PACKETS.add(ClientboundTestInstanceBlockStatus.class);

        /// Not yet implemented
        IGNORED_PACKETS.add(ClientboundResourcePackPopPacket.class);
        IGNORED_PACKETS.add(ClientboundChunksBiomesPacket.class); // ???

        IGNORED_PACKETS.add(ClientboundGameTestHighlightPosPacket.class); // Maybe?
        IGNORED_PACKETS.add(ClientboundHurtAnimationPacket.class); // Maybe?
        IGNORED_PACKETS.add(ClientboundProjectilePowerPacket.class); // Maybe?

        //// Bedrock serverbound

        IGNORED_PACKETS.add(ServerboundDiagnosticsPacket.class); // spammy
    }

    protected PacketTranslatorRegistry() {
        super(null, RegistryLoaders.empty(IdentityHashMap::new));
    }

    @SuppressWarnings("unchecked")
    public <P extends T> boolean translate(Class<? extends P> clazz, P packet, GeyserSession session, boolean canRunImmediately) {
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
                if (!isIgnoredPacket(clazz)) {
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

    @VisibleForTesting
    public static boolean isIgnoredPacket(Class<?> clazz) {
        return IGNORED_PACKETS.contains(clazz);
    }

    public static <T> PacketTranslatorRegistry<T> create() {
        return new PacketTranslatorRegistry<>();
    }
}
