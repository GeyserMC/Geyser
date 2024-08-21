/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.neoforge;

import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.platform.neoforge.mixin.PermissionNodeMixin;

/**
 * Common logic for handling the more complicated way we have to register permission on NeoForge
 */
public class PermissionUtils {

    private PermissionUtils() {
        //no
    }

    /**
     * Registers the given permission and its default value to the event. If the permission has the same name as one
     * that has already been registered to the event, it will not be registered. In other words, it will not override.
     *
     * @param permission the permission to register
     * @param permissionDefault the permission's default value. See {@link GeyserRegisterPermissionsEvent#register(String, TriState)} for TriState meanings.
     * @param event the registration event
     * @return true if the permission was registered
     */
    public static boolean register(String permission, TriState permissionDefault, PermissionGatherEvent.Nodes event) {
        // NeoForge likes to crash if you try and register a duplicate node
        if (event.getNodes().stream().noneMatch(n -> n.getNodeName().equals(permission))) {
            PermissionNode<Boolean> node = createNode(permission, permissionDefault);
            event.addNodes(node);
            return true;
        }
        return false;
    }

    private static PermissionNode<Boolean> createNode(String node, TriState permissionDefault) {
        return PermissionNodeMixin.geyser$construct(
            node,
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> switch (permissionDefault) {
                case TRUE -> true;
                case FALSE -> false;
                case NOT_SET -> {
                    if (player != null) {
                        yield player.createCommandSourceStack().hasPermission(player.server.getOperatorUserPermissionLevel());
                    }
                    yield false; // NeoForge javadocs say player is null in the case of an offline player.
                }
            }
        );
    }
}
