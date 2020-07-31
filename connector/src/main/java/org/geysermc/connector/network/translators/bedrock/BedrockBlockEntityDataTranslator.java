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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientUpdateSignPacket;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import java.util.HashMap;
import java.util.Map;

@Translator(packet = BlockEntityDataPacket.class)
public class BedrockBlockEntityDataTranslator extends PacketTranslator<BlockEntityDataPacket> {

    // In case two people are editing signs at the same time this array holds the temporary messages to be sent
    // Position -> Message being held
    protected static Map<Position, String> lastMessages = new HashMap<>();

    @Override
    public void translate(BlockEntityDataPacket packet, GeyserSession session) {
        NbtMap tag = packet.getData();
        if (tag.getString("id").equals("Sign")) {
            // This is the reason why this all works - Bedrock sends packets every time you update the sign, Java only wants the final packet
            // But Bedrock sends one final packet when you're done editing the sign, which should be equal to the last message since there's no edits
            // So if the latest update does not match the last cached update then it's still being edited
            Position pos = new Position(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            if (!tag.getString("Text").equals(lastMessages.get(pos))) {
                lastMessages.put(pos, tag.getString("Text"));
                return;
            }
            // Otherwise the two messages are identical and we can get to work deconstructing
            StringBuilder newMessage = new StringBuilder();
            // While Bedrock's sign lines are one string, Java's is an array of each line
            // (Initialized all with empty strings because it complains about null)
            String[] lines = new String[] {"", "", "", ""};
            int iterator = 0;
            // This converts the message into the array'd message Java wants
            for (char character : tag.getString("Text").toCharArray()) {
                // If we get a return in Bedrock, that signals to use the next line.
                if (character == '\n') {
                    lines[iterator] = newMessage.toString();
                    iterator++;
                    // Bedrock, for whatever reason, can hold a message out of bounds
                    // We don't care about that so we discard that
                    if (iterator > lines.length - 1) {
                        break;
                    }
                    newMessage = new StringBuilder();
                } else newMessage.append(character);
            }
            // Put the final line on since it isn't done in the for loop
            if (iterator < lines.length) lines[iterator] = newMessage.toString();
            ClientUpdateSignPacket clientUpdateSignPacket = new ClientUpdateSignPacket(pos, lines);
            session.sendDownstreamPacket(clientUpdateSignPacket);
            //TODO (potentially): originally I was going to update the sign blocks so Bedrock and Java users would match visually
            // However Java can still store a lot per-line and visuals are still messed up so that doesn't work

            // We remove the sign position from map to indicate there is no work-in-progress sign
            lastMessages.remove(pos);
        }

    }
}
