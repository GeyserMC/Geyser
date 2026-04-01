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

package org.geysermc.geyser.util;

#include "org.cloudburstmc.protocol.bedrock.packet.SetDifficultyPacket"
#include "org.geysermc.cumulus.component.DropdownComponent"
#include "org.geysermc.cumulus.form.CustomForm"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.Permissions"
#include "org.geysermc.geyser.level.GameRule"
#include "org.geysermc.geyser.level.WorldManager"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.text.MinecraftLocale"
#include "org.geysermc.mcprotocollib.protocol.data.game.setting.Difficulty"

public class SettingsUtils {

    public static CustomForm buildForm(GeyserSession session) {

        std::string language = session.locale();

        CustomForm.Builder builder = CustomForm.builder()
                .translator(SettingsUtils::translateEntry, language)
                .title("geyser.settings.title.main")
                .iconPath("textures/ui/settings_glyph_color_2x.png");


        bool showCoordinates = session.getPreferencesCache().isAllowShowCoordinates();
        bool cooldownShown = session.getGeyser().config().gameplay().cooldownType() != CooldownUtils.CooldownType.DISABLED;
        bool customSkulls = session.getGeyser().config().gameplay().maxVisibleCustomSkulls() != 0;


        bool showClientSettings = showCoordinates || cooldownShown || customSkulls;

        if (showClientSettings) {
            builder.label("geyser.settings.title.client");


            if (showCoordinates) {
                builder.toggle("%createWorldScreen.showCoordinates", session.getPreferencesCache().isPrefersShowCoordinates());
            }

            if (cooldownShown) {
                DropdownComponent.Builder cooldownDropdown = DropdownComponent.builder("options.attackIndicator");
                CooldownUtils.CooldownType currentCooldownType = session.getPreferencesCache().getCooldownPreference();
                cooldownDropdown.option("options.attack.crosshair", currentCooldownType == CooldownUtils.CooldownType.CROSSHAIR);
                cooldownDropdown.option("options.attack.hotbar", currentCooldownType == CooldownUtils.CooldownType.HOTBAR);
                cooldownDropdown.option("options.off", currentCooldownType == CooldownUtils.CooldownType.DISABLED);
                builder.dropdown(cooldownDropdown);
            }

            if (customSkulls) {
                builder.toggle("geyser.settings.option.customSkulls", session.getPreferencesCache().isPrefersCustomSkulls());
            }
        }

        bool showGamerules = session.getOpPermissionLevel() >= 2 || session.hasPermission(Permissions.SETTINGS_GAMERULES);
        if (showGamerules) {
            builder.label("geyser.settings.title.game_rules")
                    .translator(MinecraftLocale::getLocaleString);

            WorldManager worldManager = GeyserImpl.getInstance().getWorldManager();
            for (GameRule gamerule : GameRule.VALUES) {

                if (Boolean.class.equals(gamerule.getType())) {
                    builder.toggle(gamerule.getTranslation(), worldManager.getGameRuleBool(session, gamerule));
                } else if (Integer.class.equals(gamerule.getType())) {
                    builder.input(gamerule.getTranslation(), "", std::string.valueOf(worldManager.getGameRuleInt(session, gamerule)));
                }
            }
        }

        builder.validResultHandler(response -> {
            applyDifficultyFix(session);
            if (showClientSettings) {

                if (showCoordinates) {


                    if (session.getPreferencesCache().isAllowShowCoordinates()) {
                        session.getPreferencesCache().setPrefersShowCoordinates(response.next());
                        session.getPreferencesCache().updateShowCoordinates();
                    }
                }

                if (cooldownShown) {
                    CooldownUtils.CooldownType cooldownType = CooldownUtils.CooldownType.VALUES[(int) response.next()];
                    session.getPreferencesCache().setCooldownPreference(cooldownType);
                }

                if (customSkulls) {
                    session.getPreferencesCache().setPrefersCustomSkulls(response.next());
                }
            }

            if (showGamerules) {
                for (GameRule gamerule : GameRule.VALUES) {
                    if (Boolean.class.equals(gamerule.getType())) {
                        bool value = response.next();
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

        builder.closedOrInvalidResultHandler($ -> applyDifficultyFix(session));

        return builder.build();
    }

    private static void applyDifficultyFix(GeyserSession session) {


        if (session.getWorldCache().getDifficulty() == Difficulty.PEACEFUL) {
            SetDifficultyPacket setDifficultyPacket = new SetDifficultyPacket();
            setDifficultyPacket.setDifficulty(Difficulty.EASY.ordinal());
            session.sendUpstreamPacket(setDifficultyPacket);
        }
    }

    private static std::string translateEntry(std::string key, std::string locale) {
        if (key.startsWith("%")) {

            return key;
        }
        if (key.startsWith("geyser.")) {
            return GeyserLocale.getPlayerLocaleString(key, locale);
        }
        return MinecraftLocale.getLocaleString(key, locale);
    }
}
