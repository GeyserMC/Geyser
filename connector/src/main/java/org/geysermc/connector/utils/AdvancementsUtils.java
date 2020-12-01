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

import com.github.steveice10.mc.protocol.data.game.advancement.Advancement;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.units.qual.A;
import org.geysermc.common.window.SimpleFormWindow;
import org.geysermc.common.window.button.FormButton;
import org.geysermc.common.window.response.SimpleFormResponse;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.chat.MessageTranslator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancementsUtils {

    // Used in UpstreamPacketHandler.java
    public static final int ADVANCEMENTS_MENU_FORM_ID = 1341;
    public static final int ADVANCEMENTS_LIST_FORM_ID = 1342;
    Map<Integer, String[]> buttonIdsToIdAndTitle = new HashMap<>();

    private AdvancementsUtils(GeyserSession session) {

    }

    /**
     * Build a form for the given session with all advancement categories
     *
     * @param session The session to build the form for
     */
    public SimpleFormWindow buildMenuForm(GeyserSession session) throws NullPointerException {
        // Cache the language for cleaner access
        String language = session.getClientData().getLanguageCode();
        // Created menu window for advancement categories
        SimpleFormWindow window = new SimpleFormWindow(LocaleUtils.getLocaleString("gui.advancements", language), "");
        int baseId = 0;

        for (Map.Entry<String, Advancement> advancement : session.getStoredAdvancements().entrySet()) {
            String title = advancement.getValue().getDisplayData().getTitle().toString();
            String description = advancement.getValue().getDisplayData().getDescription().toString();
            String[] idAndTitle = {advancement.getValue().getId(), advancement.getValue().getDisplayData().getTitle().toString()};

            if (advancement.getValue().getId().endsWith("root")) {
                window.getButtons().add(new FormButton(MessageTranslator.convertMessage(title, language) + " - " + MessageTranslator.convertMessage(description, language)));
                buttonIdsToIdAndTitle.put(baseId++, idAndTitle);
                System.out.println(baseId + " - "+idAndTitle[0] + " - " +  idAndTitle[1]);
            }


        }

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
        // Cache the language for cleaner access
        String language = session.getClientData().getLanguageCode();
        SimpleFormWindow menuForm = (SimpleFormWindow) session.getWindowCache().getWindows().get(ADVANCEMENTS_MENU_FORM_ID);
        menuForm.setResponse(response);
        SimpleFormResponse formResponse = (SimpleFormResponse) menuForm.getResponse();
        int clickedButton = 0;

        if (formResponse != null && formResponse.getClickedButton() != null) {
            clickedButton = formResponse.getClickedButtonId();

        }
        StringBuilder content = new StringBuilder();
        String[] id = advancementsUtils.buttonIdsToIdAndTitle.get(clickedButton);
        System.out.println(advancementsUtils.buttonIdsToIdAndTitle.get(clickedButton));
        for (String string : id) {
            System.out.println(string);
            content.append(LanguageUtils.getPlayerLocaleString("geyser.advancements.earned", language) + ":\n");
        }

        // Showed advancements you have earned based on category selected
        SimpleFormWindow window = new SimpleFormWindow(id[1], content.toString());
        for (Map.Entry<String, Advancement> advancement : session.getStoredAdvancements().entrySet()) {
            if (advancement.getValue().getId().startsWith(id[0])) {
                content.append(" - " + MessageTranslator.convertMessage(advancement.getValue().getDisplayData().getTitle().toString()) + "\n     " + MessageTranslator.convertMessage(advancement.getValue().getDisplayData().getDescription().toString()) + "\n");
            }
        }
        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("gui.back", language)));




        session.sendForm(window);
        return true;
    }

    /**
     * Handle the list form response (back button)
     *
     * @param session  The session that sent the response
     * @param response The response string to parse
     * @return True if the form was parsed correctly, false if not
     */
    public static boolean handleListForm(GeyserSession session, String response) {
        AdvancementsUtils advancementsUtils = new AdvancementsUtils();
        SimpleFormWindow listForm = (SimpleFormWindow) session.getWindowCache().getWindows().get(ADVANCEMENTS_LIST_FORM_ID);
        listForm.setResponse(response);
        SimpleFormResponse formResponse = (SimpleFormResponse) listForm.getResponse();
        if (!listForm.isClosed()) {
            session.sendForm(advancementsUtils.buildMenuForm(session), ADVANCEMENTS_MENU_FORM_ID);
        }
        if (formResponse != null && formResponse.getClickedButton() != null) {
            SimpleFormWindow window = advancementsUtils.buildMenuForm(session);
            session.sendForm(window, ADVANCEMENTS_MENU_FORM_ID);
        }
        return true;
    }




}
//test