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

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.item.ArmorMaterial;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;

public class ArmorItem extends Item {
    private final ArmorMaterial material;

    public ArmorItem(String javaIdentifier, ArmorMaterial material, Builder builder) {
        super(javaIdentifier, builder);
        this.material = material;
    }

    @Override
    public void translateNbtToBedrock(@NonNull GeyserSession session, @NonNull CompoundTag tag) {
        super.translateNbtToBedrock(session, tag);

        if (tag.get("Trim") instanceof CompoundTag trim) {
            StringTag material = trim.remove("material");
            StringTag pattern = trim.remove("pattern");
            // bedrock has an uppercase first letter key, and the value is not namespaced
            trim.put(new StringTag("Material", stripNamespace(material.getValue())));
            trim.put(new StringTag("Pattern", stripNamespace(pattern.getValue())));
        }
    }

    @Override
    public void translateNbtToJava(@NonNull CompoundTag tag, @NonNull ItemMapping mapping) {
        super.translateNbtToJava(tag, mapping);

        if (tag.get("Trim") instanceof CompoundTag trim) {
            StringTag material = trim.remove("Material");
            StringTag pattern = trim.remove("Pattern");
            // java has a lowercase key, and namespaced value
            trim.put(new StringTag("material", "minecraft:" + material.getValue()));
            trim.put(new StringTag("pattern", "minecraft:" + pattern.getValue()));
        }
    }

    @Override
    public boolean isValidRepairItem(Item other) {
        return material.getRepairIngredient() == other;
    }

    private static String stripNamespace(String identifier) {
        int i = identifier.indexOf(':');
        if (i >= 0) {
            return identifier.substring(i + 1);
        }
        return identifier;
    }
}
