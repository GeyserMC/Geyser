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

package org.geysermc.geyser.command;

import cloud.commandframework.execution.postprocessor.CommandPostprocessingContext;
import cloud.commandframework.execution.postprocessor.CommandPostprocessor;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.services.types.ConsumerService;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;

public class SenderTypeProcessor implements CommandPostprocessor<GeyserCommandSource> {

    @Override
    public void accept(@NonNull CommandPostprocessingContext<GeyserCommandSource> processContext) {
        CommandMeta meta = processContext.getCommand().getCommandMeta();
        GeyserCommandSource source = processContext.getCommandContext().getSender();

        if (meta.getOrDefault(GeyserCommand.BEDROCK_ONLY, false)) {
            if (source.connection().isEmpty()) {
                source.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.bedrock_only", source.locale()));
                ConsumerService.interrupt();
            }
        } else if (meta.getOrDefault(GeyserCommand.PLAYER_ONLY, false)) {
            // it should be fine to use else-if here, because if the command is bedrock-only,
            // performing the bedrock player check is also inherently a player check

            if (source.playerUuid().isEmpty()) {
                source.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.player_only", source.locale()));
                ConsumerService.interrupt();
            }
        }
    }
}
