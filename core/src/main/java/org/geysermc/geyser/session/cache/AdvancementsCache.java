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

package org.geysermc.geyser.session.cache;

import org.geysermc.mcprotocollib.protocol.data.game.advancement.Advancement;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSeenAdvancementsPacket;
import lombok.Getter;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.geyser.level.GeyserAdvancement;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private String currentAdvancementCategoryId = null;

    /**
     * Stores if the player is currently viewing advancements.
     */
    private boolean formOpen = false;

    private final GeyserSession session;

    public AdvancementsCache(GeyserSession session) {
        this.session = session;
    }

    public void setCurrentAdvancementCategoryId(String categoryId) {
        if (!Objects.equals(currentAdvancementCategoryId, categoryId)) {
            // Only open and show list form if we're going to a different category
            currentAdvancementCategoryId = categoryId;
            if (formOpen) {
                session.closeForm();
                buildAndShowForm();
                formOpen = true;
            }
        }
    }

    public void buildAndShowForm() {
        if (currentAdvancementCategoryId == null) {
            buildAndShowMenuForm();
        } else {
            buildAndShowListForm();
        }
    }

    /**
     * Build and send a form with all advancement categories
     */
    public void buildAndShowMenuForm() {
        SimpleForm.Builder builder =
                SimpleForm.builder()
                        .translator(MinecraftLocale::getLocaleString, session.locale())
                        .title("gui.advancements");

        List<String> rootAdvancementIds = new ArrayList<>();
        for (Map.Entry<String, GeyserAdvancement> advancement : storedAdvancements.entrySet()) {
            if (advancement.getValue().getParentId() == null) { // No parent means this is a root advancement
                builder.button(MessageTranslator.convertMessage(advancement.getValue().getDisplayData().getTitle(), session.locale()));
                rootAdvancementIds.add(advancement.getKey());
            }
        }

        if (rootAdvancementIds.isEmpty()) {
            builder.content("advancements.empty");
        }

        builder.closedResultHandler(() -> {
            formOpen = false;
        }).validResultHandler((response) -> {
            String id = rootAdvancementIds.get(response.clickedButtonId());
            if (!id.isEmpty()) {
                // Send a packet indicating that we are opening this particular advancement window
                ServerboundSeenAdvancementsPacket packet = new ServerboundSeenAdvancementsPacket(id);
                session.sendDownstreamGamePacket(packet);
                currentAdvancementCategoryId = id;
                buildAndShowListForm();
            }
        });

        formOpen = true;
        session.sendForm(builder);
    }

    /**
     * Build and send the list of advancements
     */
    public void buildAndShowListForm() {
        GeyserAdvancement categoryAdvancement = storedAdvancements.get(currentAdvancementCategoryId);
        String language = session.locale();

        SimpleForm.Builder builder =
                SimpleForm.builder()
                        .title(MessageTranslator.convertMessage(categoryAdvancement.getDisplayData().getTitle(), language))
                        .content(MessageTranslator.convertMessage(categoryAdvancement.getDisplayData().getDescription(), language));

        List<GeyserAdvancement> visibleAdvancements = new ArrayList<>();
        if (currentAdvancementCategoryId != null) {
            for (GeyserAdvancement advancement : storedAdvancements.values()) {
                boolean earned = isEarned(advancement);
                if (earned || !advancement.getDisplayData().isHidden()) {
                    if (advancement.getParentId() != null && currentAdvancementCategoryId.equals(advancement.getRootId(this))) {
                        String color = earned ? advancement.getDisplayColor() : "";
                        builder.button(color + MessageTranslator.convertMessage(advancement.getDisplayData().getTitle()) + '\n');

                        visibleAdvancements.add(advancement);
                    }
                }
            }
        }

        builder.button(GeyserLocale.getPlayerLocaleString("gui.back", language));

        builder.closedResultHandler(() -> {
            // Indicate that we have closed the current advancement tab
            // Don't set currentAdvancementCategoryId to null here, so that when the advancements form is shown again (buildAndShowForm),
            // the tab that was last open is opened again, which matches Java behaviour
            formOpen = false;
            session.sendDownstreamGamePacket(new ServerboundSeenAdvancementsPacket());

        }).validResultHandler((response) -> {
            if (response.clickedButtonId() < visibleAdvancements.size()) {
                GeyserAdvancement advancement = visibleAdvancements.get(response.clickedButtonId());
                buildAndShowInfoForm(advancement);
            } else {
                buildAndShowMenuForm();
                // Indicate that we have closed the current advancement tab
                currentAdvancementCategoryId = null;
                session.sendDownstreamGamePacket(new ServerboundSeenAdvancementsPacket());
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
        String language = session.locale();

        boolean advancementHasProgress = advancement.getRequirements().size() > 1;

        int advancementProgress = getProgress(advancement);
        int advancementRequirements = advancement.getRequirements().size();

        boolean advancementEarned = advancementRequirements > 0
                && advancementProgress >= advancementRequirements;

        String earned = advancementEarned ? "yes" : "no";

        String description = getColorFromAdvancementFrameType(advancement) + MessageTranslator.convertMessage(advancement.getDisplayData().getDescription(), language);
        String earnedString = GeyserLocale.getPlayerLocaleString("geyser.advancements.earned", language, MinecraftLocale.getLocaleString("gui." + earned, language));

        /*
        Layout will look like:

        (Form title) Stone Age

        (Description) Mine stone with your new pickaxe

        Earned: Yes
        Progress: 1/4 // When advancement has multiple requirements
        Parent Advancement: Minecraft // If relevant
         */

        String content = description + "\n\n§f" + earnedString + "\n";

        if (advancementHasProgress) {
            // Only display progress with multiple requirements
            String progress = MinecraftLocale.getLocaleString("advancements.progress", language)
                    .replaceFirst("%s", String.valueOf(advancementProgress))
                    .replaceFirst("%s", String.valueOf(advancementRequirements));
            content += GeyserLocale.getPlayerLocaleString("geyser.advancements.progress", language, progress) + "\n";
        }

        if (!currentAdvancementCategoryId.equals(advancement.getParentId())) {
            // Only display the parent if it is not the category
            content += GeyserLocale.getPlayerLocaleString("geyser.advancements.parentid", language, MessageTranslator.convertMessage(storedAdvancements.get(advancement.getParentId()).getDisplayData().getTitle(), language));
        }

        session.sendForm(
                SimpleForm.builder()
                        .title(MessageTranslator.convertMessage(advancement.getDisplayData().getTitle()))
                        .content(content)
                        .button(GeyserLocale.getPlayerLocaleString("gui.back", language))
                        .validResultHandler((response) -> buildAndShowListForm())
                        .closedResultHandler(() -> {
                            // Indicate that we have closed the current advancement tab
                            formOpen = false;
                            session.sendDownstreamGamePacket(new ServerboundSeenAdvancementsPacket());
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
        if (advancement.getRequirements().isEmpty()) {
            // Minecraft handles this case, so we better as well
            return false;
        }
        // Progress should never be above requirements count, but you never know
        return getProgress(advancement) >= advancement.getRequirements().size();
    }

    /**
     * Determine the progress on an advancement.
     *
     * @param advancement the advancement to determine
     * @return the progress on the advancement.
     */
    public int getProgress(GeyserAdvancement advancement) {
        if (advancement.getRequirements().isEmpty()) {
            // Minecraft handles this case
            return 0;
        }
        int progress = 0;
        Map<String, Long> progressMap = storedAdvancementProgress.get(advancement.getId());
        if (progressMap != null) {
            // Each advancement's requirement must be fulfilled
            // For example, [[zombie, blaze, skeleton]] means that one of those three categories must be achieved
            // But [[zombie], [blaze], [skeleton]] means that all three requirements must be completed
            for (List<String> requirements : advancement.getRequirements()) {
                for (String requirement : requirements) {
                    Long obtained = progressMap.get(requirement);
                    // -1 means that this particular component required for completing the advancement
                    // has yet to be fulfilled
                    if (obtained != null && !obtained.equals(-1L)) {
                        progress++;
                    }
                }
            }
        }

        return progress;
    }

    public String getColorFromAdvancementFrameType(GeyserAdvancement advancement) {
        if (advancement.getDisplayData().getAdvancementType() == Advancement.DisplayData.AdvancementType.CHALLENGE) {
            return ChatColor.DARK_PURPLE;
        }
        return ChatColor.GREEN; // Used for types TASK and GOAL
    }
}
