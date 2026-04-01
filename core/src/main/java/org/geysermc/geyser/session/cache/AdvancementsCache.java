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
    
    @Getter
    private final Map<String, Map<String, Long>> storedAdvancementProgress = new HashMap<>();

    
    @Getter
    private final Map<String, GeyserAdvancement> storedAdvancements = new HashMap<>();

    
    private String currentAdvancementCategoryId = null;

    
    private boolean formOpen = false;

    private final GeyserSession session;

    public AdvancementsCache(GeyserSession session) {
        this.session = session;
    }

    public void setCurrentAdvancementCategoryId(String categoryId) {
        if (!Objects.equals(currentAdvancementCategoryId, categoryId)) {
            
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

    
    public void buildAndShowMenuForm() {
        SimpleForm.Builder builder =
                SimpleForm.builder()
                        .translator(MinecraftLocale::getLocaleString, session.locale())
                        .title("gui.advancements");

        List<String> rootAdvancementIds = new ArrayList<>();
        for (Map.Entry<String, GeyserAdvancement> advancement : storedAdvancements.entrySet()) {
            if (advancement.getValue().getParentId() == null) { 
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
                
                ServerboundSeenAdvancementsPacket packet = new ServerboundSeenAdvancementsPacket(id);
                session.sendDownstreamGamePacket(packet);
                currentAdvancementCategoryId = id;
                buildAndShowListForm();
            }
        });

        formOpen = true;
        session.sendForm(builder);
    }

    
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
            
            
            
            formOpen = false;
            session.sendDownstreamGamePacket(new ServerboundSeenAdvancementsPacket());

        }).validResultHandler((response) -> {
            if (response.clickedButtonId() < visibleAdvancements.size()) {
                GeyserAdvancement advancement = visibleAdvancements.get(response.clickedButtonId());
                buildAndShowInfoForm(advancement);
            } else {
                buildAndShowMenuForm();
                
                currentAdvancementCategoryId = null;
                session.sendDownstreamGamePacket(new ServerboundSeenAdvancementsPacket());
            }
        });

        session.sendForm(builder);
    }

    
    public void buildAndShowInfoForm(GeyserAdvancement advancement) {
        
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
        Progress: 1/4 
        Parent Advancement: Minecraft 
         */

        String content = description + "\n\n§f" + earnedString + "\n";

        if (advancementHasProgress) {
            
            String progress = MinecraftLocale.getLocaleString("advancements.progress", language)
                    .replaceFirst("%s", String.valueOf(advancementProgress))
                    .replaceFirst("%s", String.valueOf(advancementRequirements));
            content += GeyserLocale.getPlayerLocaleString("geyser.advancements.progress", language, progress) + "\n";
        }

        if (!currentAdvancementCategoryId.equals(advancement.getParentId())) {
            
            content += GeyserLocale.getPlayerLocaleString("geyser.advancements.parentid", language, MessageTranslator.convertMessage(storedAdvancements.get(advancement.getParentId()).getDisplayData().getTitle(), language));
        }

        session.sendForm(
                SimpleForm.builder()
                        .title(MessageTranslator.convertMessage(advancement.getDisplayData().getTitle()))
                        .content(content)
                        .button(GeyserLocale.getPlayerLocaleString("gui.back", language))
                        .validResultHandler((response) -> buildAndShowListForm())
                        .closedResultHandler(() -> {
                            
                            formOpen = false;
                            session.sendDownstreamGamePacket(new ServerboundSeenAdvancementsPacket());
                        })
        );
    }

    
    public boolean isEarned(GeyserAdvancement advancement) {
        if (advancement.getRequirements().isEmpty()) {
            
            return false;
        }
        
        return getProgress(advancement) >= advancement.getRequirements().size();
    }

    
    public int getProgress(GeyserAdvancement advancement) {
        if (advancement.getRequirements().isEmpty()) {
            
            return 0;
        }
        int progress = 0;
        Map<String, Long> progressMap = storedAdvancementProgress.get(advancement.getId());
        if (progressMap != null) {
            
            
            
            for (List<String> requirements : advancement.getRequirements()) {
                for (String requirement : requirements) {
                    Long obtained = progressMap.get(requirement);
                    
                    
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
        return ChatColor.GREEN; 
    }
}
