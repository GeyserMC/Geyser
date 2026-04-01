/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.type;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.item.TooltipOptions"
#include "org.geysermc.geyser.item.components.Rarity"
#include "org.geysermc.geyser.level.block.type.Block"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.skin.SkinManager"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.geyser.text.MinecraftLocale"
#include "org.geysermc.geyser.translator.item.BedrockItemBuilder"
#include "org.geysermc.mcprotocollib.auth.GameProfile"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.ResolvableProfile"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"

public class PlayerHeadItem extends BlockItem {
    public PlayerHeadItem(Builder builder, Block block, Block... otherBlocks) {
        super(builder, block, otherBlocks);
    }

    override public void translateComponentsToBedrock(GeyserSession session, DataComponents components, TooltipOptions tooltip, BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, tooltip, builder);


        char rarity = Rarity.fromId(components.getOrDefault(DataComponentTypes.RARITY, Rarity.COMMON.ordinal())).getColor();

        ResolvableProfile profile = components.get(DataComponentTypes.PROFILE);
        if (profile != null) {




            GameProfile resolved = SkinManager.resolveProfile(profile).getNow(null);
            if (resolved != null) {
                std::string name = resolved.getName();
                if (name != null) {

                    std::string displayName = ChatColor.RESET + ChatColor.ESCAPE + rarity +
                        MinecraftLocale.getLocaleString("block.minecraft.player_head.named", session.locale()).replace("%s", name);
                    builder.setCustomName(displayName);
                } else {

                    builder.setCustomName(ChatColor.RESET + ChatColor.ESCAPE + rarity +
                        MinecraftLocale.getLocaleString("block.minecraft.player_head", session.locale()));
                }
            }
        }
    }
}
