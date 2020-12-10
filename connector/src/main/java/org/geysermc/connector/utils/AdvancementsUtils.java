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
import net.kyori.adventure.text.Component;
import org.geysermc.common.window.SimpleFormWindow;
import org.geysermc.common.window.button.FormButton;
import org.geysermc.common.window.response.SimpleFormResponse;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.chat.MessageTranslator;

import java.util.HashMap;
import java.util.Map;

public class AdvancementsUtils {

    // Used in UpstreamPacketHandler.java
    public static final int ADVANCEMENTS_MENU_FORM_ID = 1341;
    public static final int ADVANCEMENTS_LIST_FORM_ID = 1342;
    public static final int ADVANCEMENT_INFO_FORM_ID = 1343;
    private static final Map<String, String> ADVANCEMENT_FRAME_TYPES_TO_COLOR_CODES = new HashMap<>();
    static {
        ADVANCEMENT_FRAME_TYPES_TO_COLOR_CODES.put("TASK", "§a");
        ADVANCEMENT_FRAME_TYPES_TO_COLOR_CODES.put("GOAL", "§a");
        ADVANCEMENT_FRAME_TYPES_TO_COLOR_CODES.put("CHALLENGE", "§5");
    }



    /**
     * Build a form for the given session with all advancement categories
     *
     * @param session The session to build the form for
     */
    public static SimpleFormWindow buildMenuForm(GeyserSession session) throws NullPointerException {
        // Cache the language for cleaner access
        String language = session.getClientData().getLanguageCode();
        // Created menu window for advancement categories
        SimpleFormWindow window = new SimpleFormWindow(LanguageUtils.getPlayerLocaleString("gui.advancements", language), "");
        int baseId = 0;
        session.getButtonIdsToIdButtonAdvancementCategories().clear();
        for (Map.Entry<String, Advancement> advancement : session.getStoredAdvancements().entrySet()) {
            String id = advancement.getValue().getId();
            Component title = advancement.getValue().getDisplayData().getTitle();
            if (advancement.getValue().getId().endsWith("root")) {
                window.getButtons().add(new FormButton(MessageTranslator.convertMessage(advancement.getValue().getDisplayData().getTitle(), language) + " - " + MessageTranslator.convertMessage(advancement.getValue().getDisplayData().getDescription(), language)));
                session.getButtonIdsToIdButtonAdvancementCategories().put(baseId++, id);
                session.getButtonIdsToTitleButtonAdvancementCategories().put(baseId++, title);
            }

        }
        if (window.getButtons().isEmpty()) {
            window.setContent(LanguageUtils.getPlayerLocaleString("advancements.empty", language));
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

        SimpleFormWindow menuForm = (SimpleFormWindow) session.getWindowCache().getWindows().get(ADVANCEMENTS_MENU_FORM_ID);
        menuForm.setResponse(response);
        SimpleFormResponse formResponse = (SimpleFormResponse) menuForm.getResponse();

        if (formResponse != null && formResponse.getClickedButton() != null) {
            session.setStoredAdvancementCategoryId(session.getButtonIdsToIdButtonAdvancementCategories().get(formResponse.getClickedButtonId()));
            session.setStoredAdvancementCategoryTitle(session.getButtonIdsToTitleButtonAdvancementCategories().get(formResponse.getClickedButtonId()));
        }



        try {
            session.sendForm(buildListForm(session), ADVANCEMENTS_LIST_FORM_ID);
        } catch (NullPointerException ignored) {}
        return true;
    }
    public static SimpleFormWindow buildListForm(GeyserSession session) {

            String language = session.getClientData().getLanguageCode();
            String id = session.getStoredAdvancementCategoryId();
            Component title = session.getStoredAdvancementCategoryTitle();
            int x = 0;
            SimpleFormWindow window = new SimpleFormWindow(MessageTranslator.convertMessage(title, language), "");
            session.getButtonIdsToAdvancement().clear();
            for (Map.Entry<String, Advancement> advancementEntry : session.getStoredAdvancements().entrySet()) {
                if (id != null) {
                    if (advancementEntry.getValue() != null) {
                        if (advancementEntry.getValue().getId().startsWith(id.replace("/root", "").replace("root", ""))) {
                            boolean earned = true;
                            if (session.getStoredAdvancementProgress().get(advancementEntry.getValue().getId()) != null || session.getStoredAdvancementProgress() != null || !session.getStoredAdvancementProgress().get(advancementEntry.getValue().getId()).entrySet().isEmpty()) {
                                for (Map.Entry<String, Long> entry : session.getStoredAdvancementProgress().get(advancementEntry.getValue().getId()).entrySet()) {
                                    if (entry.getValue() == -1) {
                                        earned = false;
                                        break;
                                    }
                                }
                            }
                            if (earned || !advancementEntry.getValue().getDisplayData().isShowToast()) {
                                window.getButtons().add(new FormButton("§6" + MessageTranslator.convertMessage(advancementEntry.getValue().getDisplayData().getTitle()) + "\n"));
                            } else {
                                window.getButtons().add(new FormButton(MessageTranslator.convertMessage(advancementEntry.getValue().getDisplayData().getTitle()) + "\n"));
                            }
                            session.getButtonIdsToAdvancement().put(x++, advancementEntry.getValue());
                        }
                    }
                }
            }



            window.getButtons().add(new FormButton(LanguageUtils.getPlayerLocaleString("gui.back", language)));


            return window;
    }
    /**
     * Handle the list form response (back button)
     *
     * @param session  The session that sent the response
     * @param response The response string to parse
     * @return True if the form was parsed correctly, false if not
     */
    public static boolean handleListForm(GeyserSession session, String response) {

        SimpleFormWindow listForm = (SimpleFormWindow) session.getWindowCache().getWindows().get(ADVANCEMENTS_LIST_FORM_ID);
        listForm.setResponse(response);
        SimpleFormResponse formResponse = (SimpleFormResponse) listForm.getResponse();
        if (!listForm.isClosed() && formResponse != null && formResponse.getClickedButton() != null) {

            if (session.getButtonIdsToAdvancement().get(formResponse.getClickedButtonId()) != null) {
                if (!session.getButtonIdsToAdvancement().get(formResponse.getClickedButtonId()).getId().endsWith("root")) {
                    session.sendForm(buildInfoForm(session, session.getButtonIdsToAdvancement().get(formResponse.getClickedButtonId())), ADVANCEMENT_INFO_FORM_ID);
                } else {
                    session.sendForm(buildListForm(session), ADVANCEMENT_INFO_FORM_ID);
                }
            } else {
                session.sendForm(buildMenuForm(session), ADVANCEMENTS_MENU_FORM_ID);
            }

        }

        return true;
    }

    public static SimpleFormWindow buildInfoForm(GeyserSession session, Advancement advancement) {

        String language = session.getLocale();
        StringBuilder content = new StringBuilder();
        String earned = "confirm";
        if (session.getStoredAdvancementProgress().get(advancement.getId()) != null || session.getStoredAdvancementProgress() != null || !session.getStoredAdvancementProgress().get(advancement.getId()).entrySet().isEmpty()) {
            for (Map.Entry<String, Long> entry : session.getStoredAdvancementProgress().get(advancement.getId()).entrySet()) {
                if (entry.getValue() == -1) {
                    earned = "deny";
                    break;
                }
            }
        }


        content.append(MessageTranslator.convertMessage(advancement.getDisplayData().getTitle(), language) + "\n");
        content.append(MessageTranslator.convertMessage(advancement.getDisplayData().getDescription(), language) + "\n\n");
        content.append(ADVANCEMENT_FRAME_TYPES_TO_COLOR_CODES.get(advancement.getDisplayData().getFrameType().toString()) + "["  + LanguageUtils.getPlayerLocaleString("geyser.advancements." + advancement.getDisplayData().getFrameType().toString().toLowerCase(), language) + "]" + "\n\n" + "§f");
        content.append(LanguageUtils.getPlayerLocaleString("geyser.advancements.earned", language) + ": " + LanguageUtils.getPlayerLocaleString("geyser.gui.exit." + earned, language) + "\n");
        content.append(LanguageUtils.getPlayerLocaleString("geyser.advancements.parentid", language) + ": " + MessageTranslator.convertMessage(session.getStoredAdvancements().get(advancement.getParentId()).getDisplayData().getTitle(), language) + "\n");
        SimpleFormWindow window = new SimpleFormWindow(MessageTranslator.convertMessage(advancement.getDisplayData().getTitle()), content.toString());
        window.getButtons().add(new FormButton(LanguageUtils.getPlayerLocaleString("gui.back", language)));



        return window;
    }

    public static boolean handleInfoForm(GeyserSession session, String response) {
        SimpleFormWindow listForm = (SimpleFormWindow) session.getWindowCache().getWindows().get(ADVANCEMENT_INFO_FORM_ID);
        listForm.setResponse(response);
        SimpleFormResponse formResponse = (SimpleFormResponse) listForm.getResponse();
        if (!listForm.isClosed() && formResponse != null && formResponse.getClickedButton() != null) {
            session.sendForm(buildListForm(session), ADVANCEMENTS_LIST_FORM_ID);

        }
        return true;
    }
}
