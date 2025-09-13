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

package org.geysermc.geyser.platform.mod.command;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.adapters.command.CommandSenderDefinition;
import org.geysermc.geyser.command.GeyserCommandSource;

import java.util.UUID;

public class ModCommandSource implements GeyserCommandSource {

    private final CommandSenderDefinition source;

    public ModCommandSource(CommandSenderDefinition source) {
        this.source = source;
    }

    @Override
    public String name() {
        return source.name();
    }

    @Override
    public void sendMessage(@NonNull String message) {
        source.sendMessage(message);
    }

    @Override
    public void sendMessage(net.kyori.adventure.text.Component message) {
        source.sendMessage(message, msg -> GeyserCommandSource.super.sendMessage(msg));
    }

    @Override
    public boolean isConsole() {
        return source.isConsole();
    }

    @Override
    public @Nullable UUID playerUuid() {
        return source.playerUuid();
    }

    @Override
    public boolean hasPermission(String permission) {
        // Unlike other bootstraps; we delegate to cloud here too:
        // On NeoForge; we'd have to keep track of all PermissionNodes - cloud already does that
        // For Fabric, we won't need to include the Fabric Permissions API anymore - cloud already does that too :p
        return GeyserImpl.getInstance().commandRegistry().hasPermission(this, permission);
    }

    @Override
    public Object handle() {
        return source.handle();
    }
}
