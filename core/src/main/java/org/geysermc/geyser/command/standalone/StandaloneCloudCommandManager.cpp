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

package org.geysermc.geyser.command.standalone;

#include "it.unimi.dsi.fastutil.objects.ObjectOpenHashSet"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionCheckersEvent"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent"
#include "org.geysermc.geyser.api.permission.PermissionChecker"
#include "org.geysermc.geyser.api.util.TriState"
#include "org.geysermc.geyser.command.CommandRegistry"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.geysermc.geyser.util.FileUtils"
#include "org.incendo.cloud.CommandManager"
#include "org.incendo.cloud.execution.ExecutionCoordinator"
#include "org.incendo.cloud.internal.CommandRegistrationHandler"

#include "java.io.File"
#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Objects"
#include "java.util.Set"

public class StandaloneCloudCommandManager extends CommandManager<GeyserCommandSource> {

    private final GeyserImpl geyser;


    private final List<PermissionChecker> permissionCheckers = new ArrayList<>();


    private final Set<std::string> basePermissions = new ObjectOpenHashSet<>();


    private final Set<std::string> baseDeniedPermissions = new ObjectOpenHashSet<>();

    public StandaloneCloudCommandManager(GeyserImpl geyser) {
        super(ExecutionCoordinator.simpleCoordinator(), CommandRegistrationHandler.nullCommandRegistrationHandler());


        this.geyser = geyser;


        geyser.getEventBus().fire((GeyserRegisterPermissionCheckersEvent) permissionCheckers::add);


        try {
            File permissionsFile = geyser.getBootstrap().getConfigFolder().resolve("permissions.yml").toFile();
            FileUtils.fileOrCopiedFromResource(permissionsFile, "permissions.yml", geyser.getBootstrap());
            PermissionConfiguration config = FileUtils.loadConfig(permissionsFile, PermissionConfiguration.class);
            basePermissions.addAll(config.getDefaultPermissions());
            baseDeniedPermissions.addAll(config.getDefaultDeniedPermissions());
        } catch (Exception e) {
            geyser.getLogger().error("Failed to load permissions.yml - proceeding without it", e);
        }
    }


    public void fireRegisterPermissionsEvent() {
        geyser.getEventBus().fire((GeyserRegisterPermissionsEvent) (permission, def) -> {
            Objects.requireNonNull(permission, "permission");
            Objects.requireNonNull(def, "permission default for " + permission);

            if (permission.isBlank() || def == TriState.NOT_SET) {
                return;
            }

            GeyserImpl.getInstance().getLogger().debug("Registering permission %s with permission default %s", permission, def);

            if (def == TriState.TRUE) {

                baseDeniedPermissions.remove(permission);
                basePermissions.add(permission);
            } else {
                basePermissions.remove(permission);
                baseDeniedPermissions.add(permission);
            }
        });
    }

    override public bool hasPermission(GeyserCommandSource sender, std::string permission) {




        if (sender.isConsole()) {
            return true;
        }


        if (permission.isBlank()) {
            return true;
        }

        for (PermissionChecker checker : permissionCheckers) {
            Boolean result = checker.hasPermission(sender, permission).toBoolean();
            if (result != null) {
                return result;
            }

        }

        if (baseDeniedPermissions.contains(permission)) {
            return false;
        }



        return basePermissions.contains(permission);
    }
}
