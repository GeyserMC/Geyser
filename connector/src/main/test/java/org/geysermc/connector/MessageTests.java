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

package org.geysermc.connector;

import org.geysermc.connector.utils.MessageUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MessageTests {

    private static final Map<String, String> JAVA_TO_BEDROCK_MESSAGES = new HashMap<String, String>() {
        {
            put("{\"text\":\"\",\"extra\":[{\"text\":\"DoctorMad9952 joined the game\",\"color\":\"yellow\"}]}",
                    "§eDoctorMad9952 joined the game");
            put("{\"text\":\"\",\"extra\":[\"Plugins (3): \",{\"text\":\"WorldEdit\",\"color\":\"green\"},{\"text\":\", \",\"color\":\"white\"},{\"text\":\"ViaVersion\",\"color\":\"green\"},{\"text\":\", \",\"color\":\"white\"},{\"text\":\"Geyser-Spigot\",\"color\":\"green\"}]}",
                    "Plugins (3): §aWorldEdit§f, §aViaVersion§f, §aGeyser-Spigot");
        }
    };

    /**
     * Test to ensure that Java messages are correctly translated to Bedrock
     */
    @Test
    public void testMessageTranslation() {
        for (Map.Entry<String, String> entry : JAVA_TO_BEDROCK_MESSAGES.entrySet()) {
            String bedrockMessage = MessageUtils.convertMessage(entry.getKey(), "en_US");
            Assert.assertEquals("Translation of messages is incorrect", bedrockMessage, entry.getValue());
        }
    }

}
