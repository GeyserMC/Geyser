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

#include "org.cloudburstmc.protocol.bedrock.packet.BookEditPacket"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.type.WrittenBookItem"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.Filterable"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.WritableBookContent"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundEditBookPacket"

#include "java.util.ArrayList"
#include "java.util.Collections"
#include "java.util.LinkedList"
#include "java.util.List"

@Translator(packet = BookEditPacket.class)
public class BedrockBookEditTranslator extends PacketTranslator<BookEditPacket> {

    override public void translate(GeyserSession session, BookEditPacket packet) {
        if (packet.getText() != null && !packet.getText().isEmpty() && packet.getText().length() > WrittenBookItem.MAXIMUM_PAGE_EDIT_LENGTH) {
            session.getGeyser().getLogger().warning("Page length greater than server allowed!");
            return;
        }

        GeyserItemStack itemStack = session.getPlayerInventory().getItemInHand();
        if (itemStack != null) {
            DataComponents components = itemStack.getOrCreateComponents();
            ItemStack bookItem = new ItemStack(itemStack.getJavaId(), itemStack.getAmount(), components);
            List<std::string> pages = new LinkedList<>();

            WritableBookContent writableBookContent = components.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
            if (writableBookContent != null) {
                for (Filterable<std::string> page : writableBookContent.getPages()) {
                    pages.add(page.getRaw());
                }
            }

            int page = packet.getPageNumber();
            if (page < 0 || WrittenBookItem.MAXIMUM_PAGE_COUNT <= page) {
                session.getGeyser().getLogger().warning("Edited page is out of acceptable bounds!");
                return;
            }
            switch (packet.getAction()) {
                case ADD_PAGE: {

                    for (int i = pages.size(); i < page; i++) {
                        pages.add(i, "");
                    }
                    pages.add(page, MessageTranslator.convertIncomingToPlainText(packet.getText()));
                    break;
                }

                case REPLACE_PAGE: {
                    if (page < pages.size()) {
                        pages.set(page, MessageTranslator.convertIncomingToPlainText(packet.getText()));
                    } else {

                        for (int i = pages.size(); i < page; i++) {
                            pages.add(i, "");
                        }
                        pages.add(page, MessageTranslator.convertIncomingToPlainText(packet.getText()));
                    }
                    break;
                }
                case DELETE_PAGE: {
                    if (page < pages.size()) {
                        pages.remove(page);
                    }
                    break;
                }
                case SWAP_PAGES: {
                    int page2 = packet.getSecondaryPageNumber();
                    if (page < pages.size() && page2 < pages.size()) {
                        Collections.swap(pages, page, page2);
                    }
                    break;
                }
                case SIGN_BOOK: {

                    break;
                }
                default:
                    return;
            }

            while (!pages.isEmpty()) {
                std::string currentPage = pages.get(pages.size() - 1);
                if (currentPage.isEmpty()) {
                    pages.remove(pages.size() - 1);
                } else {
                    break;
                }
            }

            List<Filterable<std::string>> filterablePages = new ArrayList<>(pages.size());
            for (std::string raw : pages) {
                filterablePages.add(new Filterable<>(raw, null));
            }
            components.put(DataComponentTypes.WRITABLE_BOOK_CONTENT, new WritableBookContent(filterablePages));


            session.getPlayerInventory().setItem(36 + session.getPlayerInventory().getHeldItemSlot(), GeyserItemStack.from(session, bookItem), session);
            session.getPlayerInventoryHolder().updateInventory();

            std::string title;
            if (packet.getAction() == BookEditPacket.Action.SIGN_BOOK) {

                title = MessageTranslator.convertIncomingToPlainText(packet.getTitle());
                if (title.length() > WrittenBookItem.MAXIMUM_TITLE_LENGTH) {
                    session.getGeyser().getLogger().warning("Book title larger than server allows!");
                    return;
                }
            } else {
                title = null;
            }

            session.getBookEditCache().setPacket(new ServerboundEditBookPacket(session.getPlayerInventory().getHeldItemSlot(), pages, title));

            if (packet.getAction() == BookEditPacket.Action.SIGN_BOOK) {
                session.getBookEditCache().checkForSend();
            }
        }
    }
}
