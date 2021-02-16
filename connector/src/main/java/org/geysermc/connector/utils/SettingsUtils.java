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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import org.geysermc.common.window.CustomFormBuilder;
import org.geysermc.common.window.CustomFormWindow;
import org.geysermc.common.window.button.FormImage;
import org.geysermc.common.window.component.DropdownComponent;
import org.geysermc.common.window.component.InputComponent;
import org.geysermc.common.window.component.LabelComponent;
import org.geysermc.common.window.component.ToggleComponent;
import org.geysermc.common.window.response.CustomFormResponse;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.ArrayList;

public class SettingsUtils {

    // Used in UpstreamPacketHandler.java
    public static final int SETTINGS_FORM_ID = 1338;

    /**
     * Build a settings form for the given session and store it for later
     *
     * @param session The session to build the form for
     */
    public static void buildForm(GeyserSession session) {
        // Cache the language for cleaner access
        String language = session.getLocale();

        CustomFormBuilder builder = new CustomFormBuilder(LanguageUtils.getPlayerLocaleString("geyser.settings.title.main", language));
        builder.setIcon(new FormImage(FormImage.FormImageType.PATH, "textures/ui/settings_glyph_color_2x.png"));

        builder.addComponent(new LabelComponent(LanguageUtils.getPlayerLocaleString("geyser.settings.title.client", language)));
        builder.addComponent(new ToggleComponent(LanguageUtils.getPlayerLocaleString("geyser.settings.option.coordinates", language), session.getWorldCache().isShowCoordinates()));


        if (session.getOpPermissionLevel() >= 2 || session.hasPermission("geyser.settings.server")) {
            builder.addComponent(new LabelComponent(LanguageUtils.getPlayerLocaleString("geyser.settings.title.server", language)));

            DropdownComponent gamemodeDropdown = new DropdownComponent();
            gamemodeDropdown.setText("%createWorldScreen.gameMode.personal");
            gamemodeDropdown.setOptions(new ArrayList<>());
            for (GameMode gamemode : GameMode.values()) {
                gamemodeDropdown.addOption(LocaleUtils.getLocaleString("selectWorld.gameMode." + gamemode.name().toLowerCase(), language), session.getGameMode() == gamemode);
            }
            builder.addComponent(gamemodeDropdown);

            DropdownComponent difficultyDropdown = new DropdownComponent();
            difficultyDropdown.setText("%options.difficulty");
            difficultyDropdown.setOptions(new ArrayList<>());
            for (Difficulty difficulty : Difficulty.values()) {
                difficultyDropdown.addOption("%options.difficulty." + difficulty.name().toLowerCase(), session.getWorldCache().getDifficulty() == difficulty);
            }
            builder.addComponent(difficultyDropdown);
        }

        if (session.getOpPermissionLevel() >= 2 || session.hasPermission("geyser.settings.gamerules")) {
            builder.addComponent(new LabelComponent(LanguageUtils.getPlayerLocaleString("geyser.settings.title.game_rules", language)));
            for (GameRule gamerule : GameRule.values()) {
                if (gamerule.equals(GameRule.UNKNOWN)) {
                    continue;
                }

                // Add the relevant form item based on the gamerule type
                if (Boolean.class.equals(gamerule.getType())) {
                    builder.addComponent(new ToggleComponent(LocaleUtils.getLocaleString("gamerule." + gamerule.getJavaID(), language), GeyserConnector.getInstance().getWorldManager().getGameRuleBool(session, gamerule)));
                } else if (Integer.class.equals(gamerule.getType())) {
                    builder.addComponent(new InputComponent(LocaleUtils.getLocaleString("gamerule." + gamerule.getJavaID(), language), "", String.valueOf(GeyserConnector.getInstance().getWorldManager().getGameRuleInt(session, gamerule))));
                }
            }
        }

        session.setSettingsForm(builder.build());
    }

    /**
     * Handle the settings form response
     *
     * @param session The session that sent the response
     * @param response The response string to parse
     * @return True if the form was parsed correctly, false if not
     */
    public static boolean handleSettingsForm(GeyserSession session, String response) {
        CustomFormWindow settingsForm = session.getSettingsForm();
        settingsForm.setResponse(response);

        CustomFormResponse settingsResponse = (CustomFormResponse) settingsForm.getResponse();
        if (settingsResponse == null) {
            return false;
        }
        int offset = 0;

        offset++; // Client settings title

        session.getWorldCache().setShowCoordinates(settingsResponse.getToggleResponses().get(offset));
        offset++;

        if (session.getOpPermissionLevel() >= 2 || session.hasPermission("geyser.settings.server")) {
            offset++; // Server settings title

            GameMode gameMode = GameMode.values()[settingsResponse.getDropdownResponses().get(offset).getElementID()];
            if (gameMode != null && gameMode != session.getGameMode()) {
                session.getConnector().getWorldManager().setPlayerGameMode(session, gameMode);
            }
            offset++;

            Difficulty difficulty = Difficulty.values()[settingsResponse.getDropdownResponses().get(offset).getElementID()];
            if (difficulty != null && difficulty != session.getWorldCache().getDifficulty()) {
                session.getConnector().getWorldManager().setDifficulty(session, difficulty);
            }
            offset++;
        }

        if (session.getOpPermissionLevel() >= 2 || session.hasPermission("geyser.settings.gamerules")) {
            offset++; // Game rule title

            for (GameRule gamerule : GameRule.values()) {
                if (gamerule.equals(GameRule.UNKNOWN)) {
                    continue;
                }

                if (Boolean.class.equals(gamerule.getType())) {
                    boolean value = settingsResponse.getToggleResponses().get(offset);
                    if (value != session.getConnector().getWorldManager().getGameRuleBool(session, gamerule)) {
                        session.getConnector().getWorldManager().setGameRule(session, gamerule.getJavaID(), value);
                    }
                } else if (Integer.class.equals(gamerule.getType())) {
                    int value = Integer.parseInt(settingsResponse.getInputResponses().get(offset));
                    if (value != session.getConnector().getWorldManager().getGameRuleInt(session, gamerule)) {
                        session.getConnector().getWorldManager().setGameRule(session, gamerule.getJavaID(), value);
                    }
                }
                offset++;
            }
        }

        return true;
    }
}
