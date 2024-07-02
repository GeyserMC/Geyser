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

package org.geysermc.geyser.api.permission;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.util.TriState;

/**
 * Something capable of checking if a {@link CommandSource} has a permission
 */
@FunctionalInterface
public interface PermissionChecker {

    /**
     * Checks if the given source has a permission
     *
     * @param source the {@link CommandSource} whose permissions should be queried
     * @param permission the permission node to check
     * @return a {@link TriState} as the value of the node. {@link TriState#NOT_SET} generally means that the permission
     *         node itself was not found, and the source does not have such permission.
     *         {@link TriState#TRUE} and {@link TriState#FALSE} represent explicitly set values.
     */
    @NonNull
    TriState hasPermission(@NonNull CommandSource source, @NonNull String permission);
}
