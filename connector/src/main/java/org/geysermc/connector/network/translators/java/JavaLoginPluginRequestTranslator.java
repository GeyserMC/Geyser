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

package org.geysermc.connector.network.translators.java;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.packet.login.client.LoginPluginResponsePacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginPluginRequestPacket;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetInput;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.cumulus.ModalForm;
import org.geysermc.cumulus.response.ModalFormResponse;

@Translator(packet = LoginPluginRequestPacket.class)
public class JavaLoginPluginRequestTranslator extends PacketTranslator<LoginPluginRequestPacket> {
    @SneakyThrows
    @Override
    public void translate(LoginPluginRequestPacket packet, GeyserSession session) {
        // Handles VIAaaS (ViaVersion as a Service) reauthentication request
        if ("viaaas:reauth".equals(packet.getChannel())) {
            ByteBufNetInput data = new ByteBufNetInput(Unpooled.wrappedBuffer(packet.getData()));
            String username = data.readString();
            String connectionHash = data.readString();
            String token;
            if (session.getProtocol().getProfile().getName().equalsIgnoreCase(username)
                    && (token = session.getProtocol().getAccessToken()) != null) {
                session.sendForm(ModalForm.builder()
                        .translator(LanguageUtils::getPlayerLocaleString, session.getLocale())
                        .title("geyser.auth.login.reauth.title")
                        .content("geyser.auth.login.reauth.desc")
                        .button1("gui.yes")
                        .button2("gui.no")
                        .responseHandler((form, responseData) -> {
                            ModalFormResponse response = form.parseResponse(responseData);

                            if (!response.isCorrect() || !response.getResult()) {
                                // Let VIAaaS handle by itself
                                session.sendDownstreamPacket(
                                        new LoginPluginResponsePacket(packet.getMessageId(), null));
                                return;
                            }

                            session.getConnector().getGeneralThreadPool().submit(() -> {
                                try {
                                    SessionService service = session.getDownstream().getFlag("session-service",
                                            new SessionService());
                                    service.joinServer(session.getProtocol().getProfile(), token, connectionHash);

                                    // Success
                                    session.sendDownstreamPacket(
                                            new LoginPluginResponsePacket(packet.getMessageId(), new byte[0]));
                                } catch (RequestException e) {
                                    session.getUpstream().disconnect("Couldn't reauthenticate with VIAaaS " + e);
                                }
                            });
                        }));
                return;
            }
        }
        // A vanilla client doesn't know any PluginMessage in the Login state, so we don't know any either.
        // Note: Fabric Networking API v1 will not let the client log in without sending this
        session.sendDownstreamPacket(
                new LoginPluginResponsePacket(packet.getMessageId(), null)
        );
    }
}
