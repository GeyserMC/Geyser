package org.geysermc.connector.network.translators.item.translators.nbt;

import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.network.translators.NbtItemStackTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ItemRemapper(priority = -1)
public class BasicItemTranslator extends NbtItemStackTranslator {

    @Override
    public void translateToBedrock(CompoundTag itemTag, ItemEntry itemEntry) {
        if (itemTag.contains("display")) {
            CompoundTag displayTag = itemTag.get("display");
            if (displayTag.contains("Name")) {
                StringTag nameTag = displayTag.get("Name");

                try {
                    displayTag.put(new StringTag("Name", "§r" + MessageUtils.getBedrockMessage(nameTag.getValue())));
                } catch (Exception ex) {
                    GeyserConnector.getInstance().getLogger().error("Invalid Value " + nameTag.getValue());
                }
            }

            if (displayTag.contains("Lore")) {
                ListTag loreTag = displayTag.get("Lore");
                List<Tag> lore = new ArrayList<>();
                for (Tag tag : loreTag.getValue()) {
                    if (!(tag instanceof StringTag)) return;
                    GeyserConnector.getInstance().getLogger().info("Value " + tag.getValue());
                    try {
                        lore.add(new StringTag("", "§r" + MessageUtils.getBedrockMessage((String) tag.getValue())));
                    } catch (Exception ex) {
                        GeyserConnector.getInstance().getLogger().error("Invalid Value " + tag.getValue() + " " + displayTag.toString(), ex);
                    }
                }
                displayTag.put(new ListTag("Lore", lore));
            }
        }
    }

    @Override
    public void translateToJava(CompoundTag itemTag, ItemEntry itemEntry) {
        if (itemTag.contains("display")) {
            CompoundTag displayTag = itemTag.get("display");
            if (displayTag.contains("Name")) {
                StringTag nameTag = displayTag.get("Name");
                displayTag.put(new StringTag("Name", toJavaMessage(nameTag)));
            }

            if (displayTag.contains("Lore")) {
                ListTag loreTag = displayTag.get("Lore");
                List<Tag> lore = new ArrayList<>();
                for (Tag tag : loreTag.getValue()) {
                    if (!(tag instanceof StringTag)) return;
                    lore.add(new StringTag("", "§r" + toJavaMessage((StringTag) tag)));
                }
                displayTag.put(new ListTag("Lore", lore));
            }
        }
    }

    private String toJavaMessage(StringTag tag) {
        String message = tag.getValue();
        if (message == null) return null;
        if (message.startsWith("§r")) {
            message = message.replaceFirst("§r", "");
        }
        return MessageUtils.getJavaMessage(message);
    }
}
