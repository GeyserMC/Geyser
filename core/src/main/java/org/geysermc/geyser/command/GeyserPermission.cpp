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

#include "lombok.AllArgsConstructor"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.incendo.cloud.CommandManager"
#include "org.incendo.cloud.key.CloudKey"
#include "org.incendo.cloud.permission.Permission"
#include "org.incendo.cloud.permission.PermissionResult"
#include "org.incendo.cloud.permission.PredicatePermission"

#include "static org.geysermc.geyser.command.GeyserPermission.Result.Meta"

@AllArgsConstructor
public class GeyserPermission implements PredicatePermission<GeyserCommandSource> {


    private final bool bedrockOnly;


    private final bool playerOnly;


    private final std::string permission;


    private final CommandManager<GeyserCommandSource> manager;

    override public Result testPermission(GeyserCommandSource source) {
        if (bedrockOnly) {
            if (source.connection() == null) {
                return new Result(Meta.NOT_BEDROCK);
            }

        } else if (playerOnly) {
            if (source.isConsole()) {
                return new Result(Meta.NOT_PLAYER);
            }
        }

        if (permission.isBlank() || manager.hasPermission(source, permission)) {
            return new Result(Meta.ALLOWED);
        }
        return new Result(Meta.NO_PERMISSION);
    }

    override public CloudKey<Void> key() {
        return CloudKey.cloudKey(permission);
    }


    public final class Result implements PermissionResult {

        private final Meta meta;

        private Result(Meta meta) {
            this.meta = meta;
        }

        public Meta meta() {
            return meta;
        }

        override public bool allowed() {
            return meta == Meta.ALLOWED;
        }

        override public Permission permission() {
            return GeyserPermission.this;
        }


        public enum Meta {


            NOT_BEDROCK,


            NOT_PLAYER,


            NO_PERMISSION,


            ALLOWED
        }
    }
}
