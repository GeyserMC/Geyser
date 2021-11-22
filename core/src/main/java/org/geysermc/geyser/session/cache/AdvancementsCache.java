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

package org.geysermc.geyser.session.cache;

import com.github.steveice10.mc.protocol.data.game.advancement.Advancement;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSeenAdvancementsPacket;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.level.GeyserAdvancement;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.cumulus.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancementsCache {
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
     * Build and send a form with all advancement categories
     */
    public void buildAndShowMenuForm() {
        SimpleForm.Builder builder =
                SimpleForm.builder()
                        .translator(MinecraftLocale::getLocaleString, session.getLocale())
                        .title("gui.advancements");

        boolean hasAdvancements = false;
        for (Map.Entry<String, GeyserAdvancement> advancement : storedAdvancements.entrySet()) {
            if (advancement.getValue().getParentId() == null) { // No parent means this is a root advancement
                hasAdvancements = true;
                builder.button(MessageTranslator.convertMessage(advancement.getValue().getDisplayData().getTitle(), session.getLocale()));
            }
        }

        if (!hasAdvancements) {
            builder.content("advancements.empty");
        }

        builder.responseHandler((form, responseData) -> {
            SimpleFormResponse response = form.parseResponse(responseData);
            if (!response.isCorrect()) {
                return;
            }

            String id = "";

            int advancementIndex = 0;
            for (Map.Entry<String, GeyserAdvancement> advancement : storedAdvancements.entrySet()) {
                if (advancement.getValue().getParentId() == null) { // Root advancement
                    if (advancementIndex == response.getClickedButtonId()) {
                        id = advancement.getKey();
                        break;
                    } else {
                        advancementIndex++;
                    }
                }
            }

            if (!id.equals("")) {
                if (id.equals(currentAdvancementCategoryId)) {
                    // The server thinks we are already on this tab
                    buildAndShowListForm();
                } else {
                    // Send a packet indicating that we intend to open this particular advancement window
                    ServerboundSeenAdvancementsPacket packet = new ServerboundSeenAdvancementsPacket(id);
                    session.sendDownstreamPacket(packet);
                    // Wait for a response there
                }
            }
        });

        session.sendForm(builder);
    }

    /**
     * Build and send the list of advancements
     */
    public void buildAndShowListForm() {
        GeyserAdvancement categoryAdvancement = storedAdvancements.get(currentAdvancementCategoryId);
        String language = session.getLocale();

        SimpleForm.Builder builder =
                SimpleForm.builder()
                        .title(MessageTranslator.convertMessage(categoryAdvancement.getDisplayData().getTitle(), language))
                        .content(MessageTranslator.convertMessage(categoryAdvancement.getDisplayData().getDescription(), language));

        if (currentAdvancementCategoryId != null) {
            for (GeyserAdvancement advancement : storedAdvancements.values()) {
                if (advancement != null) {
                    if (advancement.getParentId() != null && currentAdvancementCategoryId.equals(advancement.getRootId(this))) {
                        boolean color = isEarned(advancement) || !advancement.getDisplayData().isShowToast();
                        builder.button((color ? "§6" : "") + MessageTranslator.convertMessage(advancement.getDisplayData().getTitle()) + '\n');
                    }
                }
            }
        }

        builder.button(GeyserLocale.getPlayerLocaleString("gui.back", language));

        builder.responseHandler((form, responseData) -> {
            SimpleFormResponse response = form.parseResponse(responseData);
            if (!response.isCorrect()) {
                // Indicate that we have closed the current advancement tab
                session.sendDownstreamPacket(new ServerboundSeenAdvancementsPacket());
                return;
            }

            GeyserAdvancement advancement = null;
            int advancementIndex = 0;
            // Loop around to find the advancement that the client pressed
            for (GeyserAdvancement advancementEntry : storedAdvancements.values()) {
                if (advancementEntry.getParentId() != null &&
                        currentAdvancementCategoryId.equals(advancementEntry.getRootId(this))) {
                    if (advancementIndex == response.getClickedButtonId()) {
                        advancement = advancementEntry;
                        break;
                    } else {
                        advancementIndex++;
                    }
                }
            }

            if (advancement != null) {
                buildAndShowInfoForm(advancement);
            } else {
                buildAndShowMenuForm();
                // Indicate that we have closed the current advancement tab
                session.sendDownstreamPacket(new ServerboundSeenAdvancementsPacket());
            }
        });

        session.sendForm(builder);
    }

    /**
     * Builds the advancement display info based on the chosen category
     *
     * @param advancement The advancement used to create the info display
     */
    public void buildAndShowInfoForm(GeyserAdvancement advancement) {
        // Cache language for easier access
        String language = session.getLocale();

        String earned = isEarned(advancement) ? "yes" : "no";

        String description = getColorFromAdvancementFrameType(advancement) + MessageTranslator.convertMessage(advancement.getDisplayData().getDescription(), language);
        String earnedString = GeyserLocale.getPlayerLocaleString("geyser.advancements.earned", language, MinecraftLocale.getLocaleString("gui." + earned, language));

        /*
        Layout will look like:

        (Form title) Stone Age

        (Description) Mine stone with your new pickaxe

        Earned: Yes
        Parent Advancement: Minecraft // If relevant
         */

        String content = description + "\n\n§f" + earnedString + "\n";
        if (!currentAdvancementCategoryId.equals(advancement.getParentId())) {
            // Only display the parent if it is not the category
            content += GeyserLocale.getPlayerLocaleString("geyser.advancements.parentid", language, MessageTranslator.convertMessage(storedAdvancements.get(advancement.getParentId()).getDisplayData().getTitle(), language));
        }

        session.sendForm(
                SimpleForm.builder()
                        .title(MessageTranslator.convertMessage(advancement.getDisplayData().getTitle()))
                        .content(content)
                        .button(GeyserLocale.getPlayerLocaleString("gui.back", language))
                        .responseHandler((form, responseData) -> {
                            SimpleFormResponse response = form.parseResponse(responseData);
                            if (response.isCorrect()) {
                                buildAndShowListForm();
                            }
                        })
        );
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

    public String getColorFromAdvancementFrameType(GeyserAdvancement advancement) {
        String base = "\u00a7";
        if (advancement.getDisplayData().getFrameType() == Advancement.DisplayData.FrameType.CHALLENGE) {
            return base + "5";
        }
        return base + "a"; // Used for types TASK and GOAL
    }
}
