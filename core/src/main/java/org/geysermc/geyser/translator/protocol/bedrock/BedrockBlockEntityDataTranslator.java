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

package org.geysermc.geyser.translator.protocol.bedrock;

import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetJigsawBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundSignUpdatePacket;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.SignUtils;

@Translator(packet = BlockEntityDataPacket.class)
public class BedrockBlockEntityDataTranslator extends PacketTranslator<BlockEntityDataPacket> {

    @Override
    public void translate(GeyserSession session, BlockEntityDataPacket packet) {
        NbtMap tag = packet.getData();
        String id = tag.getString("id");
        if (id.endsWith("Sign")) {
            
            int widthMax = SignUtils.getSignWidthMax(id.startsWith("Hanging"));

            String text = MessageTranslator.convertIncomingToPlainText(
                tag.getCompound(session.getWorldCache().isEditingSignOnFront() ? "FrontText" : "BackText").getString("Text"));
            
            
            StringBuilder newMessage = new StringBuilder();
            
            
            String[] lines = new String[] {"", "", "", ""};
            int iterator = 0;
            
            
            int widthCount = 0;
            
            for (char character : text.toCharArray()) {
                widthCount += SignUtils.getCharacterWidth(character);

                
                if (character == '\n' || widthCount > widthMax) {
                    
                    boolean wentOverMax = widthCount > widthMax && character != '\n';
                    widthCount = 0;
                    
                    String word = null;
                    if (wentOverMax && iterator < lines.length - 1) {
                        
                        
                        int index = newMessage.lastIndexOf(" ");
                        if (index != -1) {
                            
                            word = newMessage.substring(index + 1);
                            
                            newMessage.delete(index, newMessage.length());
                        }
                    }
                    lines[iterator] = newMessage.toString();
                    iterator++;
                    
                    
                    if (iterator > lines.length - 1) {
                        break;
                    }
                    newMessage = new StringBuilder();
                    if (wentOverMax) {
                        
                        if (word != null) {
                            newMessage.append(word);
                            
                            for (char wordCharacter : word.toCharArray()) {
                                widthCount += SignUtils.getCharacterWidth(wordCharacter);
                            }
                        }
                        
                        newMessage.append(character);
                        widthCount += SignUtils.getCharacterWidth(character);
                    }
                } else newMessage.append(character);
            }
            
            if (iterator < lines.length) lines[iterator] = newMessage.toString();
            Vector3i pos = Vector3i.from(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            ServerboundSignUpdatePacket signUpdatePacket = new ServerboundSignUpdatePacket(pos, lines, session.getWorldCache().isEditingSignOnFront());
            session.sendDownstreamGamePacket(signUpdatePacket);

        } else if (id.equals("JigsawBlock")) {
            
            Vector3i pos = Vector3i.from(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            String name = tag.getString("name");
            String target = tag.getString("target");
            String pool = tag.getString("target_pool");
            String finalState = tag.getString("final_state");
            String joint = tag.getString("joint");
            
            ServerboundSetJigsawBlockPacket jigsawPacket = new ServerboundSetJigsawBlockPacket(pos,
                    MinecraftKey.key(name), MinecraftKey.key(target), MinecraftKey.key(pool),
                    finalState, joint, 0, 0);
            session.sendDownstreamGamePacket(jigsawPacket);
        }

    }
}
