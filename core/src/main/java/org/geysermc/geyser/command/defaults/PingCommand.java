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

package org.geysermc.geyser.command.defaults;

import org.cloudburstmc.netty.channel.raknet.RakChildChannel;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;

public class PingCommand extends GeyserCommand {
    public PingCommand(String name, String description, String permission) {
        super(name, description, permission);
    }

    @Override
    public void execute(GeyserSession session, GeyserCommandSource sender, String[] args) {
        if (sender.isConsole()) {
            return;
        }

        RakSessionCodec rakSessionCodec = ((RakChildChannel) session.getUpstream().getSession().getPeer().getChannel()).rakPipeline().get(RakSessionCodec.class);

        // todo lang strings
        sender.sendMessage("Your ping is: " + ChatColor.BOLD + rakSessionCodec.getPing() + ChatColor.RESET + "ms.");
        sender.sendMessage("Your RTT is: §7" + ChatColor.BOLD + rakSessionCodec.getRTT() + ChatColor.RESET + "ms.");
    }

    @Override
    public boolean isBedrockOnly() {
        return true;
    }
}

