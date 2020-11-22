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
import com.github.steveice10.mc.protocol.data.game.advancement.Advancement;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.data.game.statistic.*;
import org.geysermc.common.window.SimpleFormWindow;
import org.geysermc.common.window.button.FormButton;
import org.geysermc.common.window.response.SimpleFormResponse;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.chat.MessageTranslator;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.java.JavaAdvancementsTranslator;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancementsUtils {

    // Used in UpstreamPacketHandler.java
    public static final int ADVANCEMENTS_MENU_FORM_ID = 1341;
    public static final int ADVANCEMENTS_LIST_FORM_ID = 1342;

    /**
     * Build a form for the given session with all advancement categories
     *
     * @param session The session to build the form for
     */
    public static SimpleFormWindow buildMenuForm(GeyserSession session) {
        // Cache the language for cleaner access
        String language = session.getClientData().getLanguageCode();
        // Created menu window for advancement categories
        SimpleFormWindow window = new SimpleFormWindow(LocaleUtils.getLocaleString("gui.advancements", language), "");

        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("advancements.story.root.title", language) + " - " + LocaleUtils.getLocaleString("advancements.story.root.description", language)));
        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("advancements.nether.root.title", language) + " - " + LocaleUtils.getLocaleString("advancements.nether.root.description", language)));
        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("advancements.end.root.title", language) + " - " + LocaleUtils.getLocaleString("advancements.end.root.description", language)));
        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("advancements.adventure.root.title", language) + " - " + LocaleUtils.getLocaleString("advancements.adventure.root.description", language)));
        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("advancements.husbandry.root.title", language) + " - " + LocaleUtils.getLocaleString("advancements.husbandry.root.description", language)));

        return window;
    }

    /**
     * Handle the menu form response
     *
     * @param session  The session that sent the response
     * @param response The response string to parse
     * @return True if the form was parsed correctly, false if not
     */
    public static boolean handleMenuForm(GeyserSession session, String response) {
        SimpleFormWindow menuForm = (SimpleFormWindow) session.getWindowCache().getWindows().get(ADVANCEMENTS_MENU_FORM_ID);
        menuForm.setResponse(response);
        SimpleFormResponse formResponse = (SimpleFormResponse) menuForm.getResponse();

        // Cache the language for cleaner access
        String language = session.getClientData().getLanguageCode();
        StringBuilder content = new StringBuilder();
        String title = null;
        if (formResponse != null && formResponse.getClickedButton() != null) {
            content.append(LanguageUtils.getPlayerLocaleString("geyser.advancements.earned", language) + ":\n");
            switch (formResponse.getClickedButtonId()) {
                case 0:
                    title = LocaleUtils.getLocaleString("advancements.story.root.title", language);

                        for (Map.Entry<String, Advancement> value : session.getStoredAdvancements().entrySet()) {
                            if (value.getKey().contains("story")){
                                content.append(" - " + MessageTranslator.convertMessage(value.getValue().getDisplayData().getTitle().toString(), language) + "\n");
                                content.append("     " + MessageTranslator.convertMessage(value.getValue().getDisplayData().getDescription().toString(), language)+ "\n\n" );
                            }
                        }

                    break;
                case 1:
                    title = LocaleUtils.getLocaleString("advancements.nether.root.title", language);

                        for (Map.Entry<String, Advancement> value : session.getStoredAdvancements().entrySet()) {
                            if (value.getKey().contains("nether")){
                                 content.append(" - " + MessageTranslator.convertMessage(value.getValue().getDisplayData().getTitle().toString(), language) + "\n");
                                 content.append("     " + MessageTranslator.convertMessage(value.getValue().getDisplayData().getDescription().toString(), language)+ "\n\n" );
                            }
                    }
                    break;
                case 2:
                    title = LocaleUtils.getLocaleString("advancements.end.root.title", language);

                        for (Map.Entry<String, Advancement> value : session.getStoredAdvancements().entrySet()) {
                            if (value.getKey().contains("end")){
                                content.append(" - " + MessageTranslator.convertMessage(value.getValue().getDisplayData().getTitle().toString(), language) + "\n");
                                content.append("     " + MessageTranslator.convertMessage(value.getValue().getDisplayData().getDescription().toString(), language)+ "\n\n" );}
                        }

                    break;
                case 3:
                    title = LocaleUtils.getLocaleString("advancements.adventure.root.title", language);

                        for (Map.Entry<String, Advancement> value : session.getStoredAdvancements().entrySet()) {
                            if (value.getKey().contains("adventure")){
                                content.append(" - " + MessageTranslator.convertMessage(value.getValue().getDisplayData().getTitle().toString(), language) + "\n");
                                content.append("     " + MessageTranslator.convertMessage(value.getValue().getDisplayData().getDescription().toString(), language)+ "\n\n" );}
                        }

                    break;
                case 4:
                    title = LocaleUtils.getLocaleString("advancements.husbandry.root.title", language);

                        for (Map.Entry<String, Advancement> value : session.getStoredAdvancements().entrySet()) {
                            if (value.getKey().contains("husbandry")){
                                content.append(" - " + MessageTranslator.convertMessage(value.getValue().getDisplayData().getTitle().toString(), language) + "\n");
                                content.append("     " + MessageTranslator.convertMessage(value.getValue().getDisplayData().getDescription().toString(), language)+ "\n\n" );}
                        }

                    break;

        }


            // Showed advancements you have earned based on category selected
            SimpleFormWindow window = new SimpleFormWindow(title, content.toString());
            window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("gui.back", language)));
            session.sendForm(window, ADVANCEMENTS_LIST_FORM_ID);
        }

        return true;
    } // Handles "back" button
    public static boolean handleListForm(GeyserSession session, String response) {
        SimpleFormWindow listForm = (SimpleFormWindow) session.getWindowCache().getWindows().get(ADVANCEMENTS_LIST_FORM_ID);
       listForm.setResponse(response);

        if (!listForm.isClosed()) {
            session.sendForm(buildMenuForm(session), ADVANCEMENTS_MENU_FORM_ID);
        }

        return true;
    }
}