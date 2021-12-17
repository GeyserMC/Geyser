/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.inventory.item.nbt;

import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemRemapper;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.util.MathUtils;

@ItemRemapper
public class FireworkRocketTranslator extends FireworkBaseTranslator {

    @Override
    public void translateToBedrock(GeyserSession session, CompoundTag itemTag, ItemMapping mapping) {
        CompoundTag fireworks = itemTag.get("Fireworks");
        if (fireworks == null) {
            return;
        }

        if (fireworks.get("Flight") != null) {
            fireworks.put(new ByteTag("Flight", MathUtils.getNbtByte(fireworks.get("Flight").getValue())));
        }

        ListTag explosions = fireworks.get("Explosions");
        if (explosions == null) {
            return;
        }
        for (Tag effect : explosions.getValue()) {
            CompoundTag effectData = (CompoundTag) effect;
            CompoundTag newEffectData = translateExplosionToBedrock(effectData, "");

            explosions.remove(effectData);
            explosions.add(newEffectData);
        }
    }

    @Override
    public void translateToJava(CompoundTag itemTag, ItemMapping mapping) {
        CompoundTag fireworks = itemTag.get("Fireworks");
        if (fireworks == null) {
            return;
        }

        if (fireworks.contains("Flight")) {
            fireworks.put(new ByteTag("Flight", MathUtils.getNbtByte(fireworks.get("Flight").getValue())));
        }

        ListTag explosions = fireworks.get("Explosions");
        if (explosions == null) {
            return;
        }
        for (Tag effect : explosions.getValue()) {
            CompoundTag effectData = (CompoundTag) effect;
            CompoundTag newEffectData = translateExplosionToJava(effectData, "");

            explosions.remove(effect);
            explosions.add(newEffectData);
        }
    }

    @Override
    public boolean acceptItem(ItemMapping mapping) {
        return "minecraft:firework_rocket".equals(mapping.getJavaIdentifier());
    }
}
