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

package org.geysermc.geyser.item.type;

import com.github.steveice10.mc.protocol.data.game.Identifier;
import com.github.steveice10.mc.protocol.data.game.item.ItemStack;
import com.github.steveice10.mc.protocol.data.game.item.component.DataComponentPatch;
import com.github.steveice10.mc.protocol.data.game.item.component.DataComponentType;
import com.github.steveice10.mc.protocol.data.game.item.component.ItemEnchantments;
import com.github.steveice10.opennbt.tag.builtin.*;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.item.Enchantment;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.InventoryUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Item {
    private final String javaIdentifier;
    private int javaId = -1;
    private final int stackSize;
    private final int attackDamage;
    private final int maxDamage;

    public Item(String javaIdentifier, Builder builder) {
        this.javaIdentifier = Identifier.formalize(javaIdentifier).intern();
        this.stackSize = builder.stackSize;
        this.maxDamage = builder.maxDamage;
        this.attackDamage = builder.attackDamage;
    }

    public String javaIdentifier() {
        return javaIdentifier;
    }

    public int javaId() {
        return javaId;
    }

    public int maxDamage() {
        return maxDamage;
    }

    public int attackDamage() {
        return attackDamage;
    }

    public int maxStackSize() {
        return stackSize;
    }

    public boolean isValidRepairItem(Item other) {
        return false;
    }

    /* Translation methods to Bedrock and back */

    public ItemData.Builder translateToBedrock(ItemStack itemStack, ItemMapping mapping, ItemMappings mappings) {
        if (InventoryUtils.isEmpty(itemStack)) {
            // Return, essentially, air
            return ItemData.builder();
        }
        ItemData.Builder builder = ItemData.builder()
                .definition(mapping.getBedrockDefinition())
                .damage(mapping.getBedrockData())
                .count(itemStack.getAmount());
        if (itemStack.getNbt() != null) {
            builder.tag(ItemTranslator.translateNbtToBedrock(itemStack.getNbt()));
        }

        CompoundTag nbt = itemStack.getNbt();
        ItemTranslator.translateCustomItem(nbt, builder, mapping);

        return builder;
    }

    public @NonNull ItemStack translateToJava(@NonNull ItemData itemData, @NonNull ItemMapping mapping, @NonNull ItemMappings mappings) {
        if (itemData.getTag() == null) {
            return new ItemStack(javaId, itemData.getCount(), null);
        }
        return new ItemStack(javaId, itemData.getCount(), ItemTranslator.translateToJavaNBT("", itemData.getTag()));
    }

    public ItemMapping toBedrockDefinition(CompoundTag nbt, ItemMappings mappings) {
        return mappings.getMapping(javaId);
    }

    /**
     * Takes components from Java Edition and map them into Bedrock.
     */
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull DataComponentPatch components, @NonNull NbtMapBuilder builder) {
//        // Basing off of ItemStack#getHoverName as of 1.20.5. VERIFY??
//        Component customName = components.get(DataComponentType.CUSTOM_NAME);
//        if (customName == null) {
//            customName = components.get(DataComponentType.ITEM_NAME);
//        }
//        if (customName != null) {
//
//        }
        List<Component> loreComponents = components.get(DataComponentType.LORE);
        if (loreComponents != null) {
            List<String> lore = new ArrayList<>();
            for (Component loreComponent : loreComponents) {
                lore.add(MessageTranslator.convertMessage(loreComponent, session.locale()));
            }
            builder.putList("Lore", NbtType.STRING, lore);
        }

        List<Tag> newTags = new ArrayList<>();
        ItemEnchantments enchantments = components.get(DataComponentType.ENCHANTMENTS);
        if (enchantments != null) {

        }
        if (enchantmentTag instanceof ListTag listTag) {
            for (Tag subTag : listTag.getValue()) {
                if (!(subTag instanceof CompoundTag)) continue;
                CompoundTag bedrockTag = remapEnchantment(session, (CompoundTag) subTag, tag);
                if (bedrockTag != null) {
                    newTags.add(bedrockTag);
                }
            }
        }

        if (!newTags.isEmpty()) {
            tag.put(new ListTag("ench", newTags));
        }
    }

    /**
     * Takes NBT from Bedrock Edition and converts any value that Java parses differently. <br>
     * Do note that this method is, these days, only called in three places (as of 2023/~1.19):
     * <ul>
     *     <li>Extra recipe loading</li>
     *     <li>Creative menu</li>
     *     <li>Stonecutters</li>
     * </ul>
     * Therefore, if translation cannot be achieved for a certain item, it is not necessarily bad.
     */
    public void translateNbtToJava(@NonNull CompoundTag tag, @NonNull ItemMapping mapping) {
        CompoundTag displayTag = tag.get("display");
        if (displayTag != null) {
            if (displayTag.contains("Name")) {
                StringTag nameTag = displayTag.get("Name");
                displayTag.put(new StringTag("Name", MessageTranslator.convertToJavaMessage(nameTag.getValue())));
            }

            if (displayTag.contains("Lore")) {
                ListTag loreTag = displayTag.get("Lore");
                List<Tag> lore = new ArrayList<>();
                for (Tag subTag : loreTag.getValue()) {
                    if (!(subTag instanceof StringTag)) continue;
                    lore.add(new StringTag("", MessageTranslator.convertToJavaMessage(((StringTag) subTag).getValue())));
                }
                displayTag.put(new ListTag("Lore", lore));
            }
        }

        ListTag enchantmentTag = tag.remove("ench");
        if (enchantmentTag != null) {
            List<Tag> enchantments = new ArrayList<>();
            for (Tag value : enchantmentTag.getValue()) {
                if (!(value instanceof CompoundTag tagValue))
                    continue;

                ShortTag bedrockId = tagValue.get("id");
                if (bedrockId == null) continue;

                Enchantment enchantment = Enchantment.getByBedrockId(bedrockId.getValue());
                if (enchantment != null) {
                    CompoundTag javaTag = new CompoundTag("");
                    Map<String, Tag> javaValue = javaTag.getValue();
                    javaValue.put("id", new StringTag("id", enchantment.getJavaIdentifier()));
                    ShortTag levelTag = tagValue.get("lvl");
                    javaValue.put("lvl", new IntTag("lvl", levelTag != null ? levelTag.getValue() : 1));
                    javaTag.setValue(javaValue);

                    enchantments.add(javaTag);
                } else {
                    GeyserImpl.getInstance().getLogger().debug("Unknown bedrock enchantment: " + bedrockId);
                }
            }
            if (!enchantments.isEmpty()) {
                if ((this instanceof EnchantedBookItem)) {
                    tag.put(new ListTag("StoredEnchantments", enchantments));
                } else {
                    tag.put(new ListTag("Enchantments", enchantments));
                }
            }
        }
    }

    protected final @Nullable NbtMap remapEnchantment(GeyserSession session, ItemEnchantments, NbtMapBuilder rootBuilder) {

        Enchantment enchantment = Enchantment.getByJavaIdentifier(((StringTag) javaEnchId).getValue());
        if (enchantment == null) {
            if (Identifier.formalize((String) javaEnchId.getValue()).equals("minecraft:sweeping")) {
                Tag javaEnchLvl = tag.get("lvl");
                int sweepingLvl = javaEnchLvl != null && javaEnchLvl.getValue() instanceof Number lvl ? lvl.intValue() : 0;

                addSweeping(session, rootTag, sweepingLvl);
                return null;
            }
            GeyserImpl.getInstance().getLogger().debug("Unknown Java enchantment while NBT item translating: " + javaEnchId.getValue());
            return null;
        }

        Tag javaEnchLvl = tag.get("lvl");

        NbtMapBuilder builder = NbtMap.builder();
        builder.putShort("id", (short) enchantment.ordinal());
        builder.putShort("lvl", );
        return builder.build();
    }

    private void addSweeping(GeyserSession session, CompoundTag itemTag, int level) {
        CompoundTag displayTag = itemTag.get("display");
        if (displayTag == null) {
            displayTag = new CompoundTag("display");
            itemTag.put(displayTag);
        }
        ListTag loreTag = displayTag.get("Lore");
        if (loreTag == null) {
            loreTag = new ListTag("Lore");
            displayTag.put(loreTag);
        }

        String sweepingTranslation = MinecraftLocale.getLocaleString("enchantment.minecraft.sweeping", session.locale());
        String lvlTranslation = MinecraftLocale.getLocaleString("enchantment.level." + level, session.locale());

        loreTag.add(new StringTag("", ChatColor.RESET + ChatColor.GRAY + sweepingTranslation + " " + lvlTranslation));
    }

    /* Translation methods end */

    public ItemStack newItemStack(int count, DataComponentPatch components) {
        return new ItemStack(this.javaId, count, components);
    }

    public void setJavaId(int javaId) { // TODO like this?
        if (this.javaId != -1) {
            throw new RuntimeException("Item ID has already been set!");
        }
        this.javaId = javaId;
    }

    @Override
    public String toString() {
        return "Item{" +
                "javaIdentifier='" + javaIdentifier + '\'' +
                ", javaId=" + javaId +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int stackSize = 64;
        private int maxDamage;
        private int attackDamage;

        public Builder stackSize(int stackSize) {
            this.stackSize = stackSize;
            return this;
        }

        public Builder attackDamage(double attackDamage) {
            // TODO properly store/send a double value once Bedrock supports it.. pls
            this.attackDamage = (int) attackDamage;
            return this;
        }

        public Builder maxDamage(int maxDamage) {
            this.maxDamage = maxDamage;
            return this;
        }

        private Builder() {
        }
    }
}
