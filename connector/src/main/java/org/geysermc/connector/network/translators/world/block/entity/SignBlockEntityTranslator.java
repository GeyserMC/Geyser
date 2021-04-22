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

package org.geysermc.connector.network.translators.world.block.entity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.nbt.NbtMapBuilder;
import org.geysermc.connector.network.translators.chat.MessageTranslator;
import org.geysermc.connector.utils.SignUtils;

@BlockEntity(name = "Sign")
public class SignBlockEntityTranslator extends BlockEntityTranslator {
    /**
     * Maps a color stored in a sign's Color tag to a Bedrock Edition formatting code.
     * <br>
     * The color names correspond to dye names, because of this we can't use a more global method.
     *
     * @param javaColor The dye color stored in the sign's Color tag.
     * @return A Bedrock Edition formatting code for valid dye colors, otherwise an empty string.
     */
    private String getBedrockSignColor(String javaColor) {
        String base = "\u00a7";
        switch (javaColor) {
            case "white":
                base += 'f';
                break;
            case "orange":
                base += '6';
                break;
            case "magenta":
            case "purple":
                base += '5';
                break;
            case "light_blue":
                base += 'b';
                break;
            case "yellow":
                base += 'e';
                break;
            case "lime":
                base += 'a';
                break;
            case "pink":
                base += 'd';
                break;
            case "gray":
                base += '8';
                break;
            case "light_gray":
                base += '7';
                break;
            case "cyan":
                base += '3';
                break;
            case "blue":
                base += '9';
                break;
            case "brown": // Brown does not have a bedrock counterpart.
            case "red": // In Java Edition light red (&c) can only be applied using commands. Red dye gives &4.
                base += '4';
                break;
            case "green":
                base += '2';
                break;
            case "black":
                base += '0';
                break;
            default:
                return "";
        }
        return base;
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

            // Java Edition 1.14 added the ability to change the text color of the whole sign using dye
            Tag color = tag.get("Color");
            if (color != null) {
                signText.append(getBedrockSignColor(color.getValue().toString()));
            }

            signText.append(finalSignLine.toString());
            signText.append("\n");
        }

        builder.put("Text", signText.toString());
    }
}
