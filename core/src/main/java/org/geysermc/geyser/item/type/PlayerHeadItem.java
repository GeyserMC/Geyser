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

import com.github.steveice10.mc.auth.data.GameProfile;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

public class PlayerHeadItem extends Item {
    public PlayerHeadItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull DataComponents components, @NonNull BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, builder);

        // TODO verify
        // Also - ChatColor.YELLOW + ChatColor.ITALIC + MessageTranslator.convertMessageLenient(nameTag.getValue(), session.locale())) this code existed if a custom name was already present.
        // But I think we would always overwrite that because translateDisplayProperties runs after this method.
        String customName = builder.getCustomName();
        if (customName == null) {
            GameProfile profile = components.get(DataComponentType.PROFILE);
            if (profile != null) {
                String name = profile.getName();
                if (name != null) {
                    // Add correct name of player skull
                    String displayName = ChatColor.RESET + ChatColor.YELLOW +
                            MinecraftLocale.getLocaleString("block.minecraft.player_head.named", session.locale()).replace("%s", name);
                    builder.setCustomName(displayName);
                } else {
                    // No name found so default to "Player Head"
                    builder.setCustomName(ChatColor.RESET + ChatColor.YELLOW +
                            MinecraftLocale.getLocaleString("block.minecraft.player_head", session.locale()));
                }
            }
        }
    }
}
