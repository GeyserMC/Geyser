/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketDefinition;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.network.GameProtocol;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.LiteralParser;

public class DebugCommand extends GeyserCommand {
    public DebugCommand(@NonNull String name, @NonNull String description, @NonNull String permission) {
        super(name, description, permission, TriState.NOT_SET, false, false);
    }

    @Override
    public void register(CommandManager<GeyserCommandSource> manager) {
        manager.command(baseBuilder(manager)
                .required("packet_by_id", LiteralParser.literal("packet_by_id"))
                .required("id", IntegerParser.integerParser(0))
                .handler(this::executePacketById));
    }

    @Override
    public void execute(CommandContext<GeyserCommandSource> ctx) {
        // No-op
    }

    private void executePacketById(CommandContext<GeyserCommandSource> ctx) {
        int id = ctx.get("id");

        BedrockCodec codec = GameProtocol.getBedrockCodec(GameProtocol.DEFAULT_BEDROCK_PROTOCOL);
        BedrockPacketDefinition<?> definition = codec.getPacketDefinition(id);
        if (definition == null) {
            ctx.sender().sendMessage("Invalid packet ID provided!");
            return;
        }

        ctx.sender().sendMessage(definition.getFactory().get().getClass().getSimpleName());
        ctx.sender().sendMessage(definition.getSerializer().getClass().getName());
    }
}
