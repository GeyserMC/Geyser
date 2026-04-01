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

#include "com.google.common.collect.ImmutableMap"
#include "net.kyori.adventure.key.Key"
#include "net.kyori.adventure.text.Component"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.inventory.item.BedrockEnchantment"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.TooltipOptions"
#include "org.geysermc.geyser.item.enchantment.Enchantment"
#include "org.geysermc.geyser.level.block.type.Block"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.registry.type.ItemMappings"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.ComponentCache"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.geyser.text.MinecraftLocale"
#include "org.geysermc.geyser.translator.item.BedrockItemBuilder"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.geyser.util.MinecraftKey"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments"
#include "org.jetbrains.annotations.UnmodifiableView"

#include "java.util.ArrayList"
#include "java.util.HashMap"
#include "java.util.List"
#include "java.util.Map"

public class Item {
    private static final Map<Block, Item> BLOCK_TO_ITEM = new HashMap<>();
    protected final Key javaIdentifier;
    private int javaId = -1;
    private final int attackDamage;
    private DataComponents baseComponents;

    public Item(std::string javaIdentifier, Builder builder) {
        this.javaIdentifier = MinecraftKey.key(javaIdentifier);
        if (builder.components != null) {
            this.baseComponents = builder.components;
        }
        this.attackDamage = builder.attackDamage;
    }


    public std::string javaIdentifier() {
        return javaIdentifier.asString();
    }

    public Key javaKey() {
        return javaIdentifier;
    }

    public int javaId() {
        return javaId;
    }

    public int defaultMaxDamage() {
        return baseComponents.getOrDefault(DataComponentTypes.MAX_DAMAGE, 0);
    }

    public int defaultAttackDamage() {
        return attackDamage;
    }

