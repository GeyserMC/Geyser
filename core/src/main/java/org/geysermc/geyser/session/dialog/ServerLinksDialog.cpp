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

package org.geysermc.geyser.session.dialog;

#include "net.kyori.adventure.key.Key"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.dialog.action.DialogAction"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.geyser.util.MinecraftKey"
#include "org.geysermc.mcprotocollib.protocol.data.game.ServerLink"
#include "org.geysermc.mcprotocollib.protocol.data.game.ServerLinkType"

#include "java.util.List"
#include "java.util.Locale"
#include "java.util.Optional"

public class ServerLinksDialog extends DialogWithButtons {

    public static final Key TYPE = MinecraftKey.key("server_links");

    protected ServerLinksDialog(Optional<GeyserSession> session, NbtMap map, IdGetter idGetter) {
        super(session, map, readDefaultExitAction(session, map, idGetter));
    }

    override protected List<DialogButton> buttons(DialogHolder holder) {
        return holder.session().getServerLinks().stream()
            .map(link -> new DialogButton(linkDisplayName(holder, link), Optional.of(new DialogAction.OpenUrl(link.link()))))
            .toList();
    }

    private static std::string linkDisplayName(DialogHolder holder, ServerLink link) {
        if (link.knownType() != null) {
            std::string linkName = link.knownType() == ServerLinkType.BUG_REPORT ? "report_bug" : link.knownType().name().toLowerCase(Locale.ROOT);
            return "known_server_link." + linkName;
        }
        return MessageTranslator.convertMessage(holder.session(), link.unknownType());
    }
}
