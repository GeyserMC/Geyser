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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientEditBookPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.protocol.bedrock.packet.BookEditPacket;
import org.geysermc.connector.inventory.GeyserItemStack;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Translator(packet = BookEditPacket.class)
public class BedrockBookEditTranslator extends PacketTranslator<BookEditPacket> {

    @Override
    public void translate(BookEditPacket packet, GeyserSession session) {
        GeyserItemStack itemStack = session.getPlayerInventory().getItemInHand();
        if (itemStack != null) {
            CompoundTag tag = itemStack.getNbt() != null ? itemStack.getNbt() : new CompoundTag("");
            ItemStack bookItem = new ItemStack(itemStack.getJavaId(), itemStack.getAmount(), tag);
            List<Tag> pages = tag.contains("pages") ? new LinkedList<>(((ListTag) tag.get("pages")).getValue()) : new LinkedList<>();

            int page = packet.getPageNumber();
            switch (packet.getAction()) {
                case ADD_PAGE: {
                    // Add empty pages in between
                    for (int i = pages.size(); i < page; i++) {
                        pages.add(i, new StringTag("", ""));
                    }
                    pages.add(page, new StringTag("", packet.getText()));
                    break;
                }
                // Called whenever a page is modified
                case REPLACE_PAGE: {
                    if (page < pages.size()) {
                        pages.set(page, new StringTag("", packet.getText()));
                    } else {
                        // Add empty pages in between
                        for (int i = pages.size(); i < page; i++) {
                            pages.add(i, new StringTag("", ""));
                        }
                        pages.add(page, new StringTag("", packet.getText()));
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
                    tag.put(new StringTag("author", packet.getAuthor()));
                    tag.put(new StringTag("title", packet.getTitle()));
                    break;
                }
                default:
                    return;
            }
            // Remove empty pages at the end
            while (pages.size() > 0) {
                StringTag currentPage = (StringTag) pages.get(pages.size() - 1);
                if (currentPage.getValue() == null || currentPage.getValue().isEmpty()) {
                    pages.remove(pages.size() - 1);
                } else {
                    break;
                }
            }
            tag.put(new ListTag("pages", pages));
            session.getPlayerInventory().setItem(36 + session.getPlayerInventory().getHeldItemSlot(), GeyserItemStack.from(bookItem), session);
            session.getInventoryTranslator().updateInventory(session, session.getPlayerInventory());

            session.getBookEditCache().setPacket(new ClientEditBookPacket(bookItem, packet.getAction() == BookEditPacket.Action.SIGN_BOOK, session.getPlayerInventory().getHeldItemSlot()));
            // There won't be any more book updates after this, so we can try sending the edit packet immediately
            if (packet.getAction() == BookEditPacket.Action.SIGN_BOOK) {
                session.getBookEditCache().checkForSend();
            }
        }
    }
}
