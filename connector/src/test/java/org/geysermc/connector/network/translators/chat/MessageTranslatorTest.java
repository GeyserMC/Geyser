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

package org.geysermc.connector.network.translators.chat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MessageTranslatorTest {

    private Map<String, String> messages = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        messages.put("{\"text\":\"\",\"extra\":[{\"text\":\"DoctorMad9952 joined the game\",\"color\":\"yellow\"}]}",
                "§r§eDoctorMad9952 joined the game");

        messages.put("{\"text\":\"\",\"extra\":[\"Plugins (3): \",{\"text\":\"WorldEdit\",\"color\":\"green\"},{\"text\":\", \",\"color\":\"white\"},{\"text\":\"ViaVersion\",\"color\":\"green\"},{\"text\":\", \",\"color\":\"white\"},{\"text\":\"Geyser-Spigot\",\"color\":\"green\"}]}",
                "Plugins (3): §r§aWorldEdit§r§f, §r§aViaVersion§r§f, §r§aGeyser-Spigot");

        // RGB downgrade test
        messages.put("{\"extra\":[{\"text\":\"          \"},{\"color\":\"gold\",\"text\":\"The \"},{\"color\":\"#E14248\",\"obfuscated\":true,\"text\":\"||\"},{\"color\":\"#3AA9FF\",\"bold\":true,\"text\":\"CubeCraft\"},{\"color\":\"#E14248\",\"obfuscated\":true,\"text\":\"||\"},{\"color\":\"gold\",\"text\":\" Network \"},{\"color\":\"green\",\"text\":\"[1.8/1.9+]\\n         \"},{\"color\":\"#f5e342\",\"text\":\"✦ \"},{\"color\":\"#b042f5\",\"bold\":true,\"text\":\"N\"},{\"color\":\"#c142f5\",\"bold\":true,\"text\":\"E\"},{\"color\":\"#d342f5\",\"bold\":true,\"text\":\"W\"},{\"color\":\"#e442f5\",\"bold\":true,\"text\":\":\"},{\"color\":\"#f542f5\",\"bold\":true,\"text\":\" \"},{\"color\":\"#bcf542\",\"bold\":true,\"text\":\"A\"},{\"color\":\"#acee3f\",\"bold\":true,\"text\":\"M\"},{\"color\":\"#9ce73c\",\"bold\":true,\"text\":\"O\"},{\"color\":\"#8ce039\",\"bold\":true,\"text\":\"N\"},{\"color\":\"#7cd936\",\"bold\":true,\"text\":\"G\"},{\"color\":\"#6cd233\",\"bold\":true,\"text\":\" \"},{\"color\":\"#5ccb30\",\"bold\":true,\"text\":\"S\"},{\"color\":\"#4cc42d\",\"bold\":true,\"text\":\"L\"},{\"color\":\"#3cbd2a\",\"bold\":true,\"text\":\"I\"},{\"color\":\"#2cb627\",\"bold\":true,\"text\":\"M\"},{\"color\":\"#1caf24\",\"bold\":true,\"text\":\"E\"},{\"color\":\"#0ca821\",\"bold\":true,\"text\":\"S\"},{\"color\":\"#f5e342\",\"text\":\" \"},{\"color\":\"#6d7c87\",\"text\":\"(kinda sus) \"},{\"color\":\"#f5e342\",\"text\":\"✦\"}],\"text\":\"\"}",
                "          §r§6The §r§c§k||§r§3§lCubeCraft§r§c§k||§r§6 Network §r§a[1.8/1.9+]\n" +
                        "         §r§e✦ §r§d§lN§r§d§lE§r§d§lW§r§d§l:§r§d§l §r§e§lA§r§e§lM§r§a§lO§r§a§lN§r§a§lG§r§a§l §r§a§lS§r§a§lL§r§2§lI§r§2§lM§r§2§lE§r§2§lS§r§e §r§8(kinda sus) §r§e✦");

        // Color code format resetting
        messages.put("{\"text\":\"\",\"extra\":[{\"text\":\"\",\"extra\":[{\"text\":\"[\",\"color\":\"gray\"},{\"text\":\"H\",\"color\":\"yellow\"},{\"text\":\"]\",\"color\":\"gray\"},{\"text\":\" \",\"color\":\"white\"},{\"text\":\"GUEST\",\"color\":\"#b7b7b7\",\"bold\":true}]},{\"text\":\"\",\"extra\":[{\"text\":\" \",\"bold\":true},{\"text\":\"»\",\"color\":\"blue\"},{\"text\":\" \",\"color\":\"gray\"}]},{\"text\":\"\",\"extra\":[{\"text\":\"rtm516\",\"color\":\"white\"},{\"text\":\": \",\"color\":\"gray\"},{\"text\":\"\",\"color\":\"white\"}]},{\"text\":\"\",\"extra\":[{\"text\":\"This is an amazing bedrock test message\",\"color\":\"white\"}]}]}\n",
                "§r§7[§r§eH§r§7]§r§f §r§7§lGUEST§r§l §r§9»§r§7 §r§frtm516§r§7: §r§fThis is an amazing bedrock test message");

        // Test translation and positional arguments
        // Disabled due to not having an GeyserConnector instance, hence it fails
        //messages.put("{\"translate\":\"death.attack.player\",\"with\":[{\"text\":\"rtm516\",\"insertion\":\"rtm516\"},{\"text\":\"*invincible_rt\",\"insertion\":\"*invincible_rt\"}]}",
        //        "rtm516 was slain by *invincible_rt");
    }

    @Test
    public void convertMessage() {
        for (Map.Entry<String, String> entry : messages.entrySet()) {
            String bedrockMessage = MessageTranslator.convertMessage(entry.getKey(), "en_US");
            Assert.assertEquals("Translation of messages is incorrect", entry.getValue(), bedrockMessage);
        }
    }

    @Test
    public void convertMessageLenient() {
        Assert.assertEquals("All newline message is not handled properly", "\n\n\n\n", MessageTranslator.convertMessageLenient("\n\n\n\n"));
        Assert.assertEquals("Empty message is not handled properly", "", MessageTranslator.convertMessageLenient(""));
        Assert.assertEquals("Reset before message is not handled properly", "§r§eGame Selector", MessageTranslator.convertMessageLenient("§r§eGame Selector"));
        Assert.assertEquals("Unimplemented formatting chars not stripped", "Bold Underline", MessageTranslator.convertMessageLenient("§m§nBold Underline"));
    }
}
