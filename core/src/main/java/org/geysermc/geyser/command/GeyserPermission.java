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

import lombok.AllArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.permission.Permission;
import org.incendo.cloud.permission.PermissionResult;
import org.incendo.cloud.permission.PredicatePermission;

@AllArgsConstructor
public class GeyserPermission implements PredicatePermission<GeyserCommandSource> {

    private final boolean bedrockOnly;
    private final boolean playerOnly;
    private final String permission;
    private final CommandManager<GeyserCommandSource> manager;

    public Result check(GeyserCommandSource source) {
        if (bedrockOnly) {
            if (source.connection() == null) {
                return Result.NOT_BEDROCK;
            }
            // connection is present -> it is a player -> playerOnly is irrelevant
        } else if (playerOnly) {
            if (source.isConsole()) {
                return Result.NOT_PLAYER; // must be a player but is console
            }
        }

        if (manager.hasPermission(source, permission)) {
            return Result.ALLOWED;
        }
        return Result.NO_PERMISSION;
    }

    @Override
    public @NonNull CloudKey<Void> key() {
        return CloudKey.cloudKey(permission);
    }

    @Override
    public @NonNull PermissionResult testPermission(@NonNull GeyserCommandSource sender) {
        return check(sender).toPermission(permission);
    }

    public enum Result {

        /**
         * The source must be a bedrock player, but is not.
         */
        NOT_BEDROCK,

        /**
         * The source must be a player, but is not.
         */
        NOT_PLAYER,

        /**
         * The source does not have a required permission node.
         */
        NO_PERMISSION,

        /**
         * The source meets all requirements.
         */
        ALLOWED;

        public PermissionResult toPermission(String  permission) {
            return PermissionResult.of(this == ALLOWED, Permission.of(permission));
        }
    }
}
