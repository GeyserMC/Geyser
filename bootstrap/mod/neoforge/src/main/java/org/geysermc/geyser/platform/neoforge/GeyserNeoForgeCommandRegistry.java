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
     */
    private final Set<String> undefinedPermissions = new HashSet<>();

    public GeyserNeoForgeCommandRegistry(GeyserImpl geyser, CommandManager<GeyserCommandSource> cloud) {
        super(geyser, cloud);
    }

    @Override
    protected void register(GeyserCommand command, Map<String, GeyserCommand> commands) {
        super.register(command, commands);

        if (!command.permission().isBlank() && command.permissionDefault() == null) {
            // Permission requirement exists but no default value specified.

            // Generally, we don't register a permission if no default is specified.
            // However, NeoForge requires that all permissions are registered, and cloud-neoforge follows that.
            undefinedPermissions.add(command.permission());
        }
    }

    @Override
    protected void onRegisterPermissions(GeyserRegisterPermissionsEvent event) {
        super.onRegisterPermissions(event);

        // Now that we are aware of all commands, we can determine which ones are actually undefined
        // (two commands may have the same permission, but only of them defines a permission default).
        // Note: This shouldn't be that necessary, as GeyserNeoForgePermissionHandler will ignore
        // anything already registered. Trying to rely on that as little as possible though.
        undefinedPermissions.removeAll(permissionDefaults.keySet());

        // Register with NOT_SET as a fallback.
        // If extensions wish, they may register permissions in an earlier listener, which won't be overridden.
        for (String permission : undefinedPermissions) {
            geyser.getLogger().debug("Registering permission " + permission + " with fallback default value of NOT_SET");
            event.register(permission, TriState.NOT_SET);
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
