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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.TrimMaterial;
import org.cloudburstmc.protocol.bedrock.data.TrimPattern;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ArmorTrim;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

public class ArmorItem extends Item {

    public ArmorItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull DataComponents components, @NonNull BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, builder);

        ArmorTrim trim = components.get(DataComponentType.TRIM);
        if (trim != null) {
            TrimMaterial material = session.getRegistryCache().trimMaterials().byId(trim.material().id());
            TrimPattern pattern = session.getRegistryCache().trimPatterns().byId(trim.pattern().id());

            // discard custom trim patterns/materials to prevent visual glitches on bedrock
            if (!getNamespace(material.getMaterialId()).equals("minecraft")
                    || !getNamespace(pattern.getPatternId()).equals("minecraft")) {
                return;
            }

            NbtMapBuilder trimBuilder = NbtMap.builder();
            // bedrock has an uppercase first letter key, and the value is not namespaced
            trimBuilder.put("Material", material.getMaterialId());
            trimBuilder.put("Pattern", pattern.getPatternId());
            builder.putCompound("Trim", trimBuilder.build());
        }
    }

    // TODO maybe some kind of namespace util?
    private static String getNamespace(String identifier) {
        int i = identifier.indexOf(':');
        if (i >= 0) {
            return identifier.substring(0, i);
        }
        return "minecraft";
    }
}
