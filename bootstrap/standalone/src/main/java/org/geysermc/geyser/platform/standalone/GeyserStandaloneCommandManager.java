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

package org.geysermc.geyser.platform.standalone;

import cloud.commandframework.CommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.internal.CommandRegistrationHandler;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionCheckersEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;
import org.geysermc.geyser.api.permission.PermissionChecker;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GeyserStandaloneCommandManager extends CommandManager<GeyserCommandSource> {

    private final GeyserImpl geyser;
    private final List<PermissionChecker> permissionCheckers = new ArrayList<>();
    private final Set<String> basePermissions = new ObjectOpenHashSet<>();

    public GeyserStandaloneCommandManager(GeyserImpl geyser) {
        super(CommandExecutionCoordinator.simpleCoordinator(), CommandRegistrationHandler.nullCommandRegistrationHandler());
        this.geyser = geyser;

        // allow any extensions to customize permissions
        geyser.getEventBus().fire((GeyserRegisterPermissionCheckersEvent) permissionCheckers::add);

        // must still implement a basic permission system
        try {
            File permissionsFile = geyser.getBootstrap().getConfigFolder().resolve("permissions.yml").toFile();
            FileUtils.fileOrCopiedFromResource(permissionsFile, "permissions.yml", geyser.getBootstrap());
            PermissionConfiguration config = FileUtils.loadConfig(permissionsFile, PermissionConfiguration.class);
            basePermissions.addAll(config.getDefaultPermissions());
        } catch (Exception e) {
            geyser.getLogger().warning("Failed to load permissions.yml");
            e.printStackTrace();
        }
    }

    /**
     * Fire a {@link GeyserRegisterPermissionsEvent} to determine any additions or removals to the base list of
     * permissions. This should be called after any event listeners have been registered, such as that of {@link CommandRegistry}.
     */
    // todo: doesn't seem like CommandRegistry has a listener anymore? should it? i forget.
    public void gatherPermissions() {
        geyser.getEventBus().fire((GeyserRegisterPermissionsEvent) (permission, def) -> {
            if (def == TriState.TRUE) {
                basePermissions.add(permission);
            } else if (def == TriState.FALSE) {
                basePermissions.remove(permission); // todo: maybe remove this case?
            }
        });
    }

    @Override
    public boolean hasPermission(@NonNull GeyserCommandSource sender, @NonNull String permission) {
        // Note: the two GeyserCommandSources on Geyser-Standalone are GeyserLogger and GeyserSession
        // GeyserLogger#hasPermission always returns true
        // GeyserSession#hasPermission delegates to this method,
        // which is why this method doesn't just call GeyserCommandSource#hasPermission

        if (sender.isConsole()) {
            return true;
        }

        for (PermissionChecker checker : permissionCheckers) {
            Boolean result = checker.hasPermission(sender, permission).toBoolean();
            if (result != null) {
                return result;
            }
        }
        return basePermissions.contains(permission);
    }

    @Override
    public @NonNull CommandMeta createDefaultCommandMeta() {
        return SimpleCommandMeta.empty();
    }
}
