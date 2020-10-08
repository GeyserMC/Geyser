/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.world.block.entity;

import com.github.steveice10.opennbt.tag.builtin.*;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.utils.MessageUtils;

import java.util.HashMap;
import java.util.Map;

@BlockEntity(name = "CommandBlock", regex = "command_block")
public class CommandBlockBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {

    @Override
    public Map<String, Object> translateTag(CompoundTag tag, int blockState) {
        Map<String, Object> map = new HashMap<>();
        if (tag.size() < 5) {
            return map; // These values aren't here
        }
        // Java infers from the block state, but Bedrock needs it in the tag
        map.put("conditionalMode", BlockStateValues.getCommandBlockValues().getOrDefault(blockState, (byte) 0));
        // Java and Bedrock values
        map.put("conditionMet", ((ByteTag) tag.get("conditionMet")).getValue());
        map.put("auto", ((ByteTag) tag.get("auto")).getValue());
        map.put("CustomName", MessageUtils.getBedrockMessage(((StringTag) tag.get("CustomName")).getValue()));
        map.put("powered", ((ByteTag) tag.get("powered")).getValue());
        map.put("Command", ((StringTag) tag.get("Command")).getValue());
        map.put("SuccessCount", ((IntTag) tag.get("SuccessCount")).getValue());
        map.put("TrackOutput", ((ByteTag) tag.get("TrackOutput")).getValue());
        map.put("UpdateLastExecution", ((ByteTag) tag.get("UpdateLastExecution")).getValue());
        if (tag.get("LastExecution") != null) {
            map.put("LastExecution", ((LongTag) tag.get("LastExecution")).getValue());
        } else {
            map.put("LastExecution", (long) 0);
        }
        return map;
    }

    @Override
    public boolean isBlock(int blockState) {
        return BlockStateValues.getCommandBlockValues().containsKey(blockState);
    }
}
