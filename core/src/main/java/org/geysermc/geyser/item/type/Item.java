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

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.item.Enchantment;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.Identifier;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;

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

    public ItemData.Builder translateToBedrock(int count, DataComponents components, ItemMapping mapping, ItemMappings mappings) {
        if (this == Items.AIR || count <= 0) {
            // Return, essentially, air
            return ItemData.builder();
        }
        ItemData.Builder builder = ItemData.builder()
                .definition(mapping.getBedrockDefinition())
                .damage(mapping.getBedrockData())
                .count(count);

        ItemTranslator.translateCustomItem(components, builder, mapping);

        return builder;
    }

    public @NonNull GeyserItemStack translateToJava(@NonNull ItemData itemData, @NonNull ItemMapping mapping, @NonNull ItemMappings mappings) {
        return GeyserItemStack.of(javaId, itemData.getCount());
    }

    public ItemMapping toBedrockDefinition(DataComponents components, ItemMappings mappings) {
        return mappings.getMapping(javaId);
    }

    /**
     * Takes components from Java Edition and map them into Bedrock.
     */
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull DataComponents components, @NonNull BedrockItemBuilder builder) {
        List<Component> loreComponents = components.get(DataComponentType.LORE);
        if (loreComponents != null) {
            List<String> lore = builder.getOrCreateLore();
            for (Component loreComponent : loreComponents) {
                lore.add(MessageTranslator.convertMessage(loreComponent, session.locale()));
            }
        }

        Integer damage = components.get(DataComponentType.DAMAGE);
        if (damage != null) {
            builder.setDamage(damage);
        }

        List<NbtMap> enchantNbtList = new ArrayList<>();
        ItemEnchantments enchantments = components.get(DataComponentType.ENCHANTMENTS);
        if (enchantments != null) {
            for (Map.Entry<Integer, Integer> enchantment : enchantments.getEnchantments().entrySet()) {
                NbtMap enchantNbt = remapEnchantment(session, enchantment.getKey(), enchantment.getValue(), builder);
                if (enchantNbt != null) {
                    enchantNbtList.add(enchantNbt);
                }
            }
        }

        if (!enchantNbtList.isEmpty()) {
            builder.putList("ench", NbtType.COMPOUND, enchantNbtList);
        }

        Integer repairCost = components.get(DataComponentType.REPAIR_COST);
        if (repairCost != null) {
            builder.putInt("RepairCost", repairCost);
        }

        // Prevents the client from trying to stack items with untranslated components
        // Relies on correct hash code implementation, and some luck
        builder.putInt("GeyserHash", components.hashCode()); // TODO: don't rely on this
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
    public void translateNbtToJava(@NonNull NbtMap bedrockTag, @NonNull DataComponents components, @NonNull ItemMapping mapping) {
        // TODO see if any items from the creative menu need this
//        CompoundTag displayTag = tag.get("display");
//        if (displayTag != null) {
//            if (displayTag.contains("Name")) {
//                StringTag nameTag = displayTag.get("Name");
//                displayTag.put(new StringTag("Name", MessageTranslator.convertToJavaMessage(nameTag.getValue())));
//            }
//
//            if (displayTag.contains("Lore")) {
//                ListTag loreTag = displayTag.get("Lore");
//                List<Tag> lore = new ArrayList<>();
//                for (Tag subTag : loreTag.getValue()) {
//                    if (!(subTag instanceof StringTag)) continue;
//                    lore.add(new StringTag("", MessageTranslator.convertToJavaMessage(((StringTag) subTag).getValue())));
//                }
//                displayTag.put(new ListTag("Lore", lore));
//            }
//        }

        // TODO no creative item should have enchantments *except* enchanted books
//        List<NbtMap> enchantmentTag = bedrockTag.getList("ench", NbtType.COMPOUND);
//        if (enchantmentTag != null) {
//            List<Tag> enchantments = new ArrayList<>();
//            for (Tag value : enchantmentTag.getValue()) {
//                if (!(value instanceof CompoundTag tagValue))
//                    continue;
//
//                ShortTag bedrockId = tagValue.get("id");
//                if (bedrockId == null) continue;
//
//                Enchantment enchantment = Enchantment.getByBedrockId(bedrockId.getValue());
//                if (enchantment != null) {
//                    CompoundTag javaTag = new CompoundTag("");
//                    Map<String, Tag> javaValue = javaTag.getValue();
//                    javaValue.put("id", new StringTag("id", enchantment.getJavaIdentifier()));
//                    ShortTag levelTag = tagValue.get("lvl");
//                    javaValue.put("lvl", new IntTag("lvl", levelTag != null ? levelTag.getValue() : 1));
//                    javaTag.setValue(javaValue);
//
//                    enchantments.add(javaTag);
//                } else {
//                    GeyserImpl.getInstance().getLogger().debug("Unknown bedrock enchantment: " + bedrockId);
//                }
//            }
//            if (!enchantments.isEmpty()) {
//                if ((this instanceof EnchantedBookItem)) {
//                    bedrockTag.put(new ListTag("StoredEnchantments", enchantments));
//                    components.put(DataComponentType.STORED_ENCHANTMENTS, enchantments);
//                } else {
//                    components.put(DataComponentType.ENCHANTMENTS, enchantments);
//                }
//            }
//        }
    }

    protected final @Nullable NbtMap remapEnchantment(GeyserSession session, int enchantId, int level, BedrockItemBuilder builder) {
        // TODO verify
        // TODO streamline Enchantment process
        Enchantment.JavaEnchantment enchantment = Enchantment.JavaEnchantment.of(enchantId);
        if (enchantment == Enchantment.JavaEnchantment.SWEEPING_EDGE) {
            addSweeping(session, builder, level);
            return null;
        }
        if (enchantment == null) {
            GeyserImpl.getInstance().getLogger().debug("Unknown Java enchantment while NBT item translating: " + enchantId);
            return null;
        }

        return NbtMap.builder()
                .putShort("id", (short) Enchantment.valueOf(enchantment.name()).ordinal())
                .putShort("lvl", (short) level)
                .build();
    }

    private void addSweeping(GeyserSession session, BedrockItemBuilder builder, int level) {
        String sweepingTranslation = MinecraftLocale.getLocaleString("enchantment.minecraft.sweeping", session.locale());
        String lvlTranslation = MinecraftLocale.getLocaleString("enchantment.level." + level, session.locale());

        builder.getOrCreateLore().add(ChatColor.RESET + ChatColor.GRAY + sweepingTranslation + " " + lvlTranslation);
    }

    /* Translation methods end */

    public GeyserItemStack newItemStack(int count, DataComponents components) {
        return GeyserItemStack.of(this.javaId, count, components);
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
