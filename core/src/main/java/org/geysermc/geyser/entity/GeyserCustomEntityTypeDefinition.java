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

package org.geysermc.geyser.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.geysermc.geyser.entity.factory.EntityFactory;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.translator.entity.EntityMetadataTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.BuiltinEntityType;

import java.util.List;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
// TODO
public class GeyserCustomEntityTypeDefinition<T extends Entity> extends EntityTypeDefinition<T> {

    public GeyserCustomEntityTypeDefinition(EntityFactory<T> factory, GeyserEntityType type, BedrockEntityDefinition definition, List<EntityMetadataTranslator<? super T, ?, ?>> translators) {
        super(factory, type, 0, 0,0, definition, translators);
    }

    @Override
    public boolean is(BuiltinEntityType builtinEntityType) {
        return false;
    }
}
