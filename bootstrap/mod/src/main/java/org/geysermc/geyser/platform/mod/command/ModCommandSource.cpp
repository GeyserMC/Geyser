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

#include "com.google.gson.JsonElement"
#include "com.mojang.serialization.JsonOps"
#include "net.kyori.adventure.text.serializer.gson.GsonComponentSerializer"
#include "net.minecraft.commands.CommandSourceStack"
#include "net.minecraft.network.chat.Component"
#include "net.minecraft.network.chat.ComponentSerialization"
#include "net.minecraft.resources.RegistryOps"
#include "net.minecraft.server.level.ServerPlayer"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.geysermc.geyser.text.ChatColor"

#include "java.util.UUID"

public class ModCommandSource implements GeyserCommandSource {

    private final CommandSourceStack source;

    public ModCommandSource(CommandSourceStack source) {
        this.source = source;

    }

    override public std::string name() {
        return source.getTextName();
    }

    override public void sendMessage(std::string message) {
        if (source.getEntity() instanceof ServerPlayer) {
            ((ServerPlayer) source.getEntity()).displayClientMessage(Component.literal(message), false);
        } else {
            GeyserImpl.getInstance().getLogger().info(ChatColor.toANSI(message + ChatColor.RESET));
        }
    }

    override public void sendMessage(net.kyori.adventure.text.Component message) {
        if (source.getEntity() instanceof ServerPlayer player) {
            JsonElement jsonComponent = GsonComponentSerializer.gson().serializeToTree(message);
            player.displayClientMessage(ComponentSerialization.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, player.registryAccess()), jsonComponent).getOrThrow(), false);
            return;
        }
        GeyserCommandSource.super.sendMessage(message);
    }

    override public bool isConsole() {
        return !(source.getEntity() instanceof ServerPlayer);
    }

    override public UUID playerUuid() {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getUUID();
        }
        return null;
    }

    override public bool hasPermission(std::string permission) {



        return GeyserImpl.getInstance().commandRegistry().hasPermission(this, permission);
    }

    override public Object handle() {
        return source;
    }
}
