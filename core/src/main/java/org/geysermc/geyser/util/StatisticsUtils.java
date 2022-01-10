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

package org.geysermc.geyser.util;

import com.github.steveice10.mc.protocol.data.game.statistic.*;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.cumulus.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.text.MinecraftLocale;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatisticsUtils {
    private static final Pattern CONTENT_PATTERN = Pattern.compile("^\\S+:", Pattern.MULTILINE);

    /**
     * Build a form for the given session with all statistic categories
     *
     * @param session The session to build the form for
     */
    public static void buildAndSendStatisticsMenu(GeyserSession session) {
        // Cache the language for cleaner access
        String language = session.getLocale();

        session.sendForm(
                SimpleForm.builder()
                        .translator(StatisticsUtils::translate, language)
                        .title("gui.stats")
                        .button("stat.generalButton", FormImage.Type.PATH, "textures/ui/World")
                        .button("stat.itemsButton - stat_type.minecraft.mined", FormImage.Type.PATH, "textures/items/iron_pickaxe")
                        .button("stat.itemsButton - stat_type.minecraft.broken", FormImage.Type.PATH, "textures/items/record_11")
                        .button("stat.itemsButton - stat_type.minecraft.crafted", FormImage.Type.PATH, "textures/blocks/crafting_table_side")
                        .button("stat.itemsButton - stat_type.minecraft.used", FormImage.Type.PATH, "textures/ui/Wrenches1")
                        .button("stat.itemsButton - stat_type.minecraft.picked_up", FormImage.Type.PATH, "textures/blocks/chest_front")
                        .button("stat.itemsButton - stat_type.minecraft.dropped", FormImage.Type.PATH, "textures/ui/trash_default")
                        .button("stat.mobsButton - geyser.statistics.killed", FormImage.Type.PATH, "textures/items/diamond_sword")
                        .button("stat.mobsButton - geyser.statistics.killed_by", FormImage.Type.PATH, "textures/ui/wither_heart_flash")
                        .responseHandler((form, responseData) -> {
                            SimpleFormResponse response = form.parseResponse(responseData);
                            if (!response.isCorrect()) {
                                return;
                            }

                            SimpleForm.Builder builder =
                                    SimpleForm.builder()
                                            .translator(StatisticsUtils::translate, language);

                            List<String> content = new ArrayList<>();

                            ItemMappings mappings = session.getItemMappings();
                            switch (response.getClickedButtonId()) {
                                case 0:
                                    builder.title("stat.generalButton");

                                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                                        if (entry.getKey() instanceof CustomStatistic statistic) {
                                            String statName = statistic.name().toLowerCase(Locale.ROOT);
                                            IntFunction<String> formatter = StatisticFormatters.get(statistic.getFormat());
                                            content.add("stat.minecraft." + statName + ": " + formatter.apply(entry.getValue()));
                                        }
                                    }
                                    break;
                                case 1:
                                    builder.title("stat.itemsButton - stat_type.minecraft.mined");

                                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                                        if (entry.getKey() instanceof BreakBlockStatistic statistic) {
                                            String identifier = BlockRegistries.CLEAN_JAVA_IDENTIFIERS.get(statistic.getId());
                                            if (identifier != null) {
                                                String block = identifier.replace("minecraft:", "block.minecraft.");
                                                content.add(block + ": " + entry.getValue());
                                            }
                                        }
                                    }
                                    break;
                                case 2:
                                    builder.title("stat.itemsButton - stat_type.minecraft.broken");

                                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                                        if (entry.getKey() instanceof BreakItemStatistic statistic) {
                                            String item = mappings.getMapping(statistic.getId()).getJavaIdentifier();
                                            content.add(getItemTranslateKey(item, language) + ": " + entry.getValue());
                                        }
                                    }
                                    break;
                                case 3:
                                    builder.title("stat.itemsButton - stat_type.minecraft.crafted");

                                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                                        if (entry.getKey() instanceof CraftItemStatistic statistic) {
                                            String item = mappings.getMapping(statistic.getId()).getJavaIdentifier();
                                            content.add(getItemTranslateKey(item, language) + ": " + entry.getValue());
                                        }
                                    }
                                    break;
                                case 4:
                                    builder.title("stat.itemsButton - stat_type.minecraft.used");

                                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                                        if (entry.getKey() instanceof UseItemStatistic statistic) {
                                            String item = mappings.getMapping(statistic.getId()).getJavaIdentifier();
                                            content.add(getItemTranslateKey(item, language) + ": " + entry.getValue());
                                        }
                                    }
                                    break;
                                case 5:
                                    builder.title("stat.itemsButton - stat_type.minecraft.picked_up");

                                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                                        if (entry.getKey() instanceof PickupItemStatistic statistic) {
                                            String item = mappings.getMapping(statistic.getId()).getJavaIdentifier();
                                            content.add(getItemTranslateKey(item, language) + ": " + entry.getValue());
                                        }
                                    }
                                    break;
                                case 6:
                                    builder.title("stat.itemsButton - stat_type.minecraft.dropped");

                                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                                        if (entry.getKey() instanceof DropItemStatistic statistic) {
                                            String item = mappings.getMapping(statistic.getId()).getJavaIdentifier();
                                            content.add(getItemTranslateKey(item, language) + ": " + entry.getValue());
                                        }
                                    }
                                    break;
                                case 7:
                                    builder.title("stat.mobsButton - geyser.statistics.killed");

                                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                                        if (entry.getKey() instanceof KillEntityStatistic statistic) {
                                            String entityName = statistic.getEntity().name().toLowerCase(Locale.ROOT);
                                            content.add("entity.minecraft." + entityName + ": " + entry.getValue());
                                        }
                                    }
                                    break;
                                case 8:
                                    builder.title("stat.mobsButton - geyser.statistics.killed_by");

                                    for (Map.Entry<Statistic, Integer> entry : session
                                            .getStatistics().entrySet()) {
                                        if (entry.getKey() instanceof KilledByEntityStatistic statistic) {
                                            String entityName = statistic.getEntity().name().toLowerCase(Locale.ROOT);
                                            content.add("entity.minecraft." + entityName + ": " + entry.getValue());
                                        }
                                    }
                                    break;
                                default:
                                    return;
                            }

                            StringBuilder assembledContent = new StringBuilder();
                            if (content.size() == 0) {
                                assembledContent.append("geyser.statistics.none");
                            } else {
                                content.replaceAll(x -> translate(x, language));
                                // Sort statistics alphabetically
                                content.sort(String::compareTo);
                                for (int i = 0; i < content.size(); i++) {
                                    assembledContent.append(content.get(i));
                                    // Make every other line gray
                                    if (i % 2 == 0) {
                                        assembledContent.append("\u00a77\n");
                                    } else {
                                        assembledContent.append("\u00a7r\n");
                                    }
                                }
                            }

                            session.sendForm(
                                    builder.content(assembledContent.toString())
                                            .button("gui.back", FormImage.Type.PATH, "textures/gui/newgui/undo")
                                            .responseHandler((form1, subFormResponseData) -> {
                                                SimpleFormResponse response1 = form.parseResponse(subFormResponseData);
                                                if (response1.isCorrect()) {
                                                    buildAndSendStatisticsMenu(session);
                                                }
                                            }));
                        }));
    }

    /**
     * Finds the item translation key from the Java locale.
     *
     * @param item     the namespaced item to search for.
     * @param language the language to search in
     * @return the full name of the item
     */
    private static String getItemTranslateKey(String item, String language) {
        item = item.replace("minecraft:", "item.minecraft.");
        String translatedItem = MinecraftLocale.getLocaleString(item, language);
        if (translatedItem.equals(item)) {
            // Didn't translate; must be a block
            translatedItem = MinecraftLocale.getLocaleString(item.replace("item.", "block."), language);
        }
        return translatedItem;
    }

    private static String translate(String keys, String locale) {
        Matcher matcher = CONTENT_PATTERN.matcher(keys);

        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String group = matcher.group();
            matcher.appendReplacement(buffer, translateEntry(group.substring(0, group.length() - 1), locale) + ":");
        }

        if (buffer.length() != 0) {
            return matcher.appendTail(buffer).toString();
        }

        String[] keySplitted = keys.split(" - ");
        for (int i = 0; i < keySplitted.length; i++) {
            keySplitted[i] = translateEntry(keySplitted[i], locale);
        }
        return String.join(" - ", keySplitted);
    }

    private static String translateEntry(String key, String locale) {
        if (key.startsWith("geyser.")) {
            return GeyserLocale.getPlayerLocaleString(key, locale);
        }
        return MinecraftLocale.getLocaleString(key, locale);
    }
}
