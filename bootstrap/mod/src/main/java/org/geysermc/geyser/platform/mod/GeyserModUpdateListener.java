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

package org.geysermc.geyser.platform.mod;

import net.minecraft.world.entity.player.Player;
import org.geysermc.geyser.Permissions;
import org.geysermc.geyser.platform.mod.command.ModCommandSource;
import org.geysermc.geyser.util.VersionCheckUtils;

public final class GeyserModUpdateListener {
    public static void onPlayReady(Player player) {
        // Should be creating this in the supplier, but we need it for the permission check.
        // Not a big deal currently because ModCommandSource doesn't load locale, so don't need to try to wait for it.
        ModCommandSource source = new ModCommandSource(player.createCommandSourceStack());
        if (source.hasPermission(Permissions.CHECK_UPDATE)) {
            VersionCheckUtils.checkForGeyserUpdate(() -> source);
        }
    }

    private GeyserModUpdateListener() {
    }
}
