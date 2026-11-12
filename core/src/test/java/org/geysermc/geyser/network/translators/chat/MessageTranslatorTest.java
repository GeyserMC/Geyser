/*
 * Copyright (c) 2019-2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.translators.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MessageTranslatorTest {

    private final Map<Component, String> messages = new HashMap<>();

    @BeforeAll
    public void setUp() {
        messages.put(Component.text("").append(Component.text("DoctorMad9952 joined the game").color(NamedTextColor.YELLOW)),
            "§r§eDoctorMad9952 joined the game");

        messages.put(Component.text("")
                .append(Component.text("Plugins (3): "))
                .append(Component.text("WorldEdit").color(NamedTextColor.GREEN))
                .append(Component.text(", ").color(NamedTextColor.WHITE))
                .append(Component.text("ViaVersion").color(NamedTextColor.GREEN))
                .append(Component.text(", ").color(NamedTextColor.WHITE))
                .append(Component.text("Geyser-Spigot").color(NamedTextColor.GREEN)),
            "§rPlugins (3): §aWorldEdit§r§f, §r§aViaVersion§r§f, §r§aGeyser-Spigot");

        // RGB downgrade test
        messages.put(Component.text("")
                .append(Component.text("          "))
                .append(Component.text("The ").color(NamedTextColor.GOLD))
                .append(Component.text("||").color(TextColor.color(0xE14248)).decorate(TextDecoration.OBFUSCATED))
                .append(Component.text("CubeCraft").color(TextColor.color(0x3AA9FF)).decorate(TextDecoration.BOLD))
                .append(Component.text("||").color(TextColor.color(0xE14248)).decorate(TextDecoration.OBFUSCATED))
                .append(Component.text(" Network ").color(NamedTextColor.GOLD))
                .append(Component.text("[1.8/1.9+]").color(NamedTextColor.GREEN)),
            "§r          §6The §r§c§k||§r§b§lCubeCraft§r§c§k||§r§6 Network §r§a[1.8/1.9+]");

        // Color code format resetting
        messages.put(Component.text("")
                .append(Component.text("")
                    .append(Component.text("[").color(NamedTextColor.GRAY))
                    .append(Component.text("H").color(NamedTextColor.YELLOW))
                    .append(Component.text("]").color(NamedTextColor.GRAY))
                    .append(Component.text(" ").color(NamedTextColor.WHITE))
                    .append(Component.text("GUEST").color(TextColor.color(0xB7B7B7)).decorate(TextDecoration.BOLD)))
                .append(Component.text("")
                    // I present to you: BOLD SPACE
                    .append(Component.text(" ").decorate(TextDecoration.BOLD))
                    .append(Component.text("»").color(NamedTextColor.BLUE))
                    .append(Component.text(" ").color(NamedTextColor.GRAY)))
                .append(Component.text("")
                    .append(Component.text("rtm516").color(NamedTextColor.WHITE))
                    .append(Component.text(": ").color(NamedTextColor.GRAY))
                    .append(Component.text("").color(NamedTextColor.WHITE)))
                .append(Component.text("")
                    .append(Component.text("This is an amazing bedrock test message").color(NamedTextColor.WHITE))),
            "§r§7[§r§eH§r§7]§r§f §r§7§lGUEST§r§l §r§9»§r§7 §r§frtm516§r§7: §r§fThis is an amazing bedrock test message");

        // Test translation and positional arguments
        // Disabled due to not having an Geyser instance, hence it fails
        //messages.put("{\"translate\":\"death.attack.player\",\"with\":[{\"text\":\"rtm516\",\"insertion\":\"rtm516\"},{\"text\":\"*invincible_rt\",\"insertion\":\"*invincible_rt\"}]}",
        //        "rtm516 was slain by *invincible_rt");
        //// Test translation with the ' character (which MessageFormat requires special handling for)
        //messages.put("{\"translate\":\"commands.give.success.single\",\"with\":[{\"text\":\"1\"},{\"color\":\"yellow\",\"hoverEvent\":{\"action\":\"show_item\",\"contents\":{\"id\":\"minecraft:player_head\",\"tag\":\"{SkullOwner:\\\"Camotoy\\\"}\"}},\"translate\":\"chat.square_brackets\",\"with\":[{\"extra\":[{\"translate\":\"block.minecraft.player_head.named\",\"with\":[{\"text\":\"Camotoy\"}]}],\"text\":\"\"}]},{\"insertion\":\"DoctorMad9952\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/tell DoctorMad9952 \"},\"hoverEvent\":{\"action\":\"show_entity\",\"contents\":{\"type\":\"minecraft:player\",\"id\":\"8d712993-d208-3dac-b4d8-f2ce7e7d2b75\",\"name\":{\"text\":\"DoctorMad9952\"}}},\"extra\":[{\"text\":\"DoctorMad9952\"}],\"text\":\"\"}]}",
        //        "Gave 1 §r§e[Camotoy's Head]§r to DoctorMad9952");

        // Newline color restore
        messages.put(Component.text(" Contribute to a weekly community goal.\n All participants will receive a reward\n and the top 3 will get extra bonus prizes!").color(TextColor.color(0xF7DC77)),
            """
                §r§e Contribute to a weekly community goal.
                §e All participants will receive a reward
                §e and the top 3 will get extra bonus prizes!""");

        // Escape curly braces in translatable strings (make MessageFormat ignore them)
        messages.put(Component.translatable("tt{tt%stt}tt").arguments(Component.text("AA")), "§rtt{ttAAtt}tt");
        messages.put(Component.translatable("tt{'tt%stt'{tt").arguments(Component.text("AA")), "§rtt{'ttAAtt'{tt");
        messages.put(Component.translatable("tt{''{tt"), "§rtt{''{tt");
        messages.put(Component.translatable("tt{{''}}tt"), "§rtt{{''}}tt");

        // Remove duplicated resets, tail resets and dangling paragraph sign
        messages.put(Component.text("abc§r").color(NamedTextColor.YELLOW), "§r§eabc");
        messages.put(Component.text("abc§r§r").color(NamedTextColor.YELLOW), "§r§eabc");
        messages.put(Component.text("abc§rd").color(NamedTextColor.YELLOW), "§r§eabc§rd");
        messages.put(Component.text("abc§r§rd").color(NamedTextColor.YELLOW), "§r§eabc§rd");
        messages.put(Component.text("abc§rde").color(NamedTextColor.YELLOW), "§r§eabc§rde");
        messages.put(Component.text("abc§r§rde").color(NamedTextColor.YELLOW), "§r§eabc§rde");
        messages.put(Component.text("abc§").color(NamedTextColor.YELLOW), "§r§eabc");

        // All newlines
        messages.put(Component.text("\n\n\n\n"), "§r\n\n\n\n");
        // Empty
        messages.put(Component.empty(), "");
        // Reset before message
        messages.put(Component.text("§r§eGame Selector"), "§r§eGame Selector");
        // Duplicate/redundant reset removal
        messages.put(Component.text("§r§r§d[Test]§r"), "§r§d[Test]");

        MessageTranslator.init();
    }

    // TODO should be parameterised test
    @Test
    public void convertMessage() {
        for (Map.Entry<Component, String> entry : messages.entrySet()) {
            String bedrockMessage = MessageTranslator.convertMessage(entry.getKey(), "en_US");
            Assertions.assertEquals(entry.getValue(), bedrockMessage, "Translation of messages is incorrect");
        }
    }

    @Test
    public void convertIncomingToPlainText() {
        Assertions.assertEquals("Many colors here", MessageTranslator.convertToPlainTextLenient("{\"extra\":[{\"color\":\"red\",\"text\":\"M\"},{\"color\":\"gold\",\"text\":\"a\"},{\"color\":\"yellow\",\"text\":\"n\"},{\"color\":\"green\",\"text\":\"y \"},{\"color\":\"aqua\",\"text\":\"c\"},{\"color\":\"dark_purple\",\"text\":\"o\"},{\"color\":\"red\",\"text\":\"l\"},{\"color\":\"gold\",\"text\":\"o\"},{\"color\":\"yellow\",\"text\":\"r\"},{\"color\":\"green\",\"text\":\"s \"},{\"color\":\"aqua\",\"text\":\"h\"},{\"color\":\"dark_purple\",\"text\":\"e\"},{\"color\":\"red\",\"text\":\"r\"},{\"color\":\"gold\",\"text\":\"e\"}],\"text\":\"\"}", "en_US"), "JSON message is not handled properly");
        Assertions.assertEquals("Many colors here", MessageTranslator.convertIncomingToPlainText("§cM§6a§en§ay §bc§5o§cl§6o§er§as §bh§5e§cr§6e"), "Legacy formatted message is not handled properly (Colors)");
        Assertions.assertEquals("Many colors here", MessageTranslator.convertToPlainTextLenient("§cM§6a§en§ay §bc§5o§cl§6o§er§as §bh§5e§cr§6e", "en_US"), "Legacy formatted message is not handled properly (Colors)");
        Assertions.assertEquals("Obf Bold Strikethrough Underline Italic Reset", MessageTranslator.convertToPlainTextLenient("§kObf §lBold §mStrikethrough §nUnderline §oItalic §rReset", "en_US"), "Legacy formatted message is not handled properly (Style)");
        Assertions.assertEquals("Strange", MessageTranslator.convertToPlainTextLenient("§rStrange", "en_US"), "Valid lenient JSON is not handled properly");
        Assertions.assertEquals("", MessageTranslator.convertToPlainTextLenient("", "en_US"), "Empty message is not handled properly");
        Assertions.assertEquals("     ", MessageTranslator.convertToPlainTextLenient("     ", "en_US"), "Whitespace is not preserved");
    }
}
