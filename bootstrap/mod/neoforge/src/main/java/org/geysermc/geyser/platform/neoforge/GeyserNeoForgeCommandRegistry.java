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

import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.neoforge.PermissionNotRegisteredException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GeyserNeoForgeCommandRegistry extends CommandRegistry {

    /**
     * Permissions with an undefined permission default. Use Set to not register the same fallback more than once.
     * NeoForge requires that all permissions are registered, and cloud-neoforge follows that.
     * This is unlike most platforms, on which we wouldn't register a permission if no default was provided.
     */
    private final Set<String> undefinedPermissions = new HashSet<>();

    public GeyserNeoForgeCommandRegistry(GeyserImpl geyser, CommandManager<GeyserCommandSource> cloud) {
        super(geyser, cloud);
    }

    @Override
    protected void register(GeyserCommand command, Map<String, GeyserCommand> commands) {
        super.register(command, commands);

        // FIRST STAGE: Collect all permissions that may have undefined defaults.
        if (!command.permission().isBlank() && command.permissionDefault() == null) {
            // Permission requirement exists but no default value specified.
            undefinedPermissions.add(command.permission());
        }
    }

    @Override
    protected void onRegisterPermissions(GeyserRegisterPermissionsEvent event) {
        super.onRegisterPermissions(event);

        // SECOND STAGE
        // Now that we are aware of all commands, we can eliminate some incorrect assumptions.
        // Example: two commands may have the same permission, but only of them defines a permission default.
        undefinedPermissions.removeAll(permissionDefaults.keySet());
    }

    /**
     * Registers permissions with possibly undefined defaults.
     * Should be subscribed late to allow extensions and mods to register a desired permission default first.
     */
    void onPermissionGatherForUndefined(PermissionGatherEvent.Nodes event) {
        // THIRD STAGE
        for (String permission : undefinedPermissions) {
            if (PermissionUtils.register(permission, TriState.NOT_SET, event)) {
                // The permission was not already registered
                geyser.getLogger().debug("Registered permission " + permission + " with fallback default value of NOT_SET");
            }
        }
    }

    @Override
    public boolean hasPermission(GeyserCommandSource source, String permission) {
        // NeoForgeServerCommandManager will throw this exception if the permission is not registered to the server.
        // We can't realistically ensure that every permission is registered (calls by API users), so we catch this.
        // This works for our calls, but not for cloud's internal usage. For that case, see above.
        try {
            return super.hasPermission(source, permission);
        } catch (PermissionNotRegisteredException e) {
            return false;
        }
    }
}
