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

package org.geysermc.platform.sponge.command;

import lombok.AllArgsConstructor;

import net.kyori.adventure.text.Component;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.network.translators.chat.MessageTranslator;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

@AllArgsConstructor
public class SpongeCommandSender implements CommandSender {

    private final CommandCause handle;

    @Override
    public String getName() {
        // todo: probably not okay. maybe cast root cause to ServerPlayer if instance of, then get username
        return handle.friendlyIdentifier().orElse(handle.identifier());
    }

    @Override
    public void sendMessage(String message) {
        handle.audience().sendMessage(Component.text(MessageTranslator.convertMessage(message))); // this looks icky to me
    }

    @Override
    public boolean isConsole() {
        return !(handle.cause().root() instanceof ServerPlayer);
    }

    @Override
    public boolean hasPermission(String permission) {
        return handle.hasPermission(permission);
    }
}
