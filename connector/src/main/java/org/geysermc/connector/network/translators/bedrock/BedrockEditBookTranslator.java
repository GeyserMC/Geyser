package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.message.TextMessage;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientEditBookPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.protocol.bedrock.packet.BookEditPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.MessageUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Translator(packet = BookEditPacket.class)
public class BedrockEditBookTranslator  extends PacketTranslator<BookEditPacket> {

    @Override
    public void translate(BookEditPacket packet, GeyserSession session) {
        ItemStack itemStack = session.getInventory().getItemInHand();
        if (itemStack != null) {
            CompoundTag tag = itemStack.getNbt() != null ? itemStack.getNbt() : new CompoundTag("");
            ItemStack bookItem = new ItemStack(itemStack.getId(), itemStack.getAmount(), tag);
            List<Tag> pages = tag.contains("pages") ? new LinkedList<>(((ListTag) tag.get("pages")).getValue()) : new LinkedList<>();

            int page = packet.getPageNumber();

            switch (packet.getAction()) {
                case ADD_PAGE: {
                    pages.add(0, new StringTag("", MessageUtils.getJavaMessage(packet.getText())));
                    tag.put(new ListTag("pages", pages));
                    ClientEditBookPacket editBookPacket = new ClientEditBookPacket(bookItem, false, Hand.MAIN_HAND);
                    session.getDownstream().getSession().send(editBookPacket);
                    break;
                }
                case REPLACE_PAGE: {
                    if (page < pages.size()) {
                        pages.set(page, new StringTag("", MessageUtils.getJavaMessage(packet.getText())));
                    }
                    tag.put(new ListTag("pages", pages));
                    ClientEditBookPacket editBookPacket = new ClientEditBookPacket(bookItem, false, Hand.MAIN_HAND);
                    session.getDownstream().getSession().send(editBookPacket);
                    break;
                }
                case DELETE_PAGE: {
                    if (page < pages.size()) {
                        pages.remove(page);

                        tag.put(new ListTag("pages", pages));
                        ClientEditBookPacket editBookPacket = new ClientEditBookPacket(bookItem, false, Hand.MAIN_HAND);
                        session.getDownstream().getSession().send(editBookPacket);
                    }
                    break;
                }
                case SWAP_PAGES: {
                    int page2 = packet.getSecondaryPageNumber();
                    if (page < pages.size() && page2 < pages.size()) {
                        Collections.swap(pages, page, page2);

                        tag.put(new ListTag("pages", pages));
                        ClientEditBookPacket editBookPacket = new ClientEditBookPacket(bookItem, false, Hand.MAIN_HAND);
                        session.getDownstream().getSession().send(editBookPacket);
                    }
                    break;
                }
                case SIGN_BOOK: {
                    tag.put(new StringTag("author", packet.getAuthor()));
                    tag.put(new StringTag("title", packet.getTitle()));
                    tag.put(new ListTag("pages", pages));
                    ClientEditBookPacket editBookPacket = new ClientEditBookPacket(bookItem, true, Hand.MAIN_HAND);
                    session.getDownstream().getSession().send(editBookPacket);
                    break;
                }
            }
        }
    }
}

