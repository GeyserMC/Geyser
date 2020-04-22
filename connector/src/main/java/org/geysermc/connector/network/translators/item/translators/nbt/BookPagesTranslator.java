package org.geysermc.connector.network.translators.item.translators.nbt;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.network.translators.NbtItemStackTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

@ItemRemapper
public class BookPagesTranslator extends NbtItemStackTranslator {

    @Override
    public void translateToBedrock(CompoundTag itemTag, ItemEntry itemEntry) {
        if (itemTag.contains("pages")) {
            List<Tag> pages = new ArrayList<>();
            ListTag pagesTag = itemTag.get("pages");
            for (Tag tag : pagesTag.getValue()) {
                if (!(tag instanceof StringTag))
                    continue;

                StringTag textTag = (StringTag) tag;

                CompoundTag pageTag = new CompoundTag("");
                pageTag.put(new StringTag("photoname", ""));
                pageTag.put(new StringTag("text", MessageUtils.getBedrockMessage(textTag.getValue())));
                pages.add(pageTag);
            }

            itemTag.remove("pages");
            itemTag.put(new ListTag("pages", pages));
        }
    }

    @Override
    public void translateToJava(CompoundTag itemTag, ItemEntry itemEntry) {
        if (itemTag.contains("pages")) {
            List<Tag> pages = new ArrayList<>();
            ListTag pagesTag = itemTag.get("pages");
            for (Tag tag : pagesTag.getValue()) {
                if (!(tag instanceof CompoundTag))
                    continue;

                CompoundTag pageTag = (CompoundTag) tag;

                StringTag textTag = pageTag.get("text");
                pages.add(new StringTag(MessageUtils.getJavaMessage(textTag.getValue())));
            }

            itemTag.remove("pages");
            itemTag.put(new ListTag("pages", pages));
        }
    }
}
