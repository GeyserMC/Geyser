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

package org.geysermc.geyser.command.defaults;

import org.geysermc.common.PlatformType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.CommandSender;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;

import java.util.Collections;

public class StopCommand extends GeyserCommand {

    private final GeyserImpl geyser;

    public StopCommand(GeyserImpl geyser, String name, String description, String permission) {
        super(name, description, permission);
        this.geyser = geyser;

        this.setAliases(Collections.singletonList("shutdown"));
    }

    @Override
    public void execute(GeyserSession session, CommandSender sender, String[] args) {
        if (!sender.isConsole() && geyser.getPlatformType() == PlatformType.STANDALONE) {
            sender.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.permission_fail", sender.getLocale()));
            return;
        }

        geyser.getBootstrap().onDisable();
    }
}