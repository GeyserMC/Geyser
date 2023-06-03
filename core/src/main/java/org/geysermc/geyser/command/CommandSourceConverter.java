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

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public record CommandSourceConverter<S, P>(Class<S> senderType,
                                           Function<UUID, P> playerLookup, Function<P, S> senderLookup,
                                           Supplier<S> consoleProvider) {


    public S convert(GeyserCommandSource source) throws IllegalArgumentException {
        Object handle = source.handle();
        if (senderType.isAssignableFrom(source.handle().getClass())) {
            return (S) handle; // one of the server platform implementations
        }

        if (source.isConsole()) {
            return consoleProvider.get(); // one of the loggers
        }

        // GeyserSession
        Optional<UUID> optionalUUID = source.playerUuid();
        if (optionalUUID.isPresent()) {
            UUID uuid = optionalUUID.get();

            P player = playerLookup.apply(uuid);
            if (player == null) {
                throw new IllegalArgumentException("failed to find player for " + uuid);
            }

            return senderLookup.apply(player);
        }

        throw new IllegalArgumentException("failed to convert source for " + source);
    }

    public static <S, P extends S> CommandSourceConverter<S, P> simple(Class<S> senderType,
                                                                       Function<UUID, P> playerLookup,
                                                                       Supplier<S> consoleProvider) {

        return new CommandSourceConverter<>(senderType, playerLookup, s -> s, consoleProvider);
    }
}
