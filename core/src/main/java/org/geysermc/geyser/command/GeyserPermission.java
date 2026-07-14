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
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.permission.Permission;
import org.incendo.cloud.permission.PermissionResult;
import org.incendo.cloud.permission.PredicatePermission;

import static org.geysermc.geyser.command.GeyserPermission.Result.Meta;

@AllArgsConstructor
public class GeyserPermission implements PredicatePermission<GeyserCommandSource> {

    /**
     * True if this permission requires the command source to be a bedrock player
     */
    private final boolean bedrockOnly;

    /**
     * True if this permission requires the command source to be any player
     */
    private final boolean playerOnly;

    /**
     * The permission node that the command source must have
     */
    private final String permission;

    @Override
    public @NonNull Result testPermission(@NonNull GeyserCommandSource source) {
        if (bedrockOnly) {
            if (source.connection() == null) {
                return new Result(Meta.NOT_BEDROCK);
            }
            // connection is present -> it is a player -> playerOnly is irrelevant
        } else if (playerOnly) {
            if (source.isConsole()) {
                return new Result(Meta.NOT_PLAYER); // must be a player but is console
            }
        }

        // Check through the GeyserCommandSource rather than cloud's manager:
        // cloud-velocity reverse-maps the wrapper back to the native Velocity
        // CommandSource and calls its hasPermission directly, which would
        // bypass VelocityCommandSource's permission-default fallback. Other
        // platforms' sources delegate to the same native checks the manager
        // would reach, so this is behavior-preserving for them, and it keeps
        // dispatch authorization consistent with the root/help paths, which
        // already resolve through the source.
        if (permission.isBlank() || source.hasPermission(permission)) {
            return new Result(Meta.ALLOWED);
        }
        return new Result(Meta.NO_PERMISSION);
    }

    @Override
    public @NonNull CloudKey<Void> key() {
        return CloudKey.cloudKey(permission);
    }

    /**
     * Basic implementation of cloud's {@link PermissionResult} that delegates to the more informative {@link Meta}.
     */
    public final class Result implements PermissionResult {

        private final Meta meta;

        private Result(Meta meta) {
            this.meta = meta;
        }

        public Meta meta() {
            return meta;
        }

        @Override
        public boolean allowed() {
            return meta == Meta.ALLOWED;
        }

        @Override
        public @NonNull Permission permission() {
            return GeyserPermission.this;
        }

        /**
         * More detailed explanation of whether the permission check passed.
         */
        public enum Meta {

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
            ALLOWED
        }
    }
}
