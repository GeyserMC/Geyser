package org.geysermc.connector.network.translators.item.translators;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.protocol.bedrock.data.ItemData;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.ItemStackTranslator;
import org.geysermc.connector.network.translators.ItemTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.Potion;

@ItemTranslator
public class PotionTranslator extends ItemStackTranslator {
    @Override
    public ItemStack translateToBedrock(GeyserSession session, ItemStack itemStack, ItemEntry itemEntry) {
        if(itemStack == null || itemStack.getNbt() == null) return itemStack;

        Tag potionTag = itemStack.getNbt().get("Potion");
        if (potionTag instanceof StringTag) {
            Potion potion = Potion.getByJavaIdentifier(((StringTag) potionTag).getValue());
            if (potion != null) {
                //TODO
                //return new ItemStack(itemEntry.getBedrockId(), potion.getBedrockId(), itemStack.getAmount(), itemStack.getNbt())
            }
            GeyserConnector.getInstance().getLogger().debug("Unknown java potion: " + potionTag.getValue());
        }
        return itemStack;
    }

    @Override
    public ItemData translateToJava(GeyserSession session, ItemData itemData, ItemEntry itemEntry) {
        return super.translateToJava(session, itemData, itemEntry);
    }

    @Override
    public boolean acceptItem(ItemEntry itemEntry) {
        return itemEntry.getJavaIdentifier().endsWith("potion");
    }
}
