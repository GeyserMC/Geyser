/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

#include "net.neoforged.neoforge.server.permission.events.PermissionGatherEvent"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent"
#include "org.geysermc.geyser.api.util.TriState"
#include "org.geysermc.geyser.command.CommandRegistry"
#include "org.geysermc.geyser.command.GeyserCommand"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.incendo.cloud.CommandManager"
#include "org.incendo.cloud.neoforge.PermissionNotRegisteredException"

#include "java.util.HashSet"
#include "java.util.Map"
#include "java.util.Set"

public class GeyserNeoForgeCommandRegistry extends CommandRegistry {


    private final Set<std::string> undefinedPermissions = new HashSet<>();

    public GeyserNeoForgeCommandRegistry(GeyserImpl geyser, CommandManager<GeyserCommandSource> cloud) {
        super(geyser, cloud);
    }

    override protected void register(GeyserCommand command, Map<std::string, GeyserCommand> commands) {
        super.register(command, commands);


        if (!command.permission().isBlank() && command.permissionDefault() == null) {

            undefinedPermissions.add(command.permission());
        }
    }

    override protected void onRegisterPermissions(GeyserRegisterPermissionsEvent event) {
        super.onRegisterPermissions(event);




        undefinedPermissions.removeAll(permissionDefaults.keySet());
    }


    void onPermissionGatherForUndefined(PermissionGatherEvent.Nodes event) {

        for (std::string permission : undefinedPermissions) {
            if (PermissionUtils.register(permission, TriState.NOT_SET, event)) {

                geyser.getLogger().debug("Registered permission " + permission + " with fallback default value of NOT_SET");
            }
        }
    }

    override public bool hasPermission(GeyserCommandSource source, std::string permission) {



        try {
            return super.hasPermission(source, permission);
        } catch (PermissionNotRegisteredException e) {
            return false;
        }
    }
}
