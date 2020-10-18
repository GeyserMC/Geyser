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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.MagicValues;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.data.game.statistic.*;
import org.geysermc.common.window.CustomFormBuilder;
import org.geysermc.common.window.CustomFormWindow;
import org.geysermc.common.window.SimpleFormWindow;
import org.geysermc.common.window.button.FormButton;
import org.geysermc.common.window.component.LabelComponent;
import org.geysermc.common.window.response.SimpleFormResponse;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;

import java.util.Map;

public class StatisticsUtils {

    // Used in UpstreamPacketHandler.java
    public static final int STATISTICS_MENU_FORM_ID = 1339;
    public static final int STATISTICS_LIST_FORM_ID = 1340;

    /**
     * Build a form for the given session with all statistic categories
     *
     * @param session The session to build the form for
     */
    public static SimpleFormWindow buildMenuForm(GeyserSession session) {
        // Cache the language for cleaner access
        String language = session.getClientData().getLanguageCode();

        SimpleFormWindow window = new SimpleFormWindow(LocaleUtils.getLocaleString("gui.stats", language), "");

        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("stat.generalButton", language)));

        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("stat.itemsButton", language) + " - " + LocaleUtils.getLocaleString("stat_type.minecraft.mined", language)));
        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("stat.itemsButton", language) + " - " + LocaleUtils.getLocaleString("stat_type.minecraft.broken", language)));
        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("stat.itemsButton", language) + " - " + LocaleUtils.getLocaleString("stat_type.minecraft.crafted", language)));
        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("stat.itemsButton", language) + " - " + LocaleUtils.getLocaleString("stat_type.minecraft.used", language)));
        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("stat.itemsButton", language) + " - " + LocaleUtils.getLocaleString("stat_type.minecraft.picked_up", language)));
        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("stat.itemsButton", language) + " - " + LocaleUtils.getLocaleString("stat_type.minecraft.dropped", language)));

        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("stat.mobsButton", language) + " - " + LanguageUtils.getPlayerLocaleString("geyser.statistics.killed", language)));
        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("stat.mobsButton", language) + " - " + LanguageUtils.getPlayerLocaleString("geyser.statistics.killed_by", language)));

        return window;
    }

    /**
     * Handle the menu form response
     *
     * @param session The session that sent the response
     * @param response The response string to parse
     * @return True if the form was parsed correctly, false if not
     */
    public static boolean handleMenuForm(GeyserSession session, String response) {
        SimpleFormWindow menuForm = (SimpleFormWindow) session.getWindowCache().getWindows().get(STATISTICS_MENU_FORM_ID);
        menuForm.setResponse(response);
        SimpleFormResponse formResponse = (SimpleFormResponse) menuForm.getResponse();

        // Cache the language for cleaner access
        String language = session.getClientData().getLanguageCode();

        if (formResponse != null && formResponse.getClickedButton() != null) {
            CustomFormBuilder builder = new CustomFormBuilder(LocaleUtils.getLocaleString("gui.stats", language));

            switch (formResponse.getClickedButtonId()) {
                case 0:
                    builder.setTitle(LocaleUtils.getLocaleString("stat.generalButton", language));

                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                        if (entry.getKey() instanceof GenericStatistic) {
                            builder.addComponent(new LabelComponent(LocaleUtils.getLocaleString("stat.minecraft." + ((GenericStatistic) entry.getKey()).name().toLowerCase(), language) + ": " + entry.getValue()));
                        }
                    }
                    break;
                case 1:
                    builder.setTitle(LocaleUtils.getLocaleString("stat.itemsButton", language) + " - " + LocaleUtils.getLocaleString("stat_type.minecraft.mined", language));

                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                        if (entry.getKey() instanceof BreakBlockStatistic) {
                            String block = BlockTranslator.JAVA_ID_TO_JAVA_IDENTIFIER_MAP.get(((BreakBlockStatistic) entry.getKey()).getId());
                            block = block.replace("minecraft:", "block.minecraft.");
                            block = LocaleUtils.getLocaleString(block, language);
                            builder.addComponent(new LabelComponent(block + ": " + entry.getValue()));
                        }
                    }
                    break;
                case 2:
                    builder.setTitle(LocaleUtils.getLocaleString("stat.itemsButton", language) + " - " + LocaleUtils.getLocaleString("stat_type.minecraft.broken", language));

                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                        if (entry.getKey() instanceof BreakItemStatistic) {
                            String item = ItemRegistry.ITEM_ENTRIES.get(((BreakItemStatistic) entry.getKey()).getId()).getJavaIdentifier();
                            item = item.replace("minecraft:", "item.minecraft.");
                            item = LocaleUtils.getLocaleString(item, language);
                            builder.addComponent(new LabelComponent(item + ": " + entry.getValue()));
                        }
                    }
                    break;
                case 3:
                    builder.setTitle(LocaleUtils.getLocaleString("stat.itemsButton", language) + " - " + LocaleUtils.getLocaleString("stat_type.minecraft.crafted", language));

                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                        if (entry.getKey() instanceof CraftItemStatistic) {
                            String item = ItemRegistry.ITEM_ENTRIES.get(((CraftItemStatistic) entry.getKey()).getId()).getJavaIdentifier();
                            item = item.replace("minecraft:", "item.minecraft.");
                            item = LocaleUtils.getLocaleString(item, language);
                            builder.addComponent(new LabelComponent(item + ": " + entry.getValue()));
                        }
                    }
                    break;
                case 4:
                    builder.setTitle(LocaleUtils.getLocaleString("stat.itemsButton", language) + " - " + LocaleUtils.getLocaleString("stat_type.minecraft.used", language));

                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                        if (entry.getKey() instanceof UseItemStatistic) {
                            String item = ItemRegistry.ITEM_ENTRIES.get(((UseItemStatistic) entry.getKey()).getId()).getJavaIdentifier();
                            item = item.replace("minecraft:", "item.minecraft.");
                            item = LocaleUtils.getLocaleString(item, language);
                            builder.addComponent(new LabelComponent(item + ": " + entry.getValue()));
                        }
                    }
                    break;
                case 5:
                    builder.setTitle(LocaleUtils.getLocaleString("stat.itemsButton", language) + " - " + LocaleUtils.getLocaleString("stat_type.minecraft.picked_up", language));

                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                        if (entry.getKey() instanceof PickupItemStatistic) {
                            String item = ItemRegistry.ITEM_ENTRIES.get(((PickupItemStatistic) entry.getKey()).getId()).getJavaIdentifier();
                            item = item.replace("minecraft:", "item.minecraft.");
                            item = LocaleUtils.getLocaleString(item, language);
                            builder.addComponent(new LabelComponent(item + ": " + entry.getValue()));
                        }
                    }
                    break;
                case 6:
                    builder.setTitle(LocaleUtils.getLocaleString("stat.itemsButton", language) + " - " + LocaleUtils.getLocaleString("stat_type.minecraft.dropped", language));

                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                        if (entry.getKey() instanceof DropItemStatistic) {
                            String item = ItemRegistry.ITEM_ENTRIES.get(((DropItemStatistic) entry.getKey()).getId()).getJavaIdentifier();
                            item = item.replace("minecraft:", "item.minecraft.");
                            item = LocaleUtils.getLocaleString(item, language);
                            builder.addComponent(new LabelComponent(item + ": " + entry.getValue()));
                        }
                    }
                    break;
                case 7:
                    builder.setTitle(LocaleUtils.getLocaleString("stat.mobsButton", language) + " - " + LanguageUtils.getPlayerLocaleString("geyser.statistics.killed", language));

                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                        if (entry.getKey() instanceof KillEntityStatistic) {
                            String mob = LocaleUtils.getLocaleString("entity.minecraft." + MagicValues.key(EntityType.class, ((KillEntityStatistic) entry.getKey()).getId()).name().toLowerCase(), language);
                            builder.addComponent(new LabelComponent(mob + ": " + entry.getValue()));
                        }
                    }
                    break;
                case 8:
                    builder.setTitle(LocaleUtils.getLocaleString("stat.mobsButton", language) + " - " + LanguageUtils.getPlayerLocaleString("geyser.statistics.killed_by", language));

                    for (Map.Entry<Statistic, Integer> entry : session.getStatistics().entrySet()) {
                        if (entry.getKey() instanceof KilledByEntityStatistic) {
                            String mob = LocaleUtils.getLocaleString("entity.minecraft." + MagicValues.key(EntityType.class, ((KilledByEntityStatistic) entry.getKey()).getId()).name().toLowerCase(), language);
                            builder.addComponent(new LabelComponent(mob + ": " + entry.getValue()));
                        }
                    }
                    break;
                default:
                    return false;
            }

            CustomFormWindow window = builder.build();
            if (window.getContent().isEmpty()) {
                builder.addComponent(new LabelComponent(LanguageUtils.getPlayerLocaleString("geyser.statistics.none", language)));
                window = builder.build();
            }

            session.sendForm(window, STATISTICS_LIST_FORM_ID);
        }

        return true;
    }

    /**
     * Handle the list form response
     *
     * @param session The session that sent the response
     * @param response The response string to parse
     * @return True if the form was parsed correctly, false if not
     */
    public static boolean handleListForm(GeyserSession session, String response) {
        CustomFormWindow listForm = (CustomFormWindow) session.getWindowCache().getWindows().get(STATISTICS_LIST_FORM_ID);
        listForm.setResponse(response);

        if (!listForm.isClosed()) {
            session.sendForm(buildMenuForm(session), STATISTICS_MENU_FORM_ID);
        }

        return true;
    }
}
