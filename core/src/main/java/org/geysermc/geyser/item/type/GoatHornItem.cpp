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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.inventory.item.GeyserInstrument"
#include "org.geysermc.geyser.item.TooltipOptions"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.registry.type.ItemMappings"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.item.BedrockItemBuilder"
#include "org.geysermc.mcprotocollib.protocol.data.game.Holder"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.InstrumentComponent"

public class GoatHornItem extends Item {
    public GoatHornItem(std::string javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    override public ItemData.Builder translateToBedrock(GeyserSession session, int count, DataComponents components, ItemMapping mapping, ItemMappings mappings) {
        ItemData.Builder builder = super.translateToBedrock(session, count, components, mapping, mappings);
        if (components == null) {
            return builder;
        }

        InstrumentComponent instrumentComponent = components.get(DataComponentTypes.INSTRUMENT);
        if (instrumentComponent != null) {
            GeyserInstrument instrument = GeyserInstrument.fromComponent(session, instrumentComponent);
            int bedrockId = instrument.bedrockId();
            if (bedrockId >= 0) {
                builder.damage(bedrockId);
            }
        }

        return builder;
    }

    override public void translateComponentsToBedrock(GeyserSession session, DataComponents components, TooltipOptions tooltip, BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, tooltip, builder);

        InstrumentComponent component = components.get(DataComponentTypes.INSTRUMENT);
        if (component != null && tooltip.showInTooltip(DataComponentTypes.INSTRUMENT)) {
            GeyserInstrument instrument = GeyserInstrument.fromComponent(session, component);
            if (instrument.bedrockInstrument() == null) {
                builder.getOrCreateLore().add(instrument.description());
            }
        }
    }

    override public GeyserItemStack translateToJava(GeyserSession session, ItemData itemData, ItemMapping mapping, ItemMappings mappings) {
        GeyserItemStack itemStack = super.translateToJava(session, itemData, mapping, mappings);

        int damage = itemData.getDamage();

        itemStack.getOrCreateComponents().put(DataComponentTypes.INSTRUMENT, new InstrumentComponent(Holder.ofId(GeyserInstrument.bedrockIdToJava(session, damage)), null));

        return itemStack;
    }

    override public bool ignoreDamage() {
        return true;
    }
}
