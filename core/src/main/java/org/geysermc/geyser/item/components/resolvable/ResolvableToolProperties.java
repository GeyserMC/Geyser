/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.components.resolvable;

import org.geysermc.geyser.api.item.custom.v2.component.java.JavaTool;
import org.geysermc.geyser.impl.HoldersImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;

import java.util.List;

public record ResolvableToolProperties(JavaTool properties) implements ResolvableComponent<ToolData> {

    @Override
    public DataComponentType<ToolData> type() {
        return DataComponentTypes.TOOL;
    }

    @Override
    public ToolData resolve(GeyserSession session) {
        List<ToolData.Rule> rules = properties.rules().stream()
            .map(rule -> new ToolData.Rule(((HoldersImpl) rule.blocks()).toHolderSet(session, JavaRegistries.BLOCK), rule.speed(), null))
            .toList();
        return new ToolData(rules, properties.defaultMiningSpeed(), 0, properties.canDestroyBlocksInCreative());
    }
}
