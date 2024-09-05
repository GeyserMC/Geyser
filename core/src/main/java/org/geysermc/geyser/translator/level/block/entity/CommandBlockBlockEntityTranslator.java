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

package org.geysermc.geyser.translator.level.block.entity;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;

@BlockEntity(type = BlockEntityType.COMMAND_BLOCK)
public class CommandBlockBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {
    @Override
    public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, BlockState blockState) {
        if (javaNbt == null || javaNbt.size() < 5) {
            return; // These values aren't here
        }
        // Java infers from the block state, but Bedrock needs it in the tag
        bedrockNbt.putBoolean("conditionalMode", blockState.getValue(Properties.CONDITIONAL));
        // Java and Bedrock values
        bedrockNbt.putByte("conditionMet", javaNbt.getByte("conditionMet"));
        bedrockNbt.putByte("auto", javaNbt.getByte("auto"));
        bedrockNbt.putString("CustomName", MessageTranslator.convertJsonMessage(javaNbt.getString("CustomName"), session.locale()));
        bedrockNbt.putByte("powered", javaNbt.getByte("powered"));
        bedrockNbt.putString("Command", javaNbt.getString("Command"));
        bedrockNbt.putInt("SuccessCount", javaNbt.getInt("SuccessCount"));
        bedrockNbt.putByte("TrackOutput", javaNbt.getByte("TrackOutput"));
        bedrockNbt.putByte("UpdateLastExecution", javaNbt.getByte("UpdateLastExecution"));
        bedrockNbt.putLong("LastExecution", javaNbt.getLong("LastExecution")); // Note: may not be present? Was a null check before 1.20.5
    }
}
