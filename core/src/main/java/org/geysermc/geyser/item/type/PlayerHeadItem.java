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

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.MinecraftLocale;

public class PlayerHeadItem extends Item {
    public PlayerHeadItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateNbtToBedrock(@NonNull GeyserSession session, @NonNull CompoundTag tag) {
        super.translateNbtToBedrock(session, tag);

        Tag display = tag.get("display");
        if (!(display instanceof CompoundTag) || !((CompoundTag) display).contains("Name")) {
            Tag skullOwner = tag.get("SkullOwner");
            if (skullOwner != null) {
                StringTag name;
                if (skullOwner instanceof StringTag) {
                    name = (StringTag) skullOwner;
                } else {
                    StringTag skullName;
                    if (skullOwner instanceof CompoundTag && (skullName = ((CompoundTag) skullOwner).get("Name")) != null) {
                        name = skullName;
                    } else {
                        session.getGeyser().getLogger().debug("Not sure how to handle skull head item display. " + tag);
                        return;
                    }
                }
                // Add correct name of player skull
                // TODO: It's always yellow, even with a custom name. Handle?
                String displayName = ChatColor.RESET + ChatColor.YELLOW + MinecraftLocale.getLocaleString("block.minecraft.player_head.named", session.locale()).replace("%s", name.getValue());
                if (!(display instanceof CompoundTag)) {
                    tag.put(display = new CompoundTag("display"));
                }
                ((CompoundTag) display).put(new StringTag("Name", displayName));
            }
        }
    }
}
