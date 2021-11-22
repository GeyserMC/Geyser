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

package org.geysermc.geyser.util;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.GameRule;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.cumulus.CustomForm;
import org.geysermc.cumulus.component.DropdownComponent;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.text.MinecraftLocale;

public class SettingsUtils {
    /**
     * Build a settings form for the given session and store it for later
     *
     * @param session The session to build the form for
     */
    public static CustomForm buildForm(GeyserSession session) {
        // Cache the language for cleaner access
        String language = session.getLocale();

        CustomForm.Builder builder = CustomForm.builder()
                .translator(SettingsUtils::translateEntry, language)
                .title("geyser.settings.title.main")
                .iconPath("textures/ui/settings_glyph_color_2x.png");

        // Only show the client title if any of the client settings are available
        boolean showClientSettings = session.getPreferencesCache().isAllowShowCoordinates()
                || CooldownUtils.getDefaultShowCooldown() != CooldownUtils.CooldownType.DISABLED
                || session.getGeyser().getConfig().isAllowCustomSkulls();

        if (showClientSettings) {
            builder.label("geyser.settings.title.client");

            // Client can only see its coordinates if reducedDebugInfo is disabled and coordinates are enabled in geyser config.
            if (session.getPreferencesCache().isAllowShowCoordinates()) {
                builder.toggle("geyser.settings.option.coordinates", session.getPreferencesCache().isPrefersShowCoordinates());
            }

            if (CooldownUtils.getDefaultShowCooldown() != CooldownUtils.CooldownType.DISABLED) {
                DropdownComponent.Builder cooldownDropdown = DropdownComponent.builder("options.attackIndicator");
                cooldownDropdown.option("options.attack.crosshair", session.getPreferencesCache().getCooldownPreference() == CooldownUtils.CooldownType.TITLE);
                cooldownDropdown.option("options.attack.hotbar", session.getPreferencesCache().getCooldownPreference() == CooldownUtils.CooldownType.ACTIONBAR);
                cooldownDropdown.option("options.off", session.getPreferencesCache().getCooldownPreference() == CooldownUtils.CooldownType.DISABLED);
                builder.dropdown(cooldownDropdown);
            }

            if (session.getGeyser().getConfig().isAllowCustomSkulls()) {
                builder.toggle("geyser.settings.option.customSkulls", session.getPreferencesCache().isPrefersCustomSkulls());
            }
        }

        boolean canModifyServer = session.getOpPermissionLevel() >= 2 || session.hasPermission("geyser.settings.server");
        if (canModifyServer) {
            builder.label("geyser.settings.title.server");

            DropdownComponent.Builder gamemodeDropdown = DropdownComponent.builder("%createWorldScreen.gameMode.personal");
            for (GameMode gamemode : GameMode.values()) {
                gamemodeDropdown.option("selectWorld.gameMode." + gamemode.name().toLowerCase(), session.getGameMode() == gamemode);
            }
            builder.dropdown(gamemodeDropdown);

            DropdownComponent.Builder difficultyDropdown = DropdownComponent.builder("%options.difficulty");
            for (Difficulty difficulty : Difficulty.values()) {
                difficultyDropdown.option("%options.difficulty." + difficulty.name().toLowerCase(), session.getWorldCache().getDifficulty() == difficulty);
            }
            builder.dropdown(difficultyDropdown);
        }

        boolean showGamerules = session.getOpPermissionLevel() >= 2 || session.hasPermission("geyser.settings.gamerules");
        if (showGamerules) {
            builder.label("geyser.settings.title.game_rules")
                    .translator(MinecraftLocale::getLocaleString); // we need translate gamerules next

            WorldManager worldManager = GeyserImpl.getInstance().getWorldManager();
            for (GameRule gamerule : GameRule.values()) {
                if (gamerule.equals(GameRule.UNKNOWN)) {
                    continue;
                }

                // Add the relevant form item based on the gamerule type
                if (Boolean.class.equals(gamerule.getType())) {
                    builder.toggle("gamerule." + gamerule.getJavaID(), worldManager.getGameRuleBool(session, gamerule));
                } else if (Integer.class.equals(gamerule.getType())) {
                    builder.input("gamerule." + gamerule.getJavaID(), "", String.valueOf(worldManager.getGameRuleInt(session, gamerule)));
                }
            }
        }

        builder.responseHandler((form, responseData) -> {
            CustomFormResponse response = form.parseResponse(responseData);
            if (response.isClosed() || response.isInvalid()) {
                return;
            }

            if (showClientSettings) {
                // Client can only see its coordinates if reducedDebugInfo is disabled and coordinates are enabled in geyser config.
                if (session.getPreferencesCache().isAllowShowCoordinates()) {
                    session.getPreferencesCache().setPrefersShowCoordinates(response.next());
                    session.getPreferencesCache().updateShowCoordinates();
                }

                if (CooldownUtils.getDefaultShowCooldown() != CooldownUtils.CooldownType.DISABLED) {
                    CooldownUtils.CooldownType cooldownType = CooldownUtils.CooldownType.VALUES[(int) response.next()];
                    session.getPreferencesCache().setCooldownPreference(cooldownType);
                }

                if (session.getGeyser().getConfig().isAllowCustomSkulls()) {
                    session.getPreferencesCache().setPrefersCustomSkulls(response.next());
                }
            }

            if (canModifyServer) {
                GameMode gameMode = GameMode.values()[(int) response.next()];
                if (gameMode != null && gameMode != session.getGameMode()) {
                    session.getGeyser().getWorldManager().setPlayerGameMode(session, gameMode);
                }

                Difficulty difficulty = Difficulty.values()[(int) response.next()];
                if (difficulty != null && difficulty != session.getWorldCache().getDifficulty()) {
                    session.getGeyser().getWorldManager().setDifficulty(session, difficulty);
                }
            }

            if (showGamerules) {
                for (GameRule gamerule : GameRule.VALUES) {
                    if (gamerule.equals(GameRule.UNKNOWN)) {
                        continue;
                    }

                    if (Boolean.class.equals(gamerule.getType())) {
                        boolean value = response.next();
                        if (value != session.getGeyser().getWorldManager().getGameRuleBool(session, gamerule)) {
                            session.getGeyser().getWorldManager().setGameRule(session, gamerule.getJavaID(), value);
                        }
                    } else if (Integer.class.equals(gamerule.getType())) {
                        int value = Integer.parseInt(response.next());
                        if (value != session.getGeyser().getWorldManager().getGameRuleInt(session, gamerule)) {
                            session.getGeyser().getWorldManager().setGameRule(session, gamerule.getJavaID(), value);
                        }
                    }
                }
            }
        });

        return builder.build();
    }

    private static String translateEntry(String key, String locale) {
        if (key.startsWith("geyser.")) {
            return GeyserLocale.getPlayerLocaleString(key, locale);
        }
        return MinecraftLocale.getLocaleString(key, locale);
    }
}
