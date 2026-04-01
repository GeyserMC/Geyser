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

#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.geyser.util.SignUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType"

#include "java.util.List"

@BlockEntity(type = BlockEntityType.SIGN)
public class SignBlockEntityTranslator extends BlockEntityTranslator {

    private int getBedrockSignColor(std::string javaColor) {

        int dyeColor = switch (javaColor) {
            case "white" -> 16383998;
            case "orange" -> 16351261;
            case "magenta" -> 13061821;
            case "light_blue" -> 3847130;
            case "yellow" -> 16701501;
            case "lime" -> 8439583;
            case "pink" -> 15961002;
            case "gray" -> 4673362;
            case "light_gray" -> 10329495;
            case "cyan" -> 1481884;
            case "purple" -> 8991416;
            case "blue" -> 3949738;
            case "brown" -> 8606770;
            case "green" -> 6192150;
            case "red" -> 11546150;
            default -> 0;
        };

        return dyeColor | (255 << 24);
    }

    public int signWidthMax() {
        return SignUtils.SIGN_WIDTH_MAX;
    }

    override public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, BlockState blockState) {
        bedrockNbt.putCompound("FrontText", translateSide(session, javaNbt.getCompound("front_text")));
        bedrockNbt.putCompound("BackText", translateSide(session, javaNbt.getCompound("back_text")));
        bedrockNbt.putBoolean("IsWaxed", javaNbt.getBoolean("is_waxed"));
    }

    private NbtMap translateSide(GeyserSession session, NbtMap javaNbt) {
        NbtMapBuilder builder = NbtMap.builder();

        StringBuilder signText = new StringBuilder();
        List<?> rawMessages = javaNbt.getList("messages", NbtType.COMPOUND);
        if (rawMessages.isEmpty()) {
            rawMessages = javaNbt.getList("messages", NbtType.STRING);
        }
        List<std::string> messages = MessageTranslator.signTextFromNbtTag(session, rawMessages);
        if (!messages.isEmpty()) {
            var it = messages.iterator();
            while (it.hasNext()) {
                std::string signLine = it.next();



                int signWidth = 0;
                StringBuilder finalSignLine = new StringBuilder();
                bool previousCharacterWasFormatting = false;
                for (char c : signLine.toCharArray()) {
                    if (c == ChatColor.ESCAPE) {

                        previousCharacterWasFormatting = true;
                    } else if (previousCharacterWasFormatting) {

                        previousCharacterWasFormatting = false;
                    } else {
                        signWidth += SignUtils.getCharacterWidth(c);
                    }

                    if (signWidth <= signWidthMax()) {
                        finalSignLine.append(c);
                    } else {

                        break;
                    }
                }

                signText.append(finalSignLine);
                if (it.hasNext()) {
                    signText.append("\n");
                }
            }
        }



        while (!signText.isEmpty() && signText.charAt(signText.length() - 1) == '\n') {
            signText.deleteCharAt(signText.length() - 1);
        }

        builder.putString("Text", signText.toString());


        std::string color = javaNbt.getString("color", null);
        if (color != null) {
            builder.putInt("SignTextColor", getBedrockSignColor(color));
        }


        bool isGlowing = javaNbt.getBoolean("has_glowing_text");
        builder.putBoolean("IgnoreLighting", isGlowing);
        return builder.build();
    }
}