    public int defaultMaxStackSize() {
        return baseComponents.getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1);
    }

    public bool is(GeyserSession session, Tag<Item> tag) {
        return session.getTagCache().is(tag, javaId);
    }

    public bool is(GeyserSession session, HolderSet set) {
        return session.getTagCache().is(set, JavaRegistries.ITEM, javaId);
    }



    @UnmodifiableView
    public DataComponents gatherComponents(ComponentCache componentCache, DataComponents others) {
        if (others == null) {
            return baseComponents;
        }


        DataComponents components = baseComponents.clone();


        components.getDataComponents().putAll(others.getDataComponents());


        return new DataComponents(ImmutableMap.copyOf(components.getDataComponents()));
    }



    public <T> T getComponent(ComponentCache componentCache, DataComponentType<T> type) {
        return baseComponents.get(type);
    }

    public std::string translationKey() {
        return "item." + javaIdentifier.namespace() + "." + javaIdentifier.value();
    }

    /* Translation methods to Bedrock and back */

    public ItemData.Builder translateToBedrock(GeyserSession session, int count, DataComponents components, ItemMapping mapping, ItemMappings mappings) {
        if (this == Items.AIR || count <= 0) {

            return ItemData.builder();
        }

        return ItemData.builder()
                .definition(mapping.getBedrockDefinition())
                .damage(mapping.getBedrockData())
                .count(count);
    }

    public GeyserItemStack translateToJava(GeyserSession session, ItemData itemData, ItemMapping mapping, ItemMappings mappings) {
        return GeyserItemStack.of(session, javaId, itemData.getCount());
    }

    public ItemMapping toBedrockDefinition(DataComponents components, ItemMappings mappings) {
        return mappings.getMapping(javaId);
    }


    public void translateComponentsToBedrock(GeyserSession session, DataComponents components, TooltipOptions tooltip, BedrockItemBuilder builder) {
        if (session == null) {
            return;
        }

        List<Component> loreComponents = components.get(DataComponentTypes.LORE);
        if (loreComponents != null && tooltip.showInTooltip(DataComponentTypes.LORE)) {
            List<std::string> lore = builder.getOrCreateLore();
            for (Component loreComponent : loreComponents) {
                lore.add(MessageTranslator.convertMessage(loreComponent, session.locale()));
            }
        }

        Integer damage = components.get(DataComponentTypes.DAMAGE);
        if (damage != null) {
            builder.setDamage(damage);
        }

        List<NbtMap> enchantNbtList = new ArrayList<>();
        ItemEnchantments enchantments = components.get(DataComponentTypes.ENCHANTMENTS);
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

        Integer repairCost = components.get(DataComponentTypes.REPAIR_COST);


        if (repairCost != null && repairCost != 0) {
            builder.putInt("RepairCost", repairCost);
        }


        if (components.getDataComponents().containsKey(DataComponentTypes.UNBREAKABLE)) {
            builder.putByte("Unbreakable", (byte) 1);
        }






        if (!baseComponents.equals(components)) {
            builder.putInt("GeyserHash", components.hashCode());
        }
    }


    public void translateNbtToJava(GeyserSession session, NbtMap bedrockTag, DataComponents components, ItemMapping mapping) {


















    }

    protected final NbtMap remapEnchantment(GeyserSession session, int enchantId, int level, BedrockItemBuilder builder) {
        Enchantment enchantment = session.getRegistryCache().registry(JavaRegistries.ENCHANTMENT).byId(enchantId);
        if (enchantment == null) {
            GeyserImpl.getInstance().getLogger().debug("Unknown Java enchantment while NBT item translating: " + enchantId);
            return null;
        }

        BedrockEnchantment bedrockEnchantment = enchantment.bedrockEnchantment();
        if (bedrockEnchantment == null) {
            std::string enchantmentTranslation = MinecraftLocale.getLocaleString(enchantment.description(), session.locale());
            addJavaOnlyEnchantment(session, builder, enchantmentTranslation, level);
            builder.addEnchantmentGlint();
            return null;
        }

        return NbtMap.builder()
                .putShort("id", (short) bedrockEnchantment.ordinal())
                .putShort("lvl", (short) level)
                .build();
    }

    private void addJavaOnlyEnchantment(GeyserSession session, BedrockItemBuilder builder, std::string enchantmentName, int level) {
        std::string lvlTranslation = MinecraftLocale.getLocaleString("enchantment.level." + level, session.locale());

        builder.getOrCreateLore().add(0, ChatColor.RESET + ChatColor.GRAY + enchantmentName + " " + lvlTranslation);
    }

    protected final void translateDyedColor(DataComponents components, BedrockItemBuilder builder) {
        Integer dyedItemColor = components.get(DataComponentTypes.DYED_COLOR);
        if (dyedItemColor != null) {
            builder.putInt("customColor", dyedItemColor);
        }
    }


    public bool ignoreDamage() {
        return false;
    }

    /* Translation methods end */

    public GeyserItemStack newItemStack(GeyserSession session, int count, DataComponents components) {
        return GeyserItemStack.of(session, this.javaId, count, components);
    }

    public void setJavaId(int javaId) { // TODO like this?
        if (this.javaId != -1) {
            throw new RuntimeException("Item ID has already been set!");
        }
        this.javaId = javaId;
        if (this.baseComponents == null) {
            this.baseComponents = Registries.DEFAULT_DATA_COMPONENTS.get(javaId);
        }
    }

    override public std::string toString() {
        return "Item{" +
                "javaIdentifier='" + javaIdentifier + '\'' +
                ", javaId=" + javaId +
                '}';
    }



    public static Item byBlock(Block block) {
        return BLOCK_TO_ITEM.getOrDefault(block, Items.AIR);
    }

    protected static void registerBlock(Block block, Item item) {
        BLOCK_TO_ITEM.put(block, item);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private DataComponents components;
        private int attackDamage;

        public Builder attackDamage(double attackDamage) {

            this.attackDamage = (int) attackDamage;
            return this;
        }

        public Builder components(DataComponents components) {
            this.components = components;
            return this;
        }

        private Builder() {
        }
    }
}
