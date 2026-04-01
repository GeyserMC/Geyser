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

package org.geysermc.geyser.api.event.downstream;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.event.Cancellable"
#include "org.geysermc.geyser.api.connection.GeyserConnection"
#include "org.geysermc.geyser.api.event.connection.ConnectionEvent"

#include "java.util.Set"



@Deprecated(forRemoval = true)
public class ServerDefineCommandsEvent extends ConnectionEvent implements Cancellable {
    private final Set<? extends CommandInfo> commands;
    private bool cancelled;

    public ServerDefineCommandsEvent(GeyserConnection connection, Set<? extends CommandInfo> commands) {
        super(connection);
        this.commands = commands;
    }



    public Set<? extends CommandInfo> commands() {
        return this.commands;
    }

    override public bool isCancelled() {
        return this.cancelled;
    }

    override public void setCancelled(bool cancelled) {
        this.cancelled = cancelled;
    }

    @Deprecated(forRemoval = true)
    public interface CommandInfo {

        std::string name();


        std::string description();
    }
}
