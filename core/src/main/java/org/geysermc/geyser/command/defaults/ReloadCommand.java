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

package org.geysermc.geyser.command.defaults;

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.text.GeyserLocale;
import org.incendo.cloud.context.CommandContext;

import java.util.concurrent.TimeUnit;

public class ReloadCommand extends GeyserCommand {

    private final GeyserImpl geyser;

    public ReloadCommand(GeyserImpl geyser, String name, String description, String permission) {
        super(name, description, permission, TriState.NOT_SET);
        this.geyser = geyser;
    }

    @Override
    public void execute(CommandContext<GeyserCommandSource> context) {
        GeyserCommandSource source = context.sender();
        source.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.commands.reload.message", source.locale()));

        geyser.getSessionManager().disconnectAll("geyser.commands.reload.kick");
        //FIXME Without the tiny wait, players do not get kicked - same happens when Geyser tries to disconnect all sessions on shutdown
        geyser.getScheduledThread().schedule(geyser::reloadGeyser, 10, TimeUnit.MILLISECONDS);
    }
}
