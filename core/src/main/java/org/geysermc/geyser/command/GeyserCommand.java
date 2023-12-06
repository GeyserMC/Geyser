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

package org.geysermc.geyser.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Collections;
import java.util.List;

@Accessors(fluent = true)
@Getter
@RequiredArgsConstructor
public abstract class GeyserCommand implements Command {

    protected final String name;
    /**
     * The description of the command - will attempt to be translated.
     */
    protected final String description;
    protected final String permission;

    private List<String> aliases = Collections.emptyList();

    public abstract void execute(@Nullable GeyserSession session, GeyserCommandSource sender, String[] args);

    /**
     * If false, hides the command from being shown on the Geyser Standalone GUI.
     *
     * @return true if the command can be run on the server console
     */
    @Override
    public boolean isExecutableOnConsole() {
        return true;
    }

    /**
     * Used in the GUI to know what subcommands can be run
     *
     * @return a list of all possible subcommands, or empty if none.
     */
    @NonNull
    @Override
    public List<String> subCommands() {
        return Collections.emptyList();
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    /**
     * Used for permission defaults on server implementations.
     *
     * @return if this command is designated to be used only by server operators.
     */
    @Override
    public boolean isSuggestedOpOnly() {
        return false;
    }
}