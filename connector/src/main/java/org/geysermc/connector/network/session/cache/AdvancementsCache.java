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

package org.geysermc.connector.network.session.cache;

import com.github.steveice10.mc.protocol.data.game.advancement.Advancement;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientAdvancementTabPacket;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.common.window.SimpleFormWindow;
import org.geysermc.common.window.button.FormButton;
import org.geysermc.common.window.response.SimpleFormResponse;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.chat.MessageTranslator;
import org.geysermc.connector.utils.GeyserAdvancement;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.connector.utils.LocaleUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancementsCache {

    // Different form IDs
    public static final int ADVANCEMENTS_MENU_FORM_ID = 1341;
    public static final int ADVANCEMENTS_LIST_FORM_ID = 1342;
    public static final int ADVANCEMENT_INFO_FORM_ID = 1343;

    /**
     * Stores the player's advancement progress
     */
    @Getter
    private final Map<String, Map<String, Long>> storedAdvancementProgress = new HashMap<>();

    /**
     * Stores advancements for the player.
     */
    @Getter
    private final Map<String, GeyserAdvancement> storedAdvancements = new HashMap<>();

    /**
     * Stores player's chosen advancement's ID and title for use in form creators.
     */
    @Setter
    private String currentAdvancementCategoryId = null;

    private final GeyserSession session;

    public AdvancementsCache(GeyserSession session) {
        this.session = session;
    }

    /**
     * Build a form with all advancement categories
     *
     * @return The built advancement category menu
     */
    public SimpleFormWindow buildMenuForm() {
        // Cache the language for cleaner access
        String language = session.getClientData().getLanguageCode();

        // Created menu window for advancement categories
        SimpleFormWindow window = new SimpleFormWindow(LocaleUtils.getLocaleString("gui.advancements", language), "");
        for (Map.Entry<String, GeyserAdvancement> advancement : storedAdvancements.entrySet()) {
            if (advancement.getValue().getParentId() == null) { // No parent means this is a root advancement
                window.getButtons().add(new FormButton(MessageTranslator.convertMessage(advancement.getValue().getDisplayData().getTitle(), language)));
            }
        }

        if (window.getButtons().isEmpty()) {
            window.setContent(LocaleUtils.getLocaleString("advancements.empty", language));
        }

        return window;
    }

    /**
     * Builds the list of advancements
     *
     * @return The built list form
     */
    public SimpleFormWindow buildListForm() {
        // Cache the language for easier access
        String language = session.getLocale();
        String id = currentAdvancementCategoryId;
        GeyserAdvancement categoryAdvancement = storedAdvancements.get(currentAdvancementCategoryId);

        // Create the window
        SimpleFormWindow window = new SimpleFormWindow(MessageTranslator.convertMessage(categoryAdvancement.getDisplayData().getTitle(), language),
                MessageTranslator.convertMessage(categoryAdvancement.getDisplayData().getDescription(), language));

        if (id != null) {
            for (Map.Entry<String, GeyserAdvancement> advancementEntry : storedAdvancements.entrySet()) {
                GeyserAdvancement advancement = advancementEntry.getValue();
                if (advancement != null) {
                    if (advancement.getParentId() != null && currentAdvancementCategoryId.equals(advancement.getRootId(this))) {
                        boolean earned = isEarned(advancement);

                        if (earned || !advancement.getDisplayData().isShowToast()) {
                            window.getButtons().add(new FormButton("§6" + MessageTranslator.convertMessage(advancementEntry.getValue().getDisplayData().getTitle()) + "\n"));
                        } else {
                            window.getButtons().add(new FormButton(MessageTranslator.convertMessage(advancementEntry.getValue().getDisplayData().getTitle()) + "\n"));
                        }
                    }
                }
            }
        }

        window.getButtons().add(new FormButton(LanguageUtils.getPlayerLocaleString("gui.back", language)));

        return window;
    }

    /**
     * Builds the advancement display info based on the chosen category
     *
     * @param advancement The advancement used to create the info display
     * @return The information for the chosen advancement
     */
    public SimpleFormWindow buildInfoForm(GeyserAdvancement advancement) {
        // Cache language for easier access
        String language = session.getLocale();

        String earned = isEarned(advancement) ? "yes" : "no";

        String description = getColorFromAdvancementFrameType(advancement) + MessageTranslator.convertMessage(advancement.getDisplayData().getDescription(), language);
        String earnedString = LanguageUtils.getPlayerLocaleString("geyser.advancements.earned", language, LocaleUtils.getLocaleString("gui." + earned, language));

        /*
        Layout will look like:

        (Form title) Stone Age

        (Description) Mine stone with your new pickaxe

        Earned: Yes
        Parent Advancement: Minecraft // If relevant
         */

        String content = description + "\n\n§f" +
                earnedString + "\n";
        if (!currentAdvancementCategoryId.equals(advancement.getParentId())) {
            // Only display the parent if it is not the category
            content += LanguageUtils.getPlayerLocaleString("geyser.advancements.parentid", language, MessageTranslator.convertMessage(storedAdvancements.get(advancement.getParentId()).getDisplayData().getTitle(), language));
        }
        SimpleFormWindow window = new SimpleFormWindow(MessageTranslator.convertMessage(advancement.getDisplayData().getTitle()), content);
        window.getButtons().add(new FormButton(LanguageUtils.getPlayerLocaleString("gui.back", language)));

        return window;
    }

    /**
     * Determine if this advancement has been earned.
     *
     * @param advancement the advancement to determine
     * @return true if the advancement has been earned.
     */
    public boolean isEarned(GeyserAdvancement advancement) {
        boolean earned = false;
        if (advancement.getRequirements().size() == 0) {
            // Minecraft handles this case, so we better as well
            return false;
        }
        Map<String, Long> progress = storedAdvancementProgress.get(advancement.getId());
        if (progress != null) {
            // Each advancement's requirement must be fulfilled
            // For example, [[zombie, blaze, skeleton]] means that one of those three categories must be achieved
            // But [[zombie], [blaze], [skeleton]] means that all three requirements must be completed
            for (List<String> requirements : advancement.getRequirements()) {
                boolean requirementsDone = false;
                for (String requirement : requirements) {
                    Long obtained = progress.get(requirement);
                    // -1 means that this particular component required for completing the advancement
                    // has yet to be fulfilled
                    if (obtained != null && !obtained.equals(-1L)) {
                        requirementsDone = true;
                        break;
                    }
                }
                if (!requirementsDone) {
                    return false;
                }
            }
            earned = true;
        }
        return earned;
    }

    /**
     * Handle the menu form response
     *
     * @param response The response string to parse
     * @return True if the form was parsed correctly, false if not
     */
    public boolean handleMenuForm(String response) {
        SimpleFormWindow menuForm = (SimpleFormWindow) session.getWindowCache().getWindows().get(ADVANCEMENTS_MENU_FORM_ID);
        menuForm.setResponse(response);

        SimpleFormResponse formResponse = (SimpleFormResponse) menuForm.getResponse();

        String id = "";
        if (formResponse != null && formResponse.getClickedButton() != null) {
            int advancementIndex = 0;
            for (Map.Entry<String, GeyserAdvancement> advancement : storedAdvancements.entrySet()) {
                if (advancement.getValue().getParentId() == null) { // Root advancement
                    if (advancementIndex == formResponse.getClickedButtonId()) {
                        id = advancement.getKey();
                        break;
                    } else {
                        advancementIndex++;
                    }
                }
            }
        }
        if (!id.equals("")) {
            if (id.equals(currentAdvancementCategoryId)) {
                // The server thinks we are already on this tab
                session.sendForm(buildListForm(), ADVANCEMENTS_LIST_FORM_ID);
            } else {
                // Send a packet indicating that we intend to open this particular advancement window
                ClientAdvancementTabPacket packet = new ClientAdvancementTabPacket(id);
                session.sendDownstreamPacket(packet);
                // Wait for a response there
            }
        }

        return true;
    }

    /**
     * Handle the list form response (Advancement category choice)
     *
     * @param response The response string to parse
     * @return True if the form was parsed correctly, false if not
     */
    public boolean handleListForm(String response) {
        SimpleFormWindow listForm = (SimpleFormWindow) session.getWindowCache().getWindows().get(ADVANCEMENTS_LIST_FORM_ID);
        listForm.setResponse(response);

        SimpleFormResponse formResponse = (SimpleFormResponse) listForm.getResponse();

        if (!listForm.isClosed() && formResponse != null && formResponse.getClickedButton() != null) {
            GeyserAdvancement advancement = null;
            int advancementIndex = 0;
            // Loop around to find the advancement that the client pressed
            for (GeyserAdvancement advancementEntry : storedAdvancements.values()) {
                if (advancementEntry.getParentId() != null &&
                        currentAdvancementCategoryId.equals(advancementEntry.getRootId(this))) {
                    if (advancementIndex == formResponse.getClickedButtonId()) {
                        advancement = advancementEntry;
                        break;
                    } else {
                        advancementIndex++;
                    }
                }
            }
            if (advancement != null) {
                session.sendForm(buildInfoForm(advancement), ADVANCEMENT_INFO_FORM_ID);
            } else {
                session.sendForm(buildMenuForm(), ADVANCEMENTS_MENU_FORM_ID);
                // Indicate that we have closed the current advancement tab
                session.sendDownstreamPacket(new ClientAdvancementTabPacket());
            }
        } else {
            // Indicate that we have closed the current advancement tab
            session.sendDownstreamPacket(new ClientAdvancementTabPacket());
        }

        return true;
    }

    /**
     * Handle the info form response
     *
     * @param response The response string to parse
     * @return True if the form was parsed correctly, false if not
     */
    public boolean handleInfoForm(String response) {
        SimpleFormWindow listForm = (SimpleFormWindow) session.getWindowCache().getWindows().get(ADVANCEMENT_INFO_FORM_ID);
        listForm.setResponse(response);

        SimpleFormResponse formResponse = (SimpleFormResponse) listForm.getResponse();

        if (!listForm.isClosed() && formResponse != null && formResponse.getClickedButton() != null) {
            session.sendForm(buildListForm(), ADVANCEMENTS_LIST_FORM_ID);
        }

        return true;
    }

    public String getColorFromAdvancementFrameType(GeyserAdvancement advancement) {
        String base = "\u00a7";
        if (advancement.getDisplayData().getFrameType() == Advancement.DisplayData.FrameType.CHALLENGE) {
            return base + "5";
        }
        return base + "a"; // Used for types TASK and GOAL
    }
}
