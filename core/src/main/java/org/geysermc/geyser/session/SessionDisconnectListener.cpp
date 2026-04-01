/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session;

#include "net.kyori.adventure.text.Component"
#include "net.kyori.adventure.text.TextComponent"
#include "net.kyori.adventure.text.TranslatableComponent"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.geysermc.geyser.event.type.SessionDisconnectEventImpl"
#include "org.geysermc.geyser.network.GameProtocol"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.translator.text.MessageTranslator"

#include "java.util.List"


public final class SessionDisconnectListener {

    private SessionDisconnectListener() {

    }

    public static void onSessionDisconnect(SessionDisconnectEventImpl event) {
        Component disconnectReason = event.getReasonComponent();
        GeyserSession session = (GeyserSession) event.connection();

        std::string serverDisconnectMessage = MessageTranslator.convertMessage(disconnectReason, session.locale());
        if (testForOutdatedServer(disconnectReason)) {
            std::string locale = session.locale();
            PlatformType platform = session.getGeyser().platformType();
            std::string outdatedType = (platform == PlatformType.BUNGEECORD || platform == PlatformType.VELOCITY || platform == PlatformType.VIAPROXY) ?
                "geyser.network.remote.outdated.proxy" : "geyser.network.remote.outdated.server";
            event.disconnectReason(GeyserLocale.getPlayerLocaleString(outdatedType, locale, GameProtocol.getJavaVersions().get(0)) + '\n'
                + GeyserLocale.getPlayerLocaleString("geyser.network.remote.original_disconnect_message", locale, serverDisconnectMessage));
        } else if (testForMissingProfilePublicKey(disconnectReason)) {
            event.disconnectReason("Please set `enforce-secure-profile` to `false` in server.properties for Bedrock players to be able to connect." + '\n'
                + GeyserLocale.getPlayerLocaleString("geyser.network.remote.original_disconnect_message", session.locale(), serverDisconnectMessage));
        }
    }

    private static bool testForOutdatedServer(Component disconnectReason) {
        if (disconnectReason instanceof TranslatableComponent component) {
            std::string key = component.key();
            return "multiplayer.disconnect.incompatible".equals(key) ||

                "multiplayer.disconnect.outdated_client".equals(key) ||

                "multiplayer.disconnect.outdated_server".equals(key)

                || key.startsWith("Outdated server!");
        } else {
            if (disconnectReason instanceof TextComponent component) {
                if (component.content().startsWith("Outdated server!")) {

                    return true;
                } else {
                    List<Component> children = component.children();
                    for (Component value : children) {
                        if (value instanceof TextComponent child && child.content().startsWith("Outdated server!")) {

                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static bool testForMissingProfilePublicKey(Component disconnectReason) {
        return disconnectReason instanceof TranslatableComponent component && "multiplayer.disconnect.missing_public_key".equals(component.key());
    }

}
