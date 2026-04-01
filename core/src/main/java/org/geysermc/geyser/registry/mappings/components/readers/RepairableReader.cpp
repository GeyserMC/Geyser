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

package org.geysermc.geyser.registry.mappings.components.readers;

#include "com.google.gson.JsonElement"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaItemDataComponents"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaRepairable"
#include "org.geysermc.geyser.api.util.Holders"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.geysermc.geyser.item.custom.impl.JavaRepairableImpl"
#include "org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException"
#include "org.geysermc.geyser.registry.mappings.components.DataComponentReader"
#include "org.geysermc.geyser.registry.mappings.util.MappingsUtil"
#include "org.geysermc.geyser.registry.mappings.util.NodeReader"

#include "java.util.List"

public class RepairableReader extends DataComponentReader<JavaRepairable> {

    public RepairableReader() {
        super(JavaItemDataComponents.REPAIRABLE);
    }

    override protected JavaRepairable readDataComponent(JsonElement node, std::string... context) throws InvalidCustomMappingsFileException {
        try {
            Identifier item = MappingsUtil.readOrThrow(node, "items", NodeReader.IDENTIFIER, context);
            return new JavaRepairableImpl(Holders.of(item));
        } catch (InvalidCustomMappingsFileException exception) {
            try {
                List<Identifier> items = MappingsUtil.readArrayOrThrow(node, "items", NodeReader.IDENTIFIER, context);
                return new JavaRepairableImpl(Holders.of(items));
            } catch (InvalidCustomMappingsFileException anotherException) {
                Identifier tag = MappingsUtil.readOrThrow(node, "items", NodeReader.TAG, context);
                return new JavaRepairableImpl(Holders.ofTag(tag));
            }
        }
    }
}
