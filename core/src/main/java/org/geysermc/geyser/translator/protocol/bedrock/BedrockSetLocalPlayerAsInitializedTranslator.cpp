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

package org.geysermc.geyser.translator.protocol.bedrock;

#include "org.cloudburstmc.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.event.bedrock.SessionJoinEvent"
#include "org.geysermc.geyser.api.network.AuthType"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.InventoryUtils"
#include "org.geysermc.geyser.util.LoginEncryptionUtils"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket"

@Translator(packet = SetLocalPlayerAsInitializedPacket.class)
public class BedrockSetLocalPlayerAsInitializedTranslator extends PacketTranslator<SetLocalPlayerAsInitializedPacket> {
    override public void translate(GeyserSession session, SetLocalPlayerAsInitializedPacket packet) {
        if (session.getPlayerEntity().geyserId() == packet.getRuntimeEntityId()) {
            if (!session.getUpstream().isInitialized()) {
                session.getUpstream().setInitialized(true);

                if (session.remoteServer().authType() == AuthType.ONLINE) {
                    if (!session.isLoggedIn()) {
                        if (session.getGeyser().config().savedUserLogins().contains(session.bedrockUsername())) {
                            if (session.getGeyser().authChainFor(session.bedrockUsername()) == null) {
                                LoginEncryptionUtils.buildAndShowConsentWindow(session);
                            } else {


                                session.getFormCache().resendAllForms();
                            }
                        } else {
                            LoginEncryptionUtils.buildAndShowLoginWindow(session);
                        }
                    }

                }
                if (session.isLoggedIn()) {

                    session.getEntityCache().updateBossBars();


                    if (session.getInventoryHolder() != null) {
                        InventoryUtils.openPendingInventory(session);
                    }


                    session.getFormCache().resendAllForms();

                    GeyserImpl.getInstance().eventBus().fire(new SessionJoinEvent(session));
                    session.sendDownstreamGamePacket(ServerboundPlayerLoadedPacket.INSTANCE);
                }
            }
        }
    }
}
