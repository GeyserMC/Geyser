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

package org.geysermc.geyser;

import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;
import org.geysermc.geyser.api.util.TriState;

import java.util.HashMap;
import java.util.Map;

/**
 * Permissions related to Geyser
 */
public final class Permissions {
    private static final Map<String, TriState> PERMISSIONS = new HashMap<>();

    public static final String CHECK_UPDATE = register("geyser.update");
    public static final String SERVER_SETTINGS = register("geyser.settings.server");
    public static final String SETTINGS_GAMERULES = register("geyser.settings.gamerules");

    private Permissions() {
        //no
    }

    private static String register(String permission) {
        return register(permission, TriState.NOT_SET);
    }

    @SuppressWarnings("SameParameterValue")
    private static String register(String permission, TriState permissionDefault) {
        PERMISSIONS.put(permission, permissionDefault);
        return permission;
    }

    public static void register(GeyserRegisterPermissionsEvent event) {
        for (Map.Entry<String, TriState> permission : PERMISSIONS.entrySet()) {
            event.register(permission.getKey(), permission.getValue());
        }
    }
}
