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

package org.geysermc.geyser.translator.protocol.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetJigsawBlockPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundSignUpdatePacket;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.SignUtils;

@Translator(packet = BlockEntityDataPacket.class)
public class BedrockBlockEntityDataTranslator extends PacketTranslator<BlockEntityDataPacket> {

    @Override
    public void translate(GeyserSession session, BlockEntityDataPacket packet) {
        NbtMap tag = packet.getData();
        if (tag.getString("id").equals("Sign")) {
            // This is the reason why this all works - Bedrock sends packets every time you update the sign, Java only wants the final packet
            // But Bedrock sends one final packet when you're done editing the sign, which should be equal to the last message since there's no edits
            // So if the latest update does not match the last cached update then it's still being edited
            if (!tag.getString("Text").equals(session.getLastSignMessage())) {
                session.setLastSignMessage(tag.getString("Text"));
                return;
            }
            // Otherwise the two messages are identical and we can get to work deconstructing
            StringBuilder newMessage = new StringBuilder();
            // While Bedrock's sign lines are one string, Java's is an array of each line
            // (Initialized all with empty strings because it complains about null)
            String[] lines = new String[] {"", "", "", ""};
            int iterator = 0;
            // Keep track of the width of each character
            // If it goes over the maximum, we need to start a new line to match Java
            int widthCount = 0;
            // This converts the message into the array'd message Java wants
            for (char character : tag.getString("Text").toCharArray()) {
                widthCount += SignUtils.getCharacterWidth(character);
                // If we get a return in Bedrock, or go over the character width max, that signals to use the next line.
                if (character == '\n' || widthCount > SignUtils.JAVA_CHARACTER_WIDTH_MAX) {
                    // We need to apply some more logic if we went over the character width max
                    boolean wentOverMax = widthCount > SignUtils.JAVA_CHARACTER_WIDTH_MAX && character != '\n';
                    widthCount = 0;
                    // Saves if we're moving a word to the next line
                    String word = null;
                    if (wentOverMax && iterator < lines.length - 1) {
                        // If we went over the max, we want to try to wrap properly like Bedrock does.
                        // So we look for a space in the Bedrock user's text to imply a word.
                        int index = newMessage.lastIndexOf(" ");
                        if (index != -1) {
                            // There is indeed a space in this line; let's get it
                            word = newMessage.substring(index + 1);
                            // 'Delete' that word from the string builder
                            newMessage.delete(index, newMessage.length());
                        }
                    }
                    lines[iterator] = newMessage.toString();
                    iterator++;
                    // Bedrock, for whatever reason, can hold a message out of the bounds of the four lines
                    // We don't care about that so we discard that
                    if (iterator > lines.length - 1) {
                        break;
                    }
                    newMessage = new StringBuilder();
                    if (wentOverMax) {
                        // Apply the wrapped word to the new line
                        if (word != null) {
                            newMessage.append(word);
                            // And apply the width count
                            for (char wordCharacter : word.toCharArray()) {
                                widthCount += SignUtils.getCharacterWidth(wordCharacter);
                            }
                        }
                        // If we went over the max, we want to append the character to the new line.
                        newMessage.append(character);
                        widthCount += SignUtils.getCharacterWidth(character);
                    }
                } else newMessage.append(character);
            }
            // Put the final line on since it isn't done in the for loop
            if (iterator < lines.length) lines[iterator] = newMessage.toString();
            Position pos = new Position(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            ServerboundSignUpdatePacket signUpdatePacket = new ServerboundSignUpdatePacket(pos, lines);
            session.sendDownstreamPacket(signUpdatePacket);

            // We set the sign text cached in the session to null to indicate there is no work-in-progress sign
            session.setLastSignMessage(null);

        } else if (tag.getString("id").equals("JigsawBlock")) {
            // Client has just sent a jigsaw block update
            Position pos = new Position(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            String name = tag.getString("name");
            String target = tag.getString("target");
            String pool = tag.getString("target_pool");
            String finalState = tag.getString("final_state");
            String joint = tag.getString("joint");
            ServerboundSetJigsawBlockPacket jigsawPacket = new ServerboundSetJigsawBlockPacket(pos, name, target, pool,
                    finalState, joint);
            session.sendDownstreamPacket(jigsawPacket);
        }

    }
}
