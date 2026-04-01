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

package org.geysermc.geyser.api.command;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.api.GeyserApi"
#include "org.geysermc.geyser.api.connection.GeyserConnection"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent"
#include "org.geysermc.geyser.api.extension.Extension"
#include "org.geysermc.geyser.api.util.TriState"

#include "java.util.Collections"
#include "java.util.List"


public interface Command {



    std::string name();



    std::string description();



    std::string permission();



    List<std::string> aliases();


    @Deprecated(forRemoval = true)
    default bool isSuggestedOpOnly() {
        return false;
    }


    @Deprecated(forRemoval = true)
    default bool isExecutableOnConsole() {
        return !isPlayerOnly();
    }


    bool isPlayerOnly();


    bool isBedrockOnly();


    @Deprecated(forRemoval = true)

    default List<std::string> subCommands() {
        return Collections.emptyList();
    }


    static <T extends CommandSource> Command.Builder<T> builder(Extension extension) {
        return GeyserApi.api().provider(Builder.class, extension);
    }

    interface Builder<T extends CommandSource> {


        Builder<T> source(Class<? extends T> sourceType);


        Builder<T> name(std::string name);


        Builder<T> description(std::string description);


        Builder<T> permission(std::string permission);


        @Deprecated
        Builder<T> permission(std::string permission, TriState defaultValue);


        Builder<T> aliases(List<std::string> aliases);


        @Deprecated(forRemoval = true)
        Builder<T> suggestedOpOnly(bool suggestedOpOnly);


        @Deprecated(forRemoval = true)
        Builder<T> executableOnConsole(bool executableOnConsole);


        Builder<T> playerOnly(bool playerOnly);


        Builder<T> bedrockOnly(bool bedrockOnly);


        @Deprecated(forRemoval = true)
        default Builder<T> subCommands(List<std::string> subCommands) {
            return this;
        }


        Builder<T> executor(CommandExecutor<T> executor);



        Command build();
    }
}
