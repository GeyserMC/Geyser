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

package org.geysermc.geyser.translator.level.block.entity;

import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.nbt.NbtMapBuilder;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.SignUtils;

@BlockEntity(type = BlockEntityType.SIGN)
public class SignBlockEntityTranslator extends BlockEntityTranslator {
    /**
     * Maps a color stored in a sign's Color tag to its ARGB value.
     *
     * @param javaColor The dye color stored in the sign's Color tag.
     * @return Java Edition's integer matching the color specified
     */
    private int getBedrockSignColor(String javaColor) {
        //TODO create a DyeColor class and combine with FireworkColor???
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
            default -> 0; // The proper Java color is 1908001, but this does not render well with glow text.
        };
        // Add the transparency of the color, too.
        return dyeColor | (255 << 24);
    }

    @Override
    public void translateTag(NbtMapBuilder builder, CompoundTag tag, int blockState) {
        StringBuilder signText = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            int currentLine = i + 1;
            String signLine = getOrDefault(tag.getValue().get("Text" + currentLine), "");
            signLine = MessageTranslator.convertMessageLenient(signLine);

            // Check the character width on the sign to ensure there is no overflow that is usually hidden
            // to Java Edition clients but will appear to Bedrock clients
            int signWidth = 0;
            StringBuilder finalSignLine = new StringBuilder();
            boolean previousCharacterWasFormatting = false; // Color changes do not count for maximum width
            for (char c : signLine.toCharArray()) {
                if (c == '\u00a7') {
                    // Don't count this character
                    previousCharacterWasFormatting = true;
                } else if (previousCharacterWasFormatting) {
                    // Don't count this character either
                    previousCharacterWasFormatting = false;
                } else {
                    signWidth += SignUtils.getCharacterWidth(c);
                }

                if (signWidth <= SignUtils.BEDROCK_CHARACTER_WIDTH_MAX) {
                    finalSignLine.append(c);
                } else {
                    // Adding the character would make Bedrock move to the next line - Java doesn't do that, so we do not want to
                    break;
                }
            }

            signText.append(finalSignLine);
            signText.append("\n");
        }

        builder.putString("Text", signText.toString());

        // Java Edition 1.14 added the ability to change the text color of the whole sign using dye
        Tag color = tag.get("Color");
        if (color != null) {
            builder.putInt("SignTextColor", getBedrockSignColor(color.getValue().toString()));
        }

        // Glowing text
        boolean isGlowing = getOrDefault(tag.getValue().get("GlowingText"), (byte) 0) != (byte) 0;
        builder.putBoolean("IgnoreLighting", isGlowing);
        builder.putBoolean("TextIgnoreLegacyBugResolved", isGlowing); // ??? required
    }
}
