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

package org.geysermc.geyser.platform.neoforge;

import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.platform.neoforge.mixin.PermissionNodeMixin;

public class GeyserNeoForgePermissionHandler {

    public void onPermissionGather(PermissionGatherEvent.Nodes event) {
        GeyserImpl.getInstance().eventBus().fire(
            (GeyserRegisterPermissionsEvent) (permission, defaultValue) -> {
                if (permission.isBlank()) {
                    return;
                }
                registerNode(permission, defaultValue, event);
            }
        );
    }

    private void registerNode(String node, TriState permissionDefault, PermissionGatherEvent.Nodes event) {
        PermissionNode<Boolean> permissionNode = createNode(node, permissionDefault);

        // NeoForge likes to crash if you try and register a duplicate node
        if (event.getNodes().stream().noneMatch(eventNode -> eventNode.getNodeName().equals(node))) {
            event.addNodes(permissionNode);
        }
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
                        yield false;
                    }
                }
        );
    }
}
