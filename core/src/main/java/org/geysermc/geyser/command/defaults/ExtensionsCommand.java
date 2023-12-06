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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;

import java.util.Comparator;
import java.util.List;

public class ExtensionsCommand extends GeyserCommand {
    private final GeyserImpl geyser;

    public ExtensionsCommand(GeyserImpl geyser, String name, String description, String permission) {
        super(name, description, permission);

        this.geyser = geyser;
    }

    @Override
    public void execute(@Nullable GeyserSession session, GeyserCommandSource sender, String[] args) {
        // TODO: Pagination
        int page = 1;
        int maxPage = 1;
        String header = GeyserLocale.getPlayerLocaleString("geyser.commands.extensions.header", sender.locale(), page, maxPage);
        sender.sendMessage(header);

        this.geyser.extensionManager().extensions().stream().sorted(Comparator.comparing(Extension::name)).forEach(extension -> {
            String extensionName = (extension.isEnabled() ? ChatColor.GREEN : ChatColor.RED) + extension.name();
            sender.sendMessage("- " + extensionName + ChatColor.RESET + " v" + extension.description().version() + formatAuthors(extension.description().authors()));
        });
    }

    private String formatAuthors(List<String> authors) {
        return authors.isEmpty() ? "" : " by: " + String.join(", ", authors);
    }
}
