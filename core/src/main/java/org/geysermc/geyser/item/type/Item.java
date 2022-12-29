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
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;

import javax.annotation.Nullable;
import java.util.Set;

public class Item {
    private final String javaIdentifier;
    private int javaId = -1;
    private final int stackSize;
    private final String toolType;
    private final String toolTier;
    private final int maxDamage;
    private final Set<String> repairMaterials;
    private final boolean hasSuspiciousStewEffect;

    public Item(String javaIdentifier, Builder builder) {
        this.javaIdentifier = Identifier.formalize(javaIdentifier).intern();
        this.stackSize = builder.stackSize;
        this.toolType = builder.toolType;
        this.toolTier = builder.toolTier;
        this.maxDamage = builder.maxDamage;
        this.repairMaterials = builder.repairMaterials;
        this.hasSuspiciousStewEffect = builder.hasSuspiciousStewEffect;
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

    public int maxStackSize() {
        return stackSize;
    }

    public ItemData.Builder translateToBedrock(ItemStack itemStack, ItemMapping mapping, ItemMappings mappings) {
        if (itemStack == null) {
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

    public ItemStack translateToJava(@Nullable ItemData itemData, ItemMapping mapping, ItemMappings mappings) {
        if (itemData == null) return null;
        if (itemData.getTag() == null) {
            return new ItemStack(javaId, itemData.getCount(), new CompoundTag(""));
        }
        return new ItemStack(javaId, itemData.getCount(), ItemTranslator.translateToJavaNBT("", itemData.getTag()));
    }

    public ItemMapping toBedrockDefinition(CompoundTag nbt, ItemMappings mappings) {
        return mappings.getMapping(javaId);
    }

    public ItemStack newItemStack(int count, CompoundTag tag) {
        return new ItemStack(this.javaId, count, tag);
    }

    public void setJavaId(int javaId) { // TODO like this?
        if (this.javaId != -1) { // ID has already been set.
            throw new RuntimeException();
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
        private String toolType;
        private String toolTier;
        private int maxDamage;
        private Set<String> repairMaterials;
        private boolean hasSuspiciousStewEffect;

        public Builder stackSize(int stackSize) {
            this.stackSize = stackSize;
            return this;
        }

        public Builder setToolType(String toolType) {
            this.toolType = toolType;
            return this;
        }

        public Builder setToolTier(String toolTier) {
            this.toolTier = toolTier;
            return this;
        }

        public Builder maxDamage(int maxDamage) {
            this.maxDamage = maxDamage;
            return this;
        }

        public Builder setRepairMaterials(Set<String> repairMaterials) {
            this.repairMaterials = repairMaterials;
            return this;
        }

        public Builder setHasSuspiciousStewEffect(boolean hasSuspiciousStewEffect) {
            this.hasSuspiciousStewEffect = hasSuspiciousStewEffect;
            return this;
        }

        private Builder() {
        }
    }
}
