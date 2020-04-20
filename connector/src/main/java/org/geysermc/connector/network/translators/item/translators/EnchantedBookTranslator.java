package org.geysermc.connector.network.translators.item.translators;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.data.ItemData;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.ItemStackTranslator;
import org.geysermc.connector.network.translators.ItemTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;

@ItemTranslator
public class EnchantedBookTranslator extends ItemStackTranslator {

    @Override
    public ItemStack translateToBedrock(GeyserSession session, ItemStack itemStack, ItemEntry itemEntry) {
        if(itemStack == null || itemStack.getNbt() == null) return itemStack;

        com.github.steveice10.opennbt.tag.builtin.CompoundTag itemTag = itemStack.getNbt();
        if(itemTag.contains("StoredEnchantments")){
            Tag enchTag = itemTag.get("StoredEnchantments");
            if (enchTag instanceof ListTag) {
                enchTag = new ListTag("Enchantments", ((ListTag) enchTag).getValue());
                itemTag.remove("StoredEnchantments");
                itemTag.put( enchTag);
            }
        }
        return itemStack;
    }

    @Override
    public ItemData translateToJava(GeyserSession session, ItemData itemData, ItemEntry itemEntry) {
        if(itemData == null || itemData.getTag() == null) return itemData;

        CompoundTag itemTag = itemData.getTag();
        if(itemTag.contains("Enchantments")){
            com.nukkitx.nbt.tag.ListTag<CompoundTag> enchantments = itemTag.get("Enchantments");
            com.nukkitx.nbt.tag.ListTag<CompoundTag> storedEnchantments =
                    new com.nukkitx.nbt.tag.ListTag<>("StoredEnchantments", CompoundTag.class, enchantments.getValue());
            itemTag.getValue().put("StoredEnchantments", storedEnchantments);
            itemTag.getValue().remove("Enchantments");
        }
        return itemData;
    }

    @Override
    public boolean acceptItem(ItemEntry itemEntry) {
        return "minecraft:enchanted_book".equals(itemEntry.getJavaIdentifier());
    }
}
