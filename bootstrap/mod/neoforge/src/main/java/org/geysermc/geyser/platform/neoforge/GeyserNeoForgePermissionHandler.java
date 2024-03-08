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
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContextKey;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionType;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;

import java.lang.reflect.Constructor;

public class GeyserNeoForgePermissionHandler {

    private static final Constructor<?> PERMISSION_NODE_CONSTRUCTOR;

    static {
        try {
            @SuppressWarnings("rawtypes")
            Constructor<PermissionNode> constructor = PermissionNode.class.getDeclaredConstructor(
                    String.class,
                    PermissionType.class,
                    PermissionNode.PermissionResolver.class,
                    PermissionDynamicContextKey[].class
            );
            constructor.setAccessible(true);
            PERMISSION_NODE_CONSTRUCTOR = constructor;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to construct PermissionNode!", e);
        }
    }

    public void onPermissionGather(PermissionGatherEvent.Nodes event) {
        this.registerNode(Constants.UPDATE_PERMISSION, event);

        GeyserImpl.getInstance().eventBus().fire((GeyserRegisterPermissionsEvent) (permission, defaultValue) -> this.registerNode(permission, event));
    }

    private void registerNode(String node, PermissionGatherEvent.Nodes event) {
        PermissionNode<Boolean> permissionNode = this.createNode(node);

        // NeoForge likes to crash if you try and register a duplicate node
        if (!event.getNodes().contains(permissionNode)) {
            event.addNodes(permissionNode);
        }
    }

    @SuppressWarnings("unchecked")
    private PermissionNode<Boolean> createNode(String node) {
        // The typical constructors in PermissionNode require a
        // mod id, which means our permission nodes end up becoming
        // geyser_neoforge.<node> instead of just <node>. We work around
        // this by using reflection to access the constructor that
        // doesn't require a mod id or ResourceLocation.
        try {
            return (PermissionNode<Boolean>) PERMISSION_NODE_CONSTRUCTOR.newInstance(
                    node,
                    PermissionTypes.BOOLEAN,
                    (PermissionNode.PermissionResolver<Boolean>) (player, playerUUID, context) -> false,
                    new PermissionDynamicContextKey[0]
            );
        } catch (Exception e) {
            throw new RuntimeException("Unable to create permission node " + node, e);
        }
    }
}
